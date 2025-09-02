package co.com.crediya_solicitud.consumer.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

class RestConsumerConfigUnitTest {

    private final RestConsumerConfig config = new RestConsumerConfig();

    @Test
    @DisplayName("Debe construir un WebClient no nulo")
    void shouldCreateWebClient() {
        WebClient webClient = config.webClient(WebClient.builder());
        assertThat(webClient).isNotNull();
    }
}