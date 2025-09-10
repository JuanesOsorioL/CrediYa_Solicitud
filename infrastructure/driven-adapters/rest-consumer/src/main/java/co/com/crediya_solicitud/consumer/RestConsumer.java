package co.com.crediya_solicitud.consumer;

import co.com.crediya_solicitud.consumer.dto.RequestDocumentId;
import co.com.crediya_solicitud.consumer.dto.response.ExternalClaimsResponse;
import co.com.crediya_solicitud.consumer.dto.response.ExternalErrorResponse;
import co.com.crediya_solicitud.consumer.dto.response.ExternalUserListResponse;
import co.com.crediya_solicitud.consumer.dto.response.ExternalUserResponse;
import co.com.crediya_solicitud.consumer.mapper.RestConsumerDtoMapper;
import co.com.crediya_solicitud.model.claims.Claims;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;


@Service
@RequiredArgsConstructor
public class RestConsumer implements UserGateway {
    private final WebClient client;
    private final Logger logger;
    private final RestConsumerDtoMapper restConsumerDtoMapper;

    private static final String CALL = "Se realiza el llamado al Micro de Auth";
    private static final String AUTORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final String ERROR = "Se presento un error en el Micro";


    @Override
    public Mono<Claims> validateTokenAndGetClaims(String token) {
        logger.info(CALL);
        return client
                .get()
                .uri("/v1/validateToken")
                .header(AUTORIZATION, BEARER + token)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(ExternalErrorResponse.class)
                                .doOnNext(externalErrorResponse -> logger.info(ERROR))
                                .flatMap(error -> Mono.error(
                                        new ExternalServiceException(
                                                error.getStatus(),
                                                error.getMessage(),
                                                error.getBody()
                                        )
                                ))
                )
                .bodyToMono(ExternalClaimsResponse.class)
                .map(ExternalClaimsResponse::body)
                .doOnNext(claimsDto -> logger.info("Respuesta exitosa se mapea a un claismoDto"));
    }

    @Override
    public Mono<String> getUserEmailByDocument(String documentId) {
        logger.info(CALL);
        return client
                .post()
                .uri("/v1/usuarios/document")
                .bodyValue(new RequestDocumentId(documentId))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(ExternalErrorResponse.class)
                                .doOnNext(externalErrorResponse -> logger.info(ERROR))
                                .flatMap(error -> Mono.error(
                                        new ExternalServiceException(
                                                error.getStatus(),
                                                error.getMessage(),
                                                error.getBody()
                                        )
                                ))
                )
                .bodyToMono(ExternalUserResponse.class)
                .map(response -> response.getBody().email())
                .doOnNext(email -> logger.info("Se retorna a UseCase el email" + email + " "));
    }

    @Override
    public Mono<Map<String, User>> getUsersByEmails(Set<String> emails) {
        logger.info(CALL);
        return client
                .post()
                .uri("/v1/usuarios/mapEmails")
                .bodyValue(emails)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(ExternalErrorResponse.class)
                                .doOnNext(externalErrorResponse -> logger.info(ERROR))
                                .flatMap(error -> Mono.error(
                                        new ExternalServiceException(
                                                error.getStatus(),
                                                error.getMessage(),
                                                error.getBody()
                                        )
                                ))
                )
                .bodyToMono(ExternalUserListResponse.class)
                .map(resp -> {
                    var map = resp.body();
                    return map.entrySet().stream()
                            .filter(e -> e.getKey() != null && e.getValue() != null)
                            .collect(java.util.stream.Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> restConsumerDtoMapper.toDomain(e.getValue())
                            ));
                })
                .doOnNext(map -> logger.info("Usuarios recibidos desde Auth: " + map.size()));
    }
}

