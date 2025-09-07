package co.com.crediya_solicitud.api;

import co.com.crediya_solicitud.api.dto.SolicitudDto;
import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.api.mapper.SolicitudDtoMapper;
import co.com.crediya_solicitud.api.utils.ApiResponseBuilder;
import co.com.crediya_solicitud.model.TokenDto;
import co.com.crediya_solicitud.model.UserGateway;
import co.com.crediya_solicitud.model.error.SolicitudErrorCode;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import co.com.crediya_solicitud.usecase.solicitud.SolicitudService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class SolicitudHandler {

    private final ApiResponseBuilder apiResponseBuilder;
    private final SolicitudService solicitudService;
    private final SolicitudDtoMapper solicitudDtoMapper;
    private final Validator validator;
    private final GlobalLogger logger;

    private final UserGateway userGateway;
    private final ValidateResponseToken validateResponseToken;


    private SolicitudErrorCode mapMessageToErrorCode(String code) {
        return SolicitudErrorCode.fromCode(code);
    }


    public Mono<ServerResponse> createSolicitud(ServerRequest request) {
        logger.info("SolicitudHandler -> createSolicitud : inicia el flujo.");
        String header = request.headers().header("Authorization").stream()
                .filter(authHeader -> authHeader.startsWith("Bearer "))
                .findFirst()
                .map(authHeader -> authHeader.substring(7))
                .orElse(null);

        if (header == null || header.isBlank()) {
            logger.info("SolicitudHandler -> createSolicitud : Token no proporcionado");
            return apiResponseBuilder.build(HttpStatus.UNAUTHORIZED, "Token no proporcionado", null);
        }

        String tokenSinBearer = header.replace("Bearer ", "").trim();
        TokenDto tokenDto = new TokenDto(tokenSinBearer);

        logger.info("SolicitudHandler -> createSolicitud : Token proporcionado, se procede a validar el token");
        return userGateway.validateTokenAndGetClaims(tokenDto)
                .flatMap(validateResponseToken::validate)
                .doOnNext(claimsDto -> logger.info("SolicitudHandler -> createSolicitud : Token válido"))
                .flatMap(claimsDto ->
                        request.bodyToMono(SolicitudDto.class)
                                .doOnNext(sub -> logger.info("SolicitudHandler -> createSolicitud : Nueva petición para crear solicitud, Dto recibido."))
                                .flatMap(solicitudDto ->
                                        validateResponseToken.isOwner(claimsDto, solicitudDto.document_id())
                                                .map(c -> solicitudDto)
                                )
                ).flatMap(solicitudDto -> {
                    logger.info("SolicitudHandler -> createSolicitud : se verifican los errores de jakarta del request");
                    List<SolicitudErrorCode> infraErrors = validator.validate(solicitudDto).stream()
                            .map(v -> mapMessageToErrorCode(v.getMessage()))
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();
                    if (!infraErrors.isEmpty()) {
                        logger.error("SolicitudHandler -> createSolicitud : Errores infraestructurales detectados");
                        return Mono.error(new SolicitudValidationException(infraErrors, List.of(), null));
                    }

                    Solicitud solicitud = solicitudDtoMapper.toSolicitud(solicitudDto);
                    logger.info("SolicitudHandler -> createSolicitud : Validaciones correctas, transformado a dominio");
                    return solicitudService.createSolicitud(solicitud, tokenDto)
                            .doOnNext(sub -> logger.info("SolicitudHandler -> createSolicitud : Invocando a solicitudService.createSolicitud"))
                            .map(solicitudDtoMapper::toDto);
                }).flatMap(responseSolicitudDto -> apiResponseBuilder.build(
                        HttpStatus.CREATED,
                        "Solicitud creada exitosamente",
                        responseSolicitudDto
                )).doOnSuccess(dto -> logger.info("SolicitudHandler -> createSolicitud : Usuario creado exitosamente"));
    }


    public Mono<ServerResponse> findAll(ServerRequest serverRequest) {
        return solicitudService.getAllSolicitud()
                .doOnSubscribe(sub -> logger.info("findAll suscrito"))
                .doOnNext(u -> logger.info("Se retornan todas las solicitudes"))
                .map(solicitudDtoMapper::toSolicitud)
                .doOnNext(u -> logger.info("Se convierten a DTO"))
                .collectList()
                .doOnNext(u -> logger.info("Se agrupan en una Lista"))
                .flatMap(list -> apiResponseBuilder.build(HttpStatus.OK, "Solicitudes recuperadas exitosamente", list))
                .onErrorResume(e -> apiResponseBuilder.build(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno", List.of("Error al recuperar Solicitudes")));
    }


}
