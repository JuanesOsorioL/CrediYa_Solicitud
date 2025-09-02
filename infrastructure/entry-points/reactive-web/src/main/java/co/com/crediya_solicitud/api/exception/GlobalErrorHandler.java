package co.com.crediya_solicitud.api.exception;


import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.api.utils.ApiResponseBuilder;
import co.com.crediya_solicitud.usecase.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.List;
import java.util.stream.Stream;


@Component
public class GlobalErrorHandler {

    private final ApiResponseBuilder apiResponseBuilder;
    private final GlobalLogger logger;

    public GlobalErrorHandler(ApiResponseBuilder apiResponseBuilder, GlobalLogger logger) {
        this.apiResponseBuilder = apiResponseBuilder;
        this.logger = logger;
    }

    public HandlerFilterFunction<ServerResponse, ServerResponse> filter() {
        return (request, next) -> next.handle(request)
                .onErrorResume(SolicitudValidationException.class, ex -> {
                    logger.warn("Errores de UserValidationException");

                    Stream<String> validationStream = Stream.concat(
                            ex.getInfraErrors().stream().map(SolicitudErrorCode::getMessage),
                            ex.getDomainErrors().stream().map(SolicitudErrorCode::getMessage)
                    );

                    Stream<String> externalStream = ex.getMicroAuth().stream();
                    List<String> errors = Stream.concat(validationStream, externalStream)
                            .distinct()
                            .toList();

                    return apiResponseBuilder.build(
                            HttpStatus.BAD_REQUEST,
                            "Errores de validación",
                            errors
                    );
                })
                .onErrorResume(Exception.class, ex -> {
                    logger.error("Errores de validación", ex);
                    return apiResponseBuilder.build(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "Error interno del servidor",
                            List.of("Ocurrió un error inesperado")
                    );
                }).doFinally(signal -> logger.info("Flujo finalizado!!"));
    }
}
