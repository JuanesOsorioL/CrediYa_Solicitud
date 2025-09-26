package co.com.crediya_solicitud.consumer.config;


import co.com.crediya_solicitud.model.shared_token.AuthContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class RestConsumerConfig {

    @Value("${adapter.restconsumer.url}")
    private String restBaseUrl;

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl(restBaseUrl)
                .filter(authorizationFromContext())
                .build();
    }

    private ExchangeFilterFunction authorizationFromContext() {
        return (request, next) -> Mono.deferContextual(ctx -> {
            String token = ctx.getOrDefault(AuthContext.TOKEN_KEY, "");
            if (token == null || token.isBlank()) {
                return next.exchange(request);
            }
            ClientRequest newReq = ClientRequest.from(request)
                   // .header("Authorization", "Bearer " + token)
                    .headers(h -> h.set("Authorization", "Bearer " + token))
                    .build();
            return next.exchange(newReq);
        });
    }

}
