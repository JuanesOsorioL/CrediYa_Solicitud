package co.com.crediya_solicitud.api.utils;

import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.model.claims.Claims;
import co.com.crediya_solicitud.model.error.SolicitudErrorCode;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

import static co.com.crediya_solicitud.model.error.SolicitudErrorCode.AUTHORIZED_ONLY_ADVISER;
import static co.com.crediya_solicitud.model.error.SolicitudErrorCode.AUTHORIZED_ONLY_CUSTOMER;

@Component
@AllArgsConstructor
public class ValidateResponseToken {

    private final GlobalLogger logger;
    private static final String ROLE_CUSTOMER = "Customer";
    private static final String ROLE_ADVISER = "Adviser";


    public Mono<Claims> validate(Claims claims) {
        return Mono.defer(() -> Mono.justOrEmpty(claims))
                .doOnSubscribe(s -> logger.info("ValidateResponseToken->validate: validando ClaimsDto"))
                .switchIfEmpty(Mono.error(new SolicitudValidationException(
                        List.of(), List.of(SolicitudErrorCode.TOKEN_INVALID), List.of())))
                .doOnNext(c -> logger.info("ValidateResponseToken->validate: ClaimsDto válido"));
    }

    public Mono<Claims> isCustomer(Claims claims) {
        return requireRole(claims, ROLE_CUSTOMER);
    }

    public Mono<Claims> isAdviser(Claims claims) {
        return requireRole(claims, ROLE_ADVISER);
    }

    public Mono<Claims> requireRole(Claims claims, String requiredRole) {
        return validate(claims)
                .filter(c -> requiredRole.equalsIgnoreCase(safe(c.Rol())))
                .switchIfEmpty(Mono.error(new SolicitudValidationException(
                        List.of(),
                        requiredRole.equalsIgnoreCase(safe(ROLE_CUSTOMER)) ?
                                List.of(AUTHORIZED_ONLY_CUSTOMER) :
                                List.of(AUTHORIZED_ONLY_ADVISER),
                        List.of())))
                .doOnNext(c -> logger.info("ValidateResponseToken->requireRole: rol OK: " + c.Rol() + " "));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }


    public Mono<Claims> isOwner(Claims claims, String documentoSolicitud) {
        return validate(claims)
                .doOnSubscribe(s -> logger.info("ValidateResponseToken->isOwner: validando ownership"))
                .handle((c, sink) -> {
                    if (documentoSolicitud == null || documentoSolicitud.isBlank()) {
                        sink.error(new SolicitudValidationException(
                                List.of(), List.of(SolicitudErrorCode.CLAIMS_DOCUMENT_NULL), List.of()));
                        return;
                    }
                    if (!safe(c.Document()).equals(documentoSolicitud)) {
                        logger.info("ValidateResponseToken->isOwner: intento de crear solicitud ajena ( claim = " + c.Document() + " , req = " + documentoSolicitud + " ");
                        sink.error(new SolicitudValidationException(
                                List.of(), List.of(SolicitudErrorCode.JUST_FOR_YOU), List.of()));
                        return;
                    }
                    sink.next(c);
                })
                .cast(Claims.class)
                .doOnNext(c -> logger.info("ValidateResponseToken->isOwner: ownership OK"));
    }
}