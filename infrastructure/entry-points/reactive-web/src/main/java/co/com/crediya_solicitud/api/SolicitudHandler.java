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

    public static final String AUTHORIZATION = "Authorization";
    private static final String STATE_DEFAULT = "estado-001";
    private static final String BEARER = "Bearer ";
    public static final String VALIDAR_EL_TOKEN = "SolicitudHandler -> flujoAuth : Token proporcionado, se procede a validar el token";
    public static final String ERRORES_DE_JAKARTA_DEL_REQUEST = "SolicitudHandler -> validarInfra : se verifican los errores de jakarta del request";
    public static final String ERRORES_INFRAESTRUCTURALES_DETECTADOS = "SolicitudHandler -> validarInfra : Errores infraestructurales detectados";
    public static final String ES_UN_CUSTOMER_CLIENTE = "SolicitudHandler -> createSolicitud : es un Customer(cliente)";
    public static final String PETICION_PARA_CREAR_SOLICITUD_DTO_RECIBIDO = "SolicitudHandler -> createSolicitud : Nueva petición para crear solicitud, Dto recibido.";
    public static final String NO_SE_ENCONTRARON_ERRORES_JAKARTA = "SolicitudHandler -> createSolicitud : No se encontraron Errores jakarta";
    public static final String TRANSFORMADO_A_DOMINIO_SOLICITUD = "SolicitudHandler -> createSolicitud : transformado a dominio (Solicitud)";
    public static final String SERVICE_CREATE_SOLICITUD = "SolicitudHandler -> createSolicitud : Invocando a solicitudService.createSolicitud";
    public static final String DTO_PARA_LA_RESPUESTA = "SolicitudHandler -> createSolicitud : transformado a dto para la respuesta";
    public static final String CREADO_EXITOSAMENTE = "SolicitudHandler -> createSolicitud : Usuario creado exitosamente";
    public static final String STATUS = "status";


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
        logger.info("SolicitudHandler -> createSolicitud : inicia el flujo.");
        return extraerToken(request)
                .flatMap(token -> flujoAuthCustomer()
                        .doOnNext(claimsDto -> logger.info(ES_UN_CUSTOMER_CLIENTE))
                        .flatMap(claimsDto ->
                                request.bodyToMono(SolicitudDto.class)
                                        .doOnNext(sub -> logger.info(PETICION_PARA_CREAR_SOLICITUD_DTO_RECIBIDO))
                                        .flatMap(this::validarInfra)
                                        .doOnNext(sub -> logger.info(NO_SE_ENCONTRARON_ERRORES_JAKARTA))
                                        .flatMap(solicitudDto ->
                                                validateResponseToken.isOwner(claimsDto, solicitudDto.documentId())
                                                        .map(c -> solicitudDto)
                                                        .map(s -> new SolicitudDto(s.solicitudId(), s.amount(), s.documentId(), s.term(), claimsDto.sub(), s.stateId(), s.loanTypeId()))
                                        )
                        ).doOnNext(sub -> logger.info(TRANSFORMADO_A_DOMINIO_SOLICITUD))
                        .map(solicitudDtoMapper::toSolicitud)
                        .doOnNext(sub -> logger.info(SERVICE_CREATE_SOLICITUD))
                        .flatMap(solicitudService::createSolicitud)
                        .doOnNext(sub -> logger.info(DTO_PARA_LA_RESPUESTA))
                        .map(solicitudDtoMapper::toDto)
                        .flatMap(responseSolicitudDto -> apiResponseBuilder.build(
                                HttpStatus.CREATED,
                                "Solicitud creada exitosamente",
                                responseSolicitudDto
                        ))
                        .contextWrite(ctx -> ctx.put(AuthContext.TOKEN_KEY, token))

                ).doOnSuccess(dto -> logger.info(CREADO_EXITOSAMENTE));
    }

    //segundo
    public Mono<ServerResponse> findAll(ServerRequest request) {
        logger.info("SolicitudHandler -> findAll : inicia el flujo.");
        int page = request.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = request.queryParam("size").map(Integer::parseInt).orElse(10);
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

        logger.info("findAll params -> status= " + statuses + ", page=" + page + ", size=" + size + ", offset=" + offset + " ");

        int finalSize = size;
        int finalPage = page;
        return extraerToken(request)
                .flatMap(token -> flujoAuthAdviser()

                        .then(solicitudService.countByStatus(statuses)
                                .doOnNext(total -> logger.info("countByStatus status=" + statuses + ", total= " + total + " "))
                                .flatMap(total -> {
                                    if (total == 0) {
                                        var empty = new PageImpl<List<?>>(List.of(), pageable, 0);
                                        return apiResponseBuilder.build(HttpStatus.OK, "No hay resultados para el estado solicitado", empty);
                                    }
                                    int totalPages = (int) Math.ceil((double) total / finalSize);
                                    if (offset >= total) {//paginacion fuera de rango
                                        Map<String, Object> payload = Map.of(
                                                "status", statuses.toString(),
                                                "pageSolicitada", finalPage,
                                                "size", finalSize,
                                                "totalElements", total,
                                                "totalPages", totalPages,
                                                "maxPageIndex", Math.max(0, totalPages - 1)
                                        );

                                        return apiResponseBuilder.build(
                                                HttpStatus.BAD_REQUEST,
                                                "Parámetros de paginación inválidos: la página solicitada está fuera de rango",
                                                payload
                                        );
                                    }

                                    // Rango OK, traemos la página y mapeamos a DTO
                                    return solicitudService.getSolicitudByRevision(statuses, finalPage, finalSize)
                                            .map(solicitudDtoMapper::toSolicitudRevision)
                                            .collectList()
                                            .flatMap(content -> {
                                                var pageDto = new PageImpl<>(content, pageable, total);
                                                return apiResponseBuilder.build(
                                                        HttpStatus.OK,
                                                        "Solicitudes recuperadas exitosamente",
                                                        pageDto
                                                );
                                            });
                                })

                        ).contextWrite(ctx -> ctx.put(AuthContext.TOKEN_KEY, token))
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
        logger.info(ERRORES_DE_JAKARTA_DEL_REQUEST);
        return Mono.defer(() -> {
            var infraErrors = validator.validate(dto).stream()
                    .map(v -> mapMessageToErrorCode(v.getMessage()))
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            if (!infraErrors.isEmpty()) {
                logger.error(ERRORES_INFRAESTRUCTURALES_DETECTADOS);
                return Mono.error(new SolicitudValidationException(infraErrors, List.of(), null));
            }
            return Mono.just(dto);
        });
    }

    public Mono<ServerResponse> updateSolicitud(ServerRequest request) {
        logger.info("SolicitudHandler -> updateSolicitud : inicia el flujo.");
        return extraerToken(request)
                .flatMap(token ->
                        flujoAuthAdviser()
                                .doOnNext(x -> logger.info("SolicitudHandler -> updateSolicitud : es un Asesor"))
                                .then(request.bodyToMono(DecisionDto.class))
                                .contextWrite(ctx -> ctx.put(AuthContext.TOKEN_KEY, token))
                )
                .doOnNext(dto -> logger.info("SolicitudHandler -> updateSolicitud : DTO recibido: " + dto))
                .flatMap(this::validarInfra)
                .doOnNext(dto -> logger.info("SolicitudHandler -> updateSolicitud : validaciones OK"))
                .map(solicitudDtoMapper::toDecision)
                .flatMap(solicitudService::validateUpdateSolicitud)
                .flatMap(sqsSendGateway::notificarCambio)
                .flatMap(payloadEnviado ->
                        apiResponseBuilder.build(
                                HttpStatus.OK,
                                "Solicitud de actualización enviada exitosamente",
                                payloadEnviado
                        )
                )
                .doOnSuccess(x -> logger.info("SolicitudHandler -> updateSolicitud : Solicitud de actualización enviada exitosamente"));

    }

}
