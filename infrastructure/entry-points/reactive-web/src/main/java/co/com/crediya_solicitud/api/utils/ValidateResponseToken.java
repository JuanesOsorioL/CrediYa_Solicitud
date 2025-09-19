package co.com.crediya_solicitud.api.utils;

import co.com.crediya_solicitud.api.dto.ClaismoDto;
import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.model.exception.specificexceptions.ForbiddenException;
import co.com.crediya_solicitud.model.exception.specificexceptions.UnauthorizedException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static co.com.crediya_solicitud.model.exception.SolicitudErrorCode.*;

@Component
@AllArgsConstructor
public class ValidateResponseToken {

    public static final String VALIDANDO_CLAIMS_DTO = "ValidateResponseToken -> validate : validando ClaimsDto";
    public static final String DTO_VALIDO = "ValidateResponseToken -> validate : ClaimsDto válido";
    public static final String NO_ERES_UN_CLIENTE_ERES_UN = "ValidateResponseToken -> requireRole : No eres un cliente eres un : ";
    public static final String ROL_OK = "ValidateResponseToken -> requireRole : rol OK : ";
    public static final String NO_ERES_EL_MISMO_CLIENTE = "ValidateResponseToken -> isOwner : intento ajeno  no eres el mismo cliente ";
    public static final String CLIENTE_OK = "ValidateResponseToken -> isOwner : Eres el mismo cliente -> OK";
    private final GlobalLogger logger;
    private static final String ROLE_CUSTOMER = "Customer";
    private static final String ROLE_ADVISER = "Adviser";

    public Mono<ClaismoDto> validate(ClaismoDto claismoDto) {
        return Mono.justOrEmpty(claismoDto)
                .doOnSubscribe(s -> logger.info(VALIDANDO_CLAIMS_DTO))
                .switchIfEmpty(Mono.error(new UnauthorizedException(TOKEN_INVALID)))
                .doOnNext(c -> logger.info(DTO_VALIDO));
    }

    public Mono<ClaismoDto> requireRole(ClaismoDto claismoDto, String requiredRole) {
        var code = requiredRole.equalsIgnoreCase(ROLE_CUSTOMER)
                ? AUTHORIZED_ONLY_CUSTOMER
                : AUTHORIZED_ONLY_ADVISER;

        return validate(claismoDto)
                .filter(c -> requiredRole.equalsIgnoreCase(c.Rol()))
                .switchIfEmpty(Mono.defer(() -> {
                    logger.info(NO_ERES_UN_CLIENTE_ERES_UN + claismoDto.Rol());
                    return Mono.error(new ForbiddenException(code));
                })).doOnNext(c -> logger.info(ROL_OK + c.Rol()));
    }

    public Mono<ClaismoDto> isCustomer(ClaismoDto claismoDto) {
        return requireRole(claismoDto, ROLE_CUSTOMER);
    }

    public Mono<ClaismoDto> isAdviser(ClaismoDto claismoDto) {
        return requireRole(claismoDto, ROLE_ADVISER);
    }

    public Mono<ClaismoDto> isOwner(ClaismoDto claismoDto, String documentoSolicitud) {
        return Mono.just(claismoDto)
                .filter(c -> c.Document().equals(documentoSolicitud))
                .switchIfEmpty(Mono.defer(() -> {
                    logger.info(NO_ERES_EL_MISMO_CLIENTE + "( Login = { " + claismoDto.Document() + " }, solicitud = { " + documentoSolicitud + " } )");
                    return Mono.error(new ForbiddenException(JUST_FOR_YOU));
                }))
                .doOnNext(dto -> logger.info(CLIENTE_OK));
    }
}