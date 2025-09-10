package co.com.crediya_solicitud.model.solicitud.gateways;

import co.com.crediya_solicitud.model.claims.Claims;
import co.com.crediya_solicitud.model.user.User;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

public interface UserGateway {
    Mono<Claims> validateTokenAndGetClaims(String token);

    Mono<String> getUserEmailByDocument(String documentId);

    Mono<Map<String, User>> getUsersByEmails(Set<String> emails);


}
