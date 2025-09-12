package co.com.crediya_solicitud.consumer.config;


import co.com.crediya_solicitud.model.shared_token.AuthContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class RestConsumerConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://localhost:8080/api")
                .filter((request, next) ->
                        Mono.deferContextual(ctxView -> {
                            if (ctxView.hasKey(AuthContext.TOKEN_KEY)) {
                                String token = ctxView.get(AuthContext.TOKEN_KEY);
                                ClientRequest withAuth = ClientRequest.from(request)
                                        .headers(h -> h.setBearerAuth(token))
                                        .build();
                                return next.exchange(withAuth);
                            }
                            return next.exchange(request);
                        })
                )
                .build();
    }
}
