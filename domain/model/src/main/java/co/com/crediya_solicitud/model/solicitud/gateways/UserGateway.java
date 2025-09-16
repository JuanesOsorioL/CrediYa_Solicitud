package co.com.crediya_solicitud.model.solicitud.gateways;

import co.com.crediya_solicitud.model.claims.ClaismoDto;
import co.com.crediya_solicitud.model.user.User;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;

//-> interfaz para realizar el llamado al micro de authentication
public interface UserGateway {

    Mono<ClaismoDto> validateTokenAndGetClaims(String token);

    Mono<String> getUserEmailByDocument(String documentId);

    Mono<Map<String, User>> getUsersByEmails(Set<String> emails);


}
