package co.com.crediya_solicitud.api;

import co.com.crediya_solicitud.api.dto.ClaismoDto;
import co.com.crediya_solicitud.api.dto.DecisionDto;
import co.com.crediya_solicitud.api.dto.SolicitudDto;
import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.api.mapper.SolicitudDtoMapper;
import co.com.crediya_solicitud.api.utils.ApiResponseBuilder;
import co.com.crediya_solicitud.api.utils.ValidateResponseToken;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.model.exception.specificexceptions.UnauthorizedException;
import co.com.crediya_solicitud.model.logger.menssage.LogMessageService;
import co.com.crediya_solicitud.model.shared_token.AuthContext;
import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.sqs.SqsSendGateway;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import co.com.crediya_solicitud.usecase.solicitud.SolicitudService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static co.com.crediya_solicitud.model.exception.SolicitudErrorCode.TOKEN_EMPTY;
import static co.com.crediya_solicitud.model.exception.SolicitudErrorCode.TOKEN_INVALID;

@Component
@RequiredArgsConstructor
public class SolicitudHandler {


    private static final String STATE_DEFAULT = "estado-001";
    private static final String BEARER = "Bearer ";
    public static final String AUTHORIZATION = "Authorization";
    public static final String STATUS = "status";
    public static final String PAGE = "page";
    public static final String SIZE = "size";
    public static final String PAGE_SOLICITADA = "pageSolicitada";
    public static final String TOTAL_ELEMENTS = "totalElements";
    public static final String TOTAL_PAGES = "totalPages";
    public static final String MAX_PAGE_INDEX = "maxPageIndex";

    private final ApiResponseBuilder apiResponseBuilder;
    private final SolicitudService solicitudService;
    private final SolicitudDtoMapper solicitudDtoMapper;
    private final Validator validator;
    private final GlobalLogger logger;
    private final UserGateway userGateway;
    private final ValidateResponseToken validateResponseToken;
    private final SqsSendGateway sqsSendGateway;


    //primero
    public Mono<ServerResponse> createSolicitud(ServerRequest request) {
        LogMessageService.INICIA_EL_FLUJO.info(logger);
        return extraerToken(request)
                .flatMap(token -> flujoAuthCustomer()
                        .doOnNext(claimsDto -> LogMessageService.ES_UN_CUSTOMER_CLIENTE.info(logger))
                        .flatMap(claimsDto ->
                                request.bodyToMono(SolicitudDto.class)
                                        .doOnNext(sub -> LogMessageService.PETICION_PARA_CREAR_SOLICITUD_DTO_RECIBIDO.info(logger))
                                        .flatMap(this::validarInfra)
                                        .doOnNext(sub -> LogMessageService.NO_SE_ENCONTRARON_ERRORES_JAKARTA.info(logger))
                                        .flatMap(solicitudDto ->
                                                validateResponseToken.isOwner(claimsDto, solicitudDto.documentId())
                                                        .map(c -> solicitudDto)
                                                        .map(s -> new SolicitudDto(s.solicitudId(), s.amount(), s.documentId(), s.term(), claimsDto.sub(), s.stateId(), s.loanTypeId()))
                                        )
                        ).doOnNext(sub -> LogMessageService.TRANSFORMADO_A_DOMINIO_SOLICITUD.info(logger))
                        .map(solicitudDtoMapper::toSolicitud)
                        .doOnNext(sub -> LogMessageService.SERVICE_CREATE_SOLICITUD.info(logger))
                        .flatMap(solicitudService::createSolicitud)
                        .doOnNext(sub -> LogMessageService.DTO_PARA_LA_RESPUESTA.info(logger))
                        .map(solicitudDtoMapper::toDto)
                        .flatMap(responseSolicitudDto -> apiResponseBuilder.build(
                                HttpStatus.CREATED,
                                LogMessageService.SOLICITUD_CREADA_EXITOSAMENTE.fmt(),
                                responseSolicitudDto
                        ))
                        .contextWrite(ctx -> ctx.put(AuthContext.TOKEN_KEY, token))
                ).doOnSuccess(dto -> LogMessageService.CREADO_EXITOSAMENTE.info(logger));
    }

    //segundo
    public Mono<ServerResponse> findAll(ServerRequest request) {
        LogMessageService.INICIA_EL_FLUJO.info(logger);
        int page = request.queryParam(PAGE).map(Integer::parseInt).orElse(0);
        int size = request.queryParam(SIZE).map(Integer::parseInt).orElse(10);
        page = Math.max(0, page);
        size = Math.max(1, size);

        List<String> statuses = request.queryParam(STATUS)
                .map(s -> Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(str -> !str.isBlank())
                        .toList())
                .orElse(List.of(STATE_DEFAULT));

        long offset = (long) page * size;
        Pageable pageable = PageRequest.of(page, size);

        LogMessageService.PAGINATION.info(logger, statuses, page, size, offset);

        int finalSize = size;
        int finalPage = page;
        return extraerToken(request)
                .flatMap(token -> flujoAuthAdviser()
                        .then(solicitudService.countByStatus(statuses)
                                .doOnNext(total -> LogMessageService.CANTIDAD_SOLICITUDES.info(logger, statuses, total))
                                .flatMap(total -> {
                                    if (total == 0) {
                                        var empty = new PageImpl<List<?>>(List.of(), pageable, 0);
                                        return apiResponseBuilder.build(HttpStatus.OK, LogMessageService.NO_HAY_RESULTADOS_PARA_EL_ESTADO_SOLICITADO.fmt(), empty);
                                    }
                                    int totalPages = (int) Math.ceil((double) total / finalSize);
                                    if (offset >= total) {
                                        Map<String, Object> payload = getStringObjectMap(total, statuses, finalPage, finalSize, totalPages);

                                        return apiResponseBuilder.build(
                                                HttpStatus.BAD_REQUEST,
                                                LogMessageService.PARAMETROS_DE_PAGINACION_INVALIDOS_LA_PAGINA_SOLICITADA_ESTA_FUERA_DE_RANGO.fmt(),
                                                payload
                                        );
                                    }

                                    return solicitudService.getSolicitudByRevision(statuses, finalPage, finalSize)
                                            .map(solicitudDtoMapper::toSolicitudRevision)
                                            .collectList()
                                            .flatMap(content -> {
                                                var pageDto = new PageImpl<>(content, pageable, total);
                                                return apiResponseBuilder.build(
                                                        HttpStatus.OK,
                                                        LogMessageService.SOLICITUDES_RECUPERADAS_EXITOSAMENTE.fmt(),
                                                        pageDto
                                                );
                                            });
                                })

                        ).contextWrite(ctx -> ctx.put(AuthContext.TOKEN_KEY, token))
                );
    }

    private static Map<String, Object> getStringObjectMap(Long total, List<String> statuses, int finalPage, int finalSize, int totalPages) {
        return Map.of(
                STATUS, statuses.toString(),
                PAGE_SOLICITADA, finalPage,
                SIZE, finalSize,
                TOTAL_ELEMENTS, total,
                TOTAL_PAGES, totalPages,
                MAX_PAGE_INDEX, Math.max(0, totalPages - 1)
        );
    }

    private SolicitudErrorCode mapMessageToErrorCode(String code) {
        return SolicitudErrorCode.fromCode(code);
    }

    private Mono<String> extraerToken(ServerRequest request) {
        return Mono.justOrEmpty(request.headers().firstHeader(AUTHORIZATION))
                .switchIfEmpty(Mono.error(new UnauthorizedException(TOKEN_EMPTY)))
                .filter(auth -> auth.regionMatches(true, 0, BEARER, 0, BEARER.length()))
                .switchIfEmpty(Mono.error(new UnauthorizedException(TOKEN_INVALID)))
                .map(auth -> auth.substring(BEARER.length()).trim())
                .filter(token -> !token.isEmpty())
                .switchIfEmpty(Mono.error(new UnauthorizedException(TOKEN_INVALID)));
    }

    private Mono<ClaismoDto> flujoAuth(Function<ClaismoDto, Mono<ClaismoDto>> roleCheck) {
        return userGateway.validateTokenAndGetClaims()
                .map(solicitudDtoMapper::toClaismoDto)
                .onErrorMap(ExternalServiceException.class, ex ->
                        new SolicitudValidationException(List.of(), List.of(SolicitudErrorCode.AUTH), ex.getBody())
                )
                .flatMap(roleCheck);
    }

    private Mono<ClaismoDto> flujoAuthCustomer() {
        return flujoAuth(validateResponseToken::isCustomer);
    }

    private Mono<ClaismoDto> flujoAuthAdviser() {
        return flujoAuth(validateResponseToken::isAdviser);
    }

    private <T> Mono<T> validarInfra(T dto) {
        LogMessageService.ERRORES_DE_JAKARTA_DEL_REQUEST.info(logger);
        return Mono.defer(() -> {
            var infraErrors = validator.validate(dto).stream()
                    .map(v -> mapMessageToErrorCode(v.getMessage()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            if (!infraErrors.isEmpty()) {
                LogMessageService.ERRORES_INFRAESTRUCTURALES_DETECTADOS.info(logger);
                return Mono.error(new SolicitudValidationException(infraErrors, List.of(), null));
            }
            return Mono.just(dto);
        });
    }

    public Mono<ServerResponse> updateSolicitud(ServerRequest request) {
        LogMessageService.INICIA_EL_FLUJO_UPDATE.info(logger);
        return extraerToken(request)
                .flatMap(token ->
                        flujoAuthAdviser()
                                .doOnNext(x -> LogMessageService.SOLICITUD_ES_UN_ASESOR.info(logger))
                                .then(request.bodyToMono(DecisionDto.class))
                                .contextWrite(ctx -> ctx.put(AuthContext.TOKEN_KEY, token))
                )
                .doOnNext(dto -> LogMessageService.DTO_RECIBIDO.info(logger,dto.toString()))
                .flatMap(this::validarInfra)
                .doOnNext(dto -> LogMessageService.VALIDATE_OK_UPDATE.info(logger))
                .map(solicitudDtoMapper::toDecision)
                .flatMap(solicitudService::validateUpdateSolicitud)
                .flatMap(sqsSendGateway::notificarCambio)
                .flatMap(payloadEnviado ->
                        apiResponseBuilder.build(
                                HttpStatus.OK,
                                LogMessageService.SOLICITUD_DE_ACTUALIZACION_ENVIADA_EXITOSAMENTE.fmt(),
                                payloadEnviado
                        )
                )
                .doOnSuccess(x ->
                        LogMessageService.SOLICITUD_ENVIADA_CORRECTAMENTE.info(logger));

    }

}
