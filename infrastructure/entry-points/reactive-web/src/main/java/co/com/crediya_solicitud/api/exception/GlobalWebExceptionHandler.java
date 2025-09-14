package co.com.crediya_solicitud.api.exception;


import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.api.utils.ApiResponseBuilder;
import co.com.crediya_solicitud.model.exception.DomainException;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Component
public class GlobalWebExceptionHandler implements WebExceptionHandler {


    private final ApiResponseBuilder apiResponseBuilder;
    private final GlobalLogger logger;
    private final DomainHttpStatusMapper statusMapper;

    public GlobalWebExceptionHandler(ApiResponseBuilder builder, GlobalLogger logger, DomainHttpStatusMapper statusMapper) {
        this.apiResponseBuilder = builder;
        this.logger = logger;
        this.statusMapper = statusMapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        logger.warn("GlobalWebExceptionHandler -> handle : inicia el flujo de excepcion");
        if (exchange.getResponse().isCommitted()) return Mono.error(ex);

        if (ex instanceof DomainException de) {
            var status = statusMapper.toHttpStatus(de.kind());
            var body = Map.<String, Object>of(
                    "kind", de.kind().name(),
                    "code", de.code(),
                    "errors", de.errors() == null ? List.of()
                            : de.errors().stream()
                            .map(SolicitudErrorCode::getMessage)
                            .distinct()
                            .collect(Collectors.toList())
            );

            if (status.is5xxServerError()) logger.error("DomainException TECHNICAL", de);
            else logger.warn("DomainException " + de.kind() + ": " + de.getMessage());

            return apiResponseBuilder.build(status, de.getMessage(), body)
                    .flatMap(resp -> resp.writeTo(exchange, ResponseContext.DEFAULT));
        }


        if (ex instanceof ExternalServiceException ese) {
            logger.warn("Error del micro Auth: status= " + ese.getStatus() + ", code= " + ese.getCode() + " message= " + ese.getMessage());
            return apiResponseBuilder.build(
                            HttpStatus.valueOf(ese.getStatus()),
                            ese.getCustomMessage(),
                            ese.getBody())
                    .flatMap(resp -> resp.writeTo(exchange, ResponseContext.DEFAULT));
        }

        if (ex instanceof SolicitudValidationException sve) {
            var validationMsgs = Stream.concat(
                    sve.getInfraErrors().stream().map(SolicitudErrorCode::getMessage),
                    sve.getDomainErrors().stream().map(SolicitudErrorCode::getMessage)
            );
            var externalMsgs = sve.getMicroAuth().stream();
            var errors = Stream.concat(validationMsgs, externalMsgs)
                    .distinct().toList();

            return apiResponseBuilder.build(
                            HttpStatus.BAD_REQUEST,
                            "Errores de validación",
                            errors)
                    .flatMap(resp -> resp.writeTo(exchange, ResponseContext.DEFAULT));
        }

        logger.error("Error no controlado", ex);
        return apiResponseBuilder.build(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Error interno del servidor",
                        List.of("Ocurrió un error inesperado"))
                .flatMap(resp -> resp.writeTo(exchange, ResponseContext.DEFAULT));
    }


    static final class ResponseContext implements ServerResponse.Context {
        static final ResponseContext DEFAULT = new ResponseContext();
        private final HandlerStrategies strategies = HandlerStrategies.withDefaults();

        @Override
        public List<HttpMessageWriter<?>> messageWriters() {
            return strategies.messageWriters();
        }

        @Override
        public List<ViewResolver> viewResolvers() {
            return strategies.viewResolvers();
        }
    }
}

