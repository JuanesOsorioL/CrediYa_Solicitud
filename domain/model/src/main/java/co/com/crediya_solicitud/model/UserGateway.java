package co.com.crediya_solicitud.model;

import reactor.core.publisher.Mono;

public interface UserGateway {
    Mono<String> getUserEmailByDocument(String documentId,String token);

    Mono<ClaimsDto> validateTokenAndGetClaims(String token);
}
