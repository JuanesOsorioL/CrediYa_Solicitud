package co.com.crediya_solicitud.consumer;


import co.com.crediya_solicitud.consumer.mapper.RestConsumerDtoMapper;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.user.User;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

class RestConsumerTest {
    private MockWebServer server;

    private RestConsumer restConsumer;
    private Logger mockLogger;
    private RestConsumerDtoMapper mockMapper;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        String baseUrl = server.url("/").toString();
        WebClient webClient = WebClient.builder().baseUrl(baseUrl).build();

        mockLogger = Mockito.mock(Logger.class);
        mockMapper = Mockito.mock(RestConsumerDtoMapper.class);

        restConsumer = new RestConsumer(webClient, mockLogger, mockMapper);
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }


    @Test
    @DisplayName("validateTokenAndGetClaims: devuelve ClaismoDto cuando 200 OK")
    void validateTokenAndGetClaims_success() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("""
                        {
                          "body": {
                            "Nombre": "Juan",
                            "sub": "juan@test.com",
                            "Rol": "Customer",
                            "Fecha": "2024-01-01",
                            "Apellido": "Pérez",
                            "Document": "123",
                            "Fecha2": "2024-01-01",
                            "Id": "abc"
                          }
                        }
                        """));

        var mono = restConsumer.validateTokenAndGetClaims("abc123");

        StepVerifier.create(mono)
                .assertNext(claims -> {
                    assertThat(claims.sub()).isEqualTo("juan@test.com");
                    assertThat(claims.Rol()).isEqualTo("Customer");
                    assertThat(claims.Document()).isEqualTo("123");
                })
                .verifyComplete();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).isEqualTo("/v1/validateToken");
        assertThat(req.getMethod()).isEqualTo("GET");
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer abc123");
    }

    @Test
    @DisplayName("validateTokenAndGetClaims: mapea error del micro a ExternalServiceException")
    void validateTokenAndGetClaims_error() {
        server.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.UNAUTHORIZED.value())
                .setBody("""
                        {
                          "status": 401,
                          "code": "AUTH_015",
                          "message": "Token inválido",
                          "body": ["USR_015"]
                        }
                        """));

        var mono = restConsumer.validateTokenAndGetClaims("bad-token");

        StepVerifier.create(mono)
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ExternalServiceException.class);
                    ExternalServiceException ese = (ExternalServiceException) ex;
                    assertThat(ese.getStatus()).isEqualTo(401);
                    assertThat(ese.getCode()).isEqualTo("AUTH_015");
                    assertThat(ese.getMessage()).contains("Token inválido");
                })
                .verify();
    }

    @Test
    @DisplayName("getUserEmailByDocument: obtiene el email cuando 200 OK")
    void getUserEmailByDocument_success() {
        server.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("""
                        {"body":{"email":"test@correo.com"}}
                        """));

        var mono = restConsumer.getUserEmailByDocument("123");

        StepVerifier.create(mono)
                .expectNext("test@correo.com")
                .verifyComplete();
    }

    @Test
    @DisplayName("getUserEmailByDocument: error 500 -> ExternalServiceException")
    void getUserEmailByDocument_error() {
        server.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setBody("""
                        {"status":500, "code":"ERR_X", "message":"Error interno", "body":["fallo"]}
                        """));

        var mono = restConsumer.getUserEmailByDocument("123");

        StepVerifier.create(mono)
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ExternalServiceException.class);
                    ExternalServiceException ese = (ExternalServiceException) ex;
                    assertThat(ese.getStatus()).isEqualTo(500);
                    assertThat(ese.getMessage()).contains("Error interno");
                })
                .verify();
    }

    @Test
    @DisplayName("getUsersByEmails: mapea mapa de usuarios filtrando nulos")
    void getUsersByEmails_success() {
        server.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("""
                        {
                          "body": {
                            "users": {
                              "ana@mail.com": {"email":"ana@mail.com","fullName":"Ana"},
                              "bob@mail.com": {"email":"bob@mail.com","fullName":"Bob"},
                              "null": null
                            }
                          }
                        }
                        """));

        User userAna = Mockito.mock(User.class);
        User userBob = Mockito.mock(User.class);
        Mockito.when(mockMapper.toDomain(any()))
                .thenReturn(userAna, userBob);

        var mono = restConsumer.getUsersByEmails(Set.of("ana@mail.com", "bob@mail.com"));

        StepVerifier.create(mono)
                .assertNext(map -> {
                    assertThat(map).hasSize(2);
                    assertThat(map).containsKeys("ana@mail.com", "bob@mail.com");
                    assertThat(map.get("ana@mail.com")).isSameAs(userAna);
                    assertThat(map.get("bob@mail.com")).isSameAs(userBob);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("getUsersByEmails: error del micro -> ExternalServiceException")
    void getUsersByEmails_error() {
        server.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.BAD_GATEWAY.value())
                .setBody("""
                        {"status":502, "code":"AUTH_DN", "message":"Downstream", "body":["micro caído"]}
                        """));

        var mono = restConsumer.getUsersByEmails(Set.of("a@b.com"));

        StepVerifier.create(mono)
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(ExternalServiceException.class);
                    ExternalServiceException ese = (ExternalServiceException) ex;
                    assertThat(ese.getStatus()).isEqualTo(502);
                    assertThat(ese.getMessage()).contains("Downstream");
                })
                .verify();
    }
}