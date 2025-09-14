package co.com.crediya_solicitud.api.exception;

import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.api.utils.ApiResponseBuilder;
import co.com.crediya_solicitud.model.exception.DomainException;
import co.com.crediya_solicitud.model.exception.ErrorKind;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GlobalWebExceptionHandlerTest {

    private ApiResponseBuilder builder;
    private GlobalLogger logger;
    private DomainHttpStatusMapper mapper;
    private GlobalWebExceptionHandler handler;

    @BeforeEach
    void setUp() {
        builder = mock(ApiResponseBuilder.class);
        logger = mock(GlobalLogger.class);
        mapper = mock(DomainHttpStatusMapper.class);

     when(builder.build(any(HttpStatus.class), anyString(), any()))
                .thenAnswer(inv -> ServerResponse
                        .status(inv.getArgument(0))
                        .bodyValue(inv.getArgument(2)));

        handler = new GlobalWebExceptionHandler(builder, logger, mapper);
    }

    private MockServerWebExchange newExchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
    }

  static class DummyDomainException extends DomainException {
        DummyDomainException(ErrorKind kind, String code, String message, java.util.List<SolicitudErrorCode> errors) {
            super(kind, code, message, errors, null);
        }
    }

    @Test
    void handle_maps_DomainException_to_status_and_body() {
        var ex = new DummyDomainException(
                ErrorKind.BAD_REQUEST,
                "USR_XYZ",
                "Error de dominio",
                List.of(SolicitudErrorCode.AUTH, SolicitudErrorCode.TOKEN_INVALID)
        );

        when(mapper.toHttpStatus(ErrorKind.BAD_REQUEST)).thenReturn(HttpStatus.BAD_REQUEST);

        var exchange = newExchange();
        handler.handle(exchange, ex).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        @SuppressWarnings("unchecked")
        var bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(builder).build(eq(HttpStatus.BAD_REQUEST), eq("Error de dominio"), bodyCaptor.capture());

        Map<String, Object> body = bodyCaptor.getValue();
        assertThat(body.get("kind")).isEqualTo(ErrorKind.BAD_REQUEST.name());
        assertThat(body.get("code")).isEqualTo("USR_XYZ");
        assertThat(body.get("errors")).isInstanceOf(List.class);
        assertThat((List<?>) body.get("errors")).hasSize(2);

      verify(logger, atLeastOnce()).warn(startsWith("DomainException BAD_REQUEST"));
        verify(logger, never()).error(eq("DomainException TECHNICAL"), any());
    }

    @Test
    void handle_logs_error_and_returns_5xx_when_TECHNICAL() {
        var ex = new DummyDomainException(
                ErrorKind.TECHNICAL, "TECH_001", "Falla técnica", List.of()
        );
        when(mapper.toHttpStatus(ErrorKind.TECHNICAL)).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        var exchange = newExchange();
        handler.handle(exchange, ex).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(logger).error(eq("DomainException TECHNICAL"), eq(ex));
    }

    @Test
    void handle_maps_ExternalServiceException() {
        ExternalServiceException ese = mock(ExternalServiceException.class);
        when(ese.getStatus()).thenReturn(409);
        when(ese.getCustomMessage()).thenReturn("Auth error");
       doReturn(List.of("AUTH")).when(ese).getBody();

        var exchange = newExchange();
        handler.handle(exchange, ese).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        verify(builder).build(eq(HttpStatus.CONFLICT), eq("Auth error"), eq(List.of("AUTH")));
    }

    @Test
    void handle_maps_SolicitudValidationException_to_400() {
        var infra = List.of(SolicitudErrorCode.AUTH);
        var domain = List.of(SolicitudErrorCode.TOKEN_INVALID);
        var micro = List.of("AUTH", "AUTH");

        var sve = new SolicitudValidationException(infra, domain, micro);
        var exchange = newExchange();

        handler.handle(exchange, sve).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(builder).build(eq(HttpStatus.BAD_REQUEST), eq("Errores de validación"), any());
    }

    @Test
    void handle_unexpected_exception_returns_500() {
        var boom = new RuntimeException("boom");
        var exchange = newExchange();

        handler.handle(exchange, boom).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(builder).build(eq(HttpStatus.INTERNAL_SERVER_ERROR), anyString(), any());
    }
}