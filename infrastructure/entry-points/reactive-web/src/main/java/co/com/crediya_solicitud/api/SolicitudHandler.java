package co.com.crediya_solicitud.api;

import co.com.crediya_solicitud.api.dto.SolicitudDto;
import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.api.mapper.SolicitudDtoMapper;
import co.com.crediya_solicitud.api.utils.ApiResponseBuilder;
import co.com.crediya_solicitud.api.utils.ValidateResponseToken;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.model.exception.specificexceptions.UnauthorizedException;
import co.com.crediya_solicitud.model.shared_token.AuthContext;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
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

import static co.com.crediya_solicitud.model.exception.SolicitudErrorCode.TOKEN_INVALID;

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

    private static final String STATE_DEFAULT = "estado-001";
    private static final String BEARER = "Bearer ";

    private SolicitudErrorCode mapMessageToErrorCode(String code) {
        return SolicitudErrorCode.fromCode(code);
    }
//primero
    public Mono<ServerResponse> createSolicitud(ServerRequest request) {
        logger.info("SolicitudHandler -> createSolicitud : inicia el flujo.");
        String header = request.headers().header("Authorization").stream()
                .filter(authHeader -> authHeader.startsWith(BEARER))
                .findFirst()
                .map(authHeader -> authHeader.substring(7))
                .orElse(null);

        if (header == null || header.isBlank()) {
            logger.info("SolicitudHandler -> createSolicitud : Token no proporcionado");
            return apiResponseBuilder.build(HttpStatus.UNAUTHORIZED, "Token no proporcionado", null);
        }

        String token = header.replace(BEARER, "").trim();


        logger.info("SolicitudHandler -> createSolicitud : Token proporcionado, se procede a validar el token");
        return userGateway.validateTokenAndGetClaims(token)
                .flatMap(validateResponseToken::isCustomer)
                .doOnNext(claimsDto -> logger.info("SolicitudHandler -> createSolicitud : es un Customer(cliente)"))

                .flatMap(claimsDto ->
                        request.bodyToMono(SolicitudDto.class)
                                .doOnNext(sub -> logger.info("SolicitudHandler -> createSolicitud : Nueva petición para crear solicitud, Dto recibido."))
                                .flatMap(solicitudDto ->
                                        validateResponseToken.isOwner(claimsDto, solicitudDto.documentId())
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
                    return solicitudService.createSolicitud(solicitud)
                            .doOnNext(sub -> logger.info("SolicitudHandler -> createSolicitud : Invocando a solicitudService.createSolicitud"))
                            .contextWrite(ctx -> ctx.put("authToken", token))
                            .map(solicitudDtoMapper::toDto);
                }).flatMap(responseSolicitudDto -> apiResponseBuilder.build(
                        HttpStatus.CREATED,
                        "Solicitud creada exitosamente",
                        responseSolicitudDto
                )).doOnSuccess(dto -> logger.info("SolicitudHandler -> createSolicitud : Usuario creado exitosamente"));
    }
//segundo
    public Mono<ServerResponse> findAll(ServerRequest request) {
        String rawToken = extraerToken(request);

        int page = request.queryParam("page").map(Integer::parseInt).orElse(0);
        int size = request.queryParam("size").map(Integer::parseInt).orElse(10);
        page = Math.max(0, page);
        size = Math.max(1, size);

        List<String> statuses = request.queryParam("status")
                .map(s -> Arrays.stream(s.split(","))
                        .map(String::trim)
                        .filter(str -> !str.isBlank())
                        .toList())
                .orElse(List.of(STATE_DEFAULT));

        long offset = (long) page * size;
        Pageable pageable = PageRequest.of(page, size);

        logger.info("findAll params -> status= " + statuses + ", page=" + page + ", size=" + size + ", offset=" + offset + " ");

        Mono<Void> authFlow = flujoAuth(rawToken, request).then();

        // Autenticar, contar, validar rango, traer página
        int finalSize = size;
        int finalPage = page;
        return authFlow.then(
                solicitudService.countByStatus(statuses)
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
                                    .contextWrite(ctx -> ctx.put(AuthContext.TOKEN_KEY, rawToken))
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
        );

    }

    private String extraerToken(ServerRequest request) {
        String auth = request.headers().firstHeader("Authorization");
        if (auth == null || !auth.startsWith(BEARER) || auth.length() == 7) {
            throw new UnauthorizedException(TOKEN_INVALID); // 401 con USR_015
        }
        return auth.substring(7).trim();
    }

    private Mono<?> flujoAuth(String rawToken, ServerRequest request) {
        return userGateway.validateTokenAndGetClaims(rawToken)
                .onErrorMap(ExternalServiceException.class, ex ->
                        new SolicitudValidationException(List.of(), List.of(SolicitudErrorCode.AUTH), ex.getBody()))
                .flatMap(validateResponseToken::isAdviser);
    }
}
