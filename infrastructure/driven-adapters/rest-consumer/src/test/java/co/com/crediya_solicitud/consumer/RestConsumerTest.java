package co.com.crediya_solicitud.consumer;


import co.com.crediya_solicitud.consumer.mapper.RestConsumerDtoMapper;
import co.com.crediya_solicitud.model.claims.Claismo;
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
import java.util.List;

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
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer abc123")
                .build();

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

        Claismo mapped = new Claismo("Juan", "juan@test.com", "Customer", "2024-01-01", "Pérez", "123", "2024-01-01", "abc");
        Mockito.when(mockMapper.toClaismo(any())).thenReturn(mapped);

        var mono = restConsumer.validateTokenAndGetClaims();

        StepVerifier.create(mono)
                .assertNext(claims -> assertThat(claims).isSameAs(mapped))
                .verifyComplete();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).isEqualTo("/api/v1/validateToken");
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

        var mono = restConsumer.validateTokenAndGetClaims();

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
    @DisplayName("getUsersByEmails: mapea el mapa de usuarios (sin nulos)")
    void getUsersByEmails_success() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("""
                        {
                          "body": {
                            "users": {
                              "ana@mail.com": {"email":"ana@mail.com","fullName":"Ana"},
                              "bob@mail.com": {"email":"bob@mail.com","fullName":"Bob"}
                            }
                          }
                        }
                        """));

        User userAna = Mockito.mock(User.class);
        User userBob = Mockito.mock(User.class);
        Mockito.when(mockMapper.toDomain(any()))
                .thenReturn(userAna, userBob);

        var mono = restConsumer.getUsersByEmails(List.of("ana@mail.com", "bob@mail.com"));

        StepVerifier.create(mono)
                .assertNext(map -> {
                    assertThat(map).hasSize(2);
                    assertThat(map).containsKeys("ana@mail.com", "bob@mail.com");
                    assertThat(map.get("ana@mail.com")).isSameAs(userAna);
                    assertThat(map.get("bob@mail.com")).isSameAs(userBob);
                })
                .verifyComplete();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).isEqualTo("/api/v1/map");
        assertThat(req.getMethod()).isEqualTo("POST");
        assertThat(req.getHeader(HttpHeaders.CONTENT_TYPE)).contains("application/json");
        assertThat(req.getBody().readUtf8())
                .contains("\"emails\":[\"ana@mail.com\",\"bob@mail.com\"]");
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

        var mono = restConsumer.getUsersByEmails(List.of("a@b.com"));

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