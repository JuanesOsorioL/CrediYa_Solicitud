package co.com.crediya_solicitud.api;

import co.com.crediya_solicitud.api.dto.SolicitudDto;
import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.api.mapper.SolicitudDtoMapper;
import co.com.crediya_solicitud.api.utils.ApiResponseBuilder;
import co.com.crediya_solicitud.model.UserGateway;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.usecase.solicitud.SolicitudService;
import co.com.crediya_solicitud.model.error.SolicitudErrorCode;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
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

    private SolicitudErrorCode mapMessageToErrorCode(String code) {
        return SolicitudErrorCode.fromCode(code);
    }

    public Mono<ServerResponse> createSolicitud(ServerRequest request) {
        return request.bodyToMono(SolicitudDto.class)
                .doOnSubscribe(sub -> logger.info("Nueva petición para crear solicitud"))
                .doOnNext(dto -> logger.info("DTO recibido"))
                .flatMap(dto -> {
                    List<SolicitudErrorCode> infraErrors = validator.validate(dto).stream()
                            .map(v -> mapMessageToErrorCode(v.getMessage()))
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();

                    if (!infraErrors.isEmpty()) {
                        logger.error("Errores infraestructurales detectados");
                        return Mono.error(new SolicitudValidationException(infraErrors, List.of(),null));
                    }

                    Solicitud solicitud = solicitudDtoMapper.toSolicitud(dto);
                    logger.info("Validaciones correctas, transformado a dominio");
                    return solicitudService.createSolicitud(solicitud)
                            .doOnSubscribe(sub -> logger.info("Invocando solicitudService.createSolicitud"))
                            .doOnNext(u -> logger.info("Solicitud persistido"))
                            .map(solicitudDtoMapper::toDto)
                            .flatMap(solicitudDto -> apiResponseBuilder.build(
                                    HttpStatus.CREATED,
                                    "Solicitud creada exitosamente",
                                    solicitudDto
                            ));
                }).doOnSuccess(dto -> logger.info("Usuario creado exitosamente"));
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
