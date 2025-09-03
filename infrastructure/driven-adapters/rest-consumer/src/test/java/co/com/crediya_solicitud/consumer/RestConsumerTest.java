package co.com.crediya_solicitud.consumer;


import co.com.crediya_solicitud.model.logger.Logger;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;


class RestConsumerTest {

    private static RestConsumer restConsumer;

    private static MockWebServer mockBackEnd;


    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
        var webClient = WebClient.builder().baseUrl(mockBackEnd.url("/").toString()).build();

        Logger mockLogger = Mockito.mock(Logger.class);

        restConsumer = new RestConsumer(webClient, mockLogger);


    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    @DisplayName("Debe obtener el email cuando la respuesta es exitosa")
    void validateGetUserByDocument_Success() {
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.OK.value())
                .setBody("{\"body\":{\"email\":\"test@correo.com\"}}"));

        var response = restConsumer.getUserEmailByDocument("123");

        StepVerifier.create(response)
                .expectNext("test@correo.com")
                .verifyComplete();
    }

    @Test
    @DisplayName("Debe lanzar ExternalServiceException en error 500")
    void validateGetUserByDocument_Error() {
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .setBody("{\"status\":500, \"message\":\"Error interno\", \"body\":[\"fallo\"]}"));

        var response = restConsumer.getUserEmailByDocument("123");

        StepVerifier.create(response)
                .expectErrorMatches(ex -> ex instanceof co.com.crediya_solicitud.model.exception.ExternalServiceException
                        && ((co.com.crediya_solicitud.model.exception.ExternalServiceException) ex).getStatus() == 500
                        && ex.getMessage().contains("Error interno"))
                .verify();
    }
}