package co.com.crediya_solicitud.consumer;

import co.com.crediya_solicitud.consumer.dto.RequestDocumentId;
import co.com.crediya_solicitud.consumer.dto.response.ExternalClaimsResponse;
import co.com.crediya_solicitud.consumer.dto.response.ExternalErrorResponse;
import co.com.crediya_solicitud.consumer.dto.response.ExternalUserListResponse;
import co.com.crediya_solicitud.consumer.dto.response.ExternalUserResponse;
import co.com.crediya_solicitud.consumer.mapper.RestConsumerDtoMapper;
import co.com.crediya_solicitud.model.claims.ClaismoDto;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
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

    private static final String AUTORIZATION = "Authorization";
    private static final String BEARER = "Bearer ";
    private static final String DOCUMENT = "/v1/usuarios/document";
    private static final String VALIDATE_TOKEN = "/v1/validateToken";
    private static final String USERS_BY_EMAILS = "/v1/usuarios/mapEmails";


    @Override
    public Mono<ClaismoDto> validateTokenAndGetClaims(String token) {
        logger.info("RestConsumer -> validateTokenAndGetClaims : Se realiza el llamado al Micro de Auth");
        return client
                .get()
                .uri(VALIDATE_TOKEN)
                .header(AUTORIZATION, BEARER + token)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(ExternalErrorResponse.class)
                                .doOnNext(externalErrorResponse -> logger.info("RestConsumer -> validateTokenAndGetClaims : Se presento un error en el Micro"))
                                .flatMap(error -> Mono.error(
                                        new ExternalServiceException(
                                                error.getStatus(),
                                                error.getCode(),
                                                error.getMessage(),
                                                error.getBody()

                                        )
                                ))
                )
                .bodyToMono(new ParameterizedTypeReference<ExternalClaimsResponse<ClaismoDto>>() {
                })
                .map(ExternalClaimsResponse::body)
                .doOnNext(claimsDto -> logger.info("RestConsumer -> getUsersByEmails :Respuesta exitosa se mapea a un claismoDto"));
    }


    @Override
    public Mono<String> getUserEmailByDocument(String documentId) {
        logger.info("RestConsumer -> getUserEmailByDocument : Se realiza el llamado al Micro de Auth");
        return client
                .post()
                .uri(DOCUMENT)
                .bodyValue(new RequestDocumentId(documentId))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(ExternalErrorResponse.class)
                                .doOnNext(externalErrorResponse -> logger.info("RestConsumer -> getUserEmailByDocument : Se presento un error en el Micro"))
                                .flatMap(error -> Mono.error(
                                        new ExternalServiceException(
                                                error.getStatus(),
                                                error.getCode(),
                                                error.getMessage(),
                                                error.getBody()
                                        )
                                ))
                )
                .bodyToMono(ExternalUserResponse.class)
                .map(response -> response.getBody().email())
                .doOnNext(email -> logger.info("RestConsumer -> getUsersByEmails : Se retorna a UseCase el email" + email + " "));
    }

    @Override
    public Mono<Map<String, User>> getUsersByEmails(Set<String> emails) {
        logger.info("RestConsumer -> getUsersByEmails : Se realiza el llamado al Micro de Auth");
        return client
                .post()
                .uri(USERS_BY_EMAILS)
                .bodyValue(emails)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(ExternalErrorResponse.class)
                                .doOnNext(externalErrorResponse -> logger.info("RestConsumer -> getUsersByEmails : Se presento un error en el Micro"))
                                .flatMap(error -> Mono.error(
                                        new ExternalServiceException(
                                                error.getStatus(),
                                                error.getCode(),
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
                .doOnNext(map -> logger.info("\"RestConsumer -> getUsersByEmails : Usuarios recibidos desde Auth: " + map.size()));
    }
}

