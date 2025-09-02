package co.com.crediya_solicitud.consumer;

import co.com.crediya_solicitud.consumer.dto.ExternalErrorResponse;
import co.com.crediya_solicitud.consumer.dto.ExternalUserRequest;
import co.com.crediya_solicitud.consumer.dto.ExternalUserResponse;
import co.com.crediya_solicitud.model.UserGateway;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
public class RestConsumer implements UserGateway {
    private final WebClient client;

    @Override
    public Mono<String> getUserEmailByDocument(String documentId) {
        return client
                .post()
                .uri("/v1/usuarios/document")
                .bodyValue(new ExternalUserRequest(documentId))
                .retrieve()

                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(ExternalErrorResponse.class)
                                .flatMap(error -> Mono.error(
                                        new ExternalServiceException(
                                                error.getStatus(),
                                                error.getMessage(),
                                                error.getBody()
                                        )
                                ))
                )
                .bodyToMono(ExternalUserResponse.class)
                .map(response -> response.getBody().getEmail());
    }

}
