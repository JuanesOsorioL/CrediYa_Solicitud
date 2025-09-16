package co.com.crediya_solicitud.api.utils;

import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.model.claims.ClaismoDto;
import co.com.crediya_solicitud.model.exception.specificexceptions.BadRequestException;
import co.com.crediya_solicitud.model.exception.specificexceptions.ForbiddenException;
import co.com.crediya_solicitud.model.exception.specificexceptions.UnauthorizedException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static co.com.crediya_solicitud.model.exception.SolicitudErrorCode.*;

@Component
@AllArgsConstructor
public class ValidateResponseToken {

    private final GlobalLogger logger;
    private static final String ROLE_CUSTOMER = "Customer";
    private static final String ROLE_ADVISER = "Adviser";


    public Mono<ClaismoDto> validate(ClaismoDto claims) {
        return Mono.defer(() -> Mono.justOrEmpty(claims))
                .doOnSubscribe(s -> logger.info("ValidateResponseToken->validate: validando ClaimsDto"))
                .switchIfEmpty(Mono.error(new UnauthorizedException(TOKEN_INVALID)))
                .doOnNext(c -> logger.info("ValidateResponseToken->validate: ClaimsDto válido"));
    }

    public Mono<ClaismoDto> isCustomer(ClaismoDto claims) {
        return requireRole(claims, ROLE_CUSTOMER);
    }

    public Mono<ClaismoDto> isAdviser(ClaismoDto claims) {
        return requireRole(claims, ROLE_ADVISER);
    }

    public Mono<ClaismoDto> requireRole(ClaismoDto claims, String requiredRole) {
        return validate(claims)
                .handle((c, sink) -> {
                    if (!requiredRole.equalsIgnoreCase(safe(c.Rol()))) {
                        var code = requiredRole.equalsIgnoreCase("Customer")
                                ? AUTHORIZED_ONLY_CUSTOMER
                                : AUTHORIZED_ONLY_ADVISER;
                        sink.error(new ForbiddenException(code));
                    } else {
                        sink.next(c);
                    }
                })
                .cast(ClaismoDto.class)
                .doOnNext(c -> logger.info("ValidateResponseToken->requireRole: rol OK: " + c.Rol() + " "));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }


    public Mono<ClaismoDto> isOwner(ClaismoDto claims, String documentoSolicitud) {
        return Mono.defer(() -> {
            logger.info("ValidateResponseToken->isOwner: validando si es el mismo cliente");
            if (documentoSolicitud == null || documentoSolicitud.isBlank()) {
                return Mono.error(new BadRequestException(CLAIMS_DOCUMENT_NULL)); // 400
            }
            return Mono.just(claims)
                    .handle((c, sink) -> {
                        if (!safe(c.Document()).equals(documentoSolicitud)) {
                            logger.info("ValidateResponseToken->isOwner: intento ajeno (claim=" + c.Document() + ", req=" + documentoSolicitud + ")");
                            sink.error(new ForbiddenException(JUST_FOR_YOU)); // 403
                        } else {
                            sink.next(c);
                        }
                    })
                    .cast(ClaismoDto.class)
                    .doOnNext(c -> logger.info("ValidateResponseToken->isOwner: ownership OK"));
        });
    }

}