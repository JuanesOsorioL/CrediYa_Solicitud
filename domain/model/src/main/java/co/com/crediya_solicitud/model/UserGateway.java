package co.com.crediya_solicitud.model;

import reactor.core.publisher.Mono;

public interface UserGateway {
    Mono<String> getUserEmailByDocument(String documentId,TokenDto token);

    Mono<ClaimsDto> validateTokenAndGetClaims(TokenDto token);
}
