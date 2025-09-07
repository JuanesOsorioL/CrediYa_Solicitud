package co.com.crediya_solicitud.api;

import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.model.ClaimsDto;
import co.com.crediya_solicitud.model.error.SolicitudErrorCode;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@AllArgsConstructor
public class ValidateResponseToken {
    private final GlobalLogger logger;

    public Mono<ClaimsDto> validate(ClaimsDto claims) {
        logger.info("ValidateResponseToken-> validate : se valida el ClaismoDto");

        if (claims == null) {
            logger.info("ValidateResponseToken-> validate : es nulo -> Token invalido");
            return Mono.error(new SolicitudValidationException(
                    List.of(),
                    List.of(SolicitudErrorCode.TOKEN_INVALID),
                    List.of()
            ));

        } else if (!"Customer".equalsIgnoreCase(claims.Rol())) {
            logger.info("ValidateResponseToken-> validate: No tiene permisos, No eres un Cliente eres un " + claims.Rol() + " ");
            logger.info("Rol exacto recibido: [" + claims.Rol() + "]");
            return Mono.error(new SolicitudValidationException(
                    List.of(),
                    List.of(SolicitudErrorCode.USER_UNAUTHORIZED),
                    List.of()
            ));

        } else {
            logger.info("ValidateResponseToken-> validate: Usuario válido, eres un Customer");
            return Mono.just(claims);
        }

    }

    public Mono<ClaimsDto> isOwner(ClaimsDto claims, String documentoSolicitud) {
        logger.info("ValidateResponseToken-> isOwner : se valida el docuemnto que contiene el ClaismoDto, con el documento suministrado en el request (Json)");
        return Mono.defer(() -> {
            if (claims == null || documentoSolicitud == null) {
                logger.info("ValidateResponseToken-> isOwner : El docuemnto o el ClaismoDto son null)");
                return Mono.error(new SolicitudValidationException(
                        List.of(),
                        List.of(SolicitudErrorCode.CLAIMS_DOCUMENT_NULL),
                        List.of()
                ));
            }
            if (!claims.Document().equals(documentoSolicitud)) {
                logger.info("ValidateResponseToken-> isOwner: El usuario intenta crear una solicitud que no es propia");
                logger.info("Rol exacto recibido: [" + claims.Document() + "]");
                logger.info("Rol exacto recibido: [" + documentoSolicitud + "]");
                return Mono.error(new SolicitudValidationException(
                        List.of(),
                        List.of(SolicitudErrorCode.JUST_FOR_YOU),
                        List.of()
                ));
            }

            logger.info("ValidateResponseToken-> isOwner: Usuario autorizado, es posible crear la solicitud");
            return Mono.just(claims);
        });
    }
}
