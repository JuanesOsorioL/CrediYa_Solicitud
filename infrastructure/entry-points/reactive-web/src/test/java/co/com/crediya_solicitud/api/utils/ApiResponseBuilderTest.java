package co.com.crediya_solicitud.api.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

public class ApiResponseBuilderTest {

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        ApiResponseBuilder builder = new ApiResponseBuilder();

        RouterFunction<ServerResponse> router = route(GET("/test"), request ->
                builder.build(HttpStatus.OK, "Test exitoso", "Cuerpo de prueba"));

        webTestClient = WebTestClient.bindToRouterFunction(router).build();
    }

    @Test
    void testApiResponseBuilder() {
        webTestClient.get()
                .uri("/test")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("Test exitoso")
                .jsonPath("$.body").isEqualTo("Cuerpo de prueba");
    }
}