package co.com.crediya_solicitud.model.solicitud.gateways;

import co.com.crediya_solicitud.model.claims.Claismo;
import co.com.crediya_solicitud.model.user.User;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

//-> interfaz para realizar el llamado al micro de authentication
public interface UserGateway {

    Mono<Claismo> validateTokenAndGetClaims();

//    Mono<String> getUserEmailByDocument(String documentId);

    Mono<Map<String, User>> getUsersByEmails(List<String> emails);

}
