package co.com.crediya_solicitud.api;

import co.com.crediya_solicitud.api.dto.SolicitudDto;
import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.api.mapper.SolicitudDtoMapper;
import co.com.crediya_solicitud.api.utils.ApiResponseBuilder;
import co.com.crediya_solicitud.model.UserGateway;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.usecase.solicitud.SolicitudService;
import co.com.crediya_solicitud.usecase.solicitud.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.usecase.solicitud.exception.SolicitudValidationException;
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

    private static final Map<String, SolicitudErrorCode> CODE_TO_ERROR_MAP = Map.of(
            "USR_001", SolicitudErrorCode.DOCUMENT_EMPTY,
            "USR_002", SolicitudErrorCode.TERM_EMPTY,
            "USR_003", SolicitudErrorCode.EMAIL_INVALID,
            "USR_004", SolicitudErrorCode.ID_STATE_EMPTY,
            "USR_005", SolicitudErrorCode.LOAN_TYPE_NOT_REGISTERED,
            "USR_006", SolicitudErrorCode.EMAIL_EMPTY,
            "USR_007", SolicitudErrorCode.AMOUNT_EMPTY,
            "USR_008", SolicitudErrorCode.ID_LOAN_TYPE_EMPTY,
            "USR_999", SolicitudErrorCode.GENERIC_ERROR
    );

    private SolicitudErrorCode mapMessageToErrorCode(String code) {
        return CODE_TO_ERROR_MAP.get(code);
    }

    public Mono<ServerResponse> createSolicitud(ServerRequest request) {
        return request.bodyToMono(SolicitudDto.class)
                .flatMap(dto -> {
                    List<SolicitudErrorCode> infraErrors = validator.validate(dto).stream()
                            .map(v -> mapMessageToErrorCode(v.getMessage()))
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();

                    if (!infraErrors.isEmpty()) {
                        return Mono.error(new SolicitudValidationException(infraErrors, List.of(),null));
                    }
                    Solicitud solicitud = solicitudDtoMapper.toSolicitud(dto);
                    return solicitudService.createSolicitud(solicitud)
                            .map(solicitudDtoMapper::toDto)
                            .flatMap(s -> apiResponseBuilder.build(
                                    HttpStatus.CREATED,
                                    "Solicitud creada exitosamente",
                                    s
                            ));
                });
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
