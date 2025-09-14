package co.com.crediya_solicitud.consumer.config;


import co.com.crediya_solicitud.model.shared_token.AuthContext;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class RestConsumerConfigTest {

    static MockWebServer server;
    WebClient client;

    @BeforeAll
    static void start() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterAll
    static void stop() throws IOException {
        server.shutdown();
    }

    @BeforeEach
    void setup() {
        RestConsumerConfig config = new RestConsumerConfig();
        WebClient built = config.webClient(WebClient.builder());
        client = built.mutate().baseUrl(server.url("/api").toString()).build();
    }

    @Test
    void addsAuthorizationHeaderFromContext() throws Exception {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{}"));

        Mono<Integer> mono = client.get()
                .uri("/ping")
                .exchangeToMono(resp -> Mono.just(resp.statusCode().value()))
                .contextWrite(ctx -> ctx.put(AuthContext.TOKEN_KEY, "tok-xyz"));

        StepVerifier.create(mono)
                .expectNext(200)
                .verifyComplete();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).isEqualTo("/api/ping");
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer tok-xyz");
    }

    @Test
    void doesNotAddAuthorizationWhenContextMissing() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        StepVerifier.create(
                        client.get()
                                .uri("/no-auth")
                                .exchangeToMono(resp -> Mono.just(resp.statusCode().value()))
                )
                .expectNext(200)
                .verifyComplete();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).isEqualTo("/api/no-auth");
        assertThat(req.getHeader("Authorization")).isNull();
    }

    @Test
    void overridesExistingAuthorizationHeaderWithContextToken() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(200));

        StepVerifier.create(
                        client.get()
                                .uri("/override")
                                .header("Authorization", "Bearer OLD")
                                .exchangeToMono(resp -> Mono.just(resp.statusCode().value()))
                                .contextWrite(ctx -> ctx.put(AuthContext.TOKEN_KEY, "NEW"))
                )
                .expectNext(200)
                .verifyComplete();

        RecordedRequest req = server.takeRequest();
        assertThat(req.getPath()).isEqualTo("/api/override");
        assertThat(req.getHeader("Authorization")).isEqualTo("Bearer NEW");
    }
}