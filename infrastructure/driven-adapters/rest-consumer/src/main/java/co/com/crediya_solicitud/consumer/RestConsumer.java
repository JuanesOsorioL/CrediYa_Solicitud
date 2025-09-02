package co.com.crediya_solicitud.consumer;

import co.com.crediya_solicitud.consumer.dto.ExternalErrorResponse;
import co.com.crediya_solicitud.consumer.dto.ExternalUserRequest;
import co.com.crediya_solicitud.consumer.dto.ExternalUserResponse;
import co.com.crediya_solicitud.model.UserGateway;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.logger.Logger;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
public class RestConsumer implements UserGateway {
    private final WebClient client;
    private final Logger logger;

    @Override
    public Mono<String> getUserEmailByDocument(String documentId) {
        logger.info("Se realiza el llamado al Micro de Auth");
        return client
                .post()
                .uri("/v1/usuarios/document")
                .bodyValue(new ExternalUserRequest(documentId))
                .retrieve()

                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(ExternalErrorResponse.class)
                                .doOnNext(externalErrorResponse -> logger.info("Se presento un error en el Micro"))
                                .flatMap(error -> Mono.error(
                                        new ExternalServiceException(
                                                error.getStatus(),
                                                error.getMessage(),
                                                error.getBody()
                                        )
                                ))
                )
                .bodyToMono(ExternalUserResponse.class)
                .map(response -> response.getBody().getEmail())
                .doOnNext(email -> logger.info("Se retorna a UseCase el email" + email + " "));
    }

}
