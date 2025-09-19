package co.com.crediya_solicitud.consumer;

import co.com.crediya_solicitud.consumer.dto.EmailsRequestDto;
import co.com.crediya_solicitud.consumer.dto.RequestDocumentId;
import co.com.crediya_solicitud.consumer.dto.response.ExternalClaimsResponse;
import co.com.crediya_solicitud.consumer.dto.response.ExternalErrorResponse;
import co.com.crediya_solicitud.consumer.dto.response.ExternalUserListResponse;
import co.com.crediya_solicitud.consumer.dto.response.ExternalUserResponse;
import co.com.crediya_solicitud.consumer.mapper.RestConsumerDtoMapper;
import co.com.crediya_solicitud.model.claims.Claismo;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class RestConsumer implements UserGateway {

    private final WebClient client;
    private final Logger logger;
    private final RestConsumerDtoMapper restConsumerDtoMapper;

    private static final String DOCUMENT = "/api/v1/document";
    private static final String VALIDATE_TOKEN = "/api/v1/validateToken";
    private static final String USERS_BY_EMAILS = "/api/v1/map";


    @Override
    public Mono<Claismo> validateTokenAndGetClaims() {
        logger.info("RestConsumer -> validateTokenAndGetClaims : Se realiza el llamado al Micro de Auth");
        return client
                .get()
                .uri(VALIDATE_TOKEN)
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
                ).bodyToMono(ExternalClaimsResponse.class)
                .map(ExternalClaimsResponse::body)
                .map(restConsumerDtoMapper::toClaismo)
                .doOnNext(claimsDto -> logger.info("RestConsumer -> validateTokenAndGetClaims :Respuesta exitosa se mapea a un ClaismoDto"));
    }
//
//    @Override
//    public Mono<String> getUserEmailByDocument(String documentId) {
//        logger.info("RestConsumer -> getUserEmailByDocument : Se realiza el llamado al Micro de Auth");
//        return client
//                .post()
//                .uri(DOCUMENT)
//                .bodyValue(new RequestDocumentId(documentId))
//                .retrieve()
//                .onStatus(
//                        status -> status.is4xxClientError() || status.is5xxServerError(),
//                        response -> response.bodyToMono(ExternalErrorResponse.class)
//                                .doOnNext(externalErrorResponse -> logger.info("RestConsumer -> getUserEmailByDocument : Se presento un error en el Micro"))
//                                .flatMap(error -> Mono.error(
//                                        new ExternalServiceException(
//                                                error.getStatus(),
//                                                error.getCode(),
//                                                error.getMessage(),
//                                                error.getBody()
//                                        )
//                                ))
//                )
//                .bodyToMono(ExternalUserResponse.class)
//                .map(response -> response.getBody().email())
//                .doOnNext(email -> logger.info("RestConsumer -> getUserEmailByDocument : Se retorna a UseCase el email" + email + " "));
//    }


    @Override
    public Mono<Map<String, User>> getUsersByEmails(List<String> emails) {
        logger.info("RestConsumer -> getUsersByEmails : Se realiza el llamado al Micro de Auth");
        return client
                .post()
                .uri(USERS_BY_EMAILS)
                .bodyValue(new EmailsRequestDto(emails))
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
                    var map = resp.body().users();
                    return map.entrySet().stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> restConsumerDtoMapper.toDomain(e.getValue())
                            ));
                }).doOnNext(mapa -> logger.info("RestConsumer -> getUsersByEmails : Se retorna los usuarios por email " + mapa.toString()));
    }
}

