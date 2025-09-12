package co.com.crediya_solicitud.api.exception;


import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.api.utils.ApiResponseBuilder;
import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class GlobalErrorHandlerTest {

    private GlobalErrorHandler globalErrorHandler;
    private ApiResponseBuilder apiResponseBuilder;
    private GlobalLogger logger;

    @BeforeEach
    void setUp() {
         apiResponseBuilder = Mockito.mock(ApiResponseBuilder.class);
        logger = Mockito.mock(GlobalLogger.class);
        globalErrorHandler = new GlobalErrorHandler(apiResponseBuilder, logger);

        when(apiResponseBuilder.build(
                Mockito.eq(HttpStatus.BAD_REQUEST),
                Mockito.anyString(),
                Mockito.anyList()
        )).thenAnswer(inv -> ServerResponse.status(HttpStatus.BAD_REQUEST).build());

        when(apiResponseBuilder.build(
                Mockito.eq(HttpStatus.INTERNAL_SERVER_ERROR),
                Mockito.anyString(),
                Mockito.anyList()
        )).thenAnswer(inv -> ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build());
    }

    @Test
    void shouldHandleSolicitudValidationException() {
        SolicitudValidationException ex = new SolicitudValidationException(
                List.of(SolicitudErrorCode.AUTH),
                List.of(SolicitudErrorCode.AMOUNT_INVALID),
                List.of("micro-auth-error")
        );

        var filter = globalErrorHandler.filter();

        Mono<ServerResponse> responseMono = filter.filter(null, request -> Mono.error(ex));

        StepVerifier.create(responseMono)
                .assertNext(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST))
                .verifyComplete();
    }

    @Test
    void shouldHandleGenericException() {
        RuntimeException ex = new RuntimeException("Boom!");

        var filter = globalErrorHandler.filter();

        Mono<ServerResponse> responseMono = filter.filter(null, request -> Mono.error(ex));

        StepVerifier.create(responseMono)
                .assertNext(response -> assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR))
                .verifyComplete();
    }
}