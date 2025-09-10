package co.com.crediya_solicitud.api;

import co.com.crediya_solicitud.api.dto.SolicitudDto;
import co.com.crediya_solicitud.api.dto.SolicitudResponseDto;
import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.api.mapper.SolicitudDtoMapper;
import co.com.crediya_solicitud.api.utils.ApiResponseBuilder;
import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import co.com.crediya_solicitud.usecase.solicitud.SolicitudService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SolicitudHandlerTest {

    private WebTestClient client;

    private ApiResponseBuilder apiResponseBuilder;
    private SolicitudService solicitudService;
    private SolicitudDtoMapper mapper;
    private Validator validator;
    private GlobalLogger logger;
    private UserGateway userGateway;

    @BeforeEach
    void setup() {
        apiResponseBuilder = mock(ApiResponseBuilder.class);
        solicitudService = mock(SolicitudService.class);
        mapper = mock(SolicitudDtoMapper.class);
        validator = mock(Validator.class, Answers.RETURNS_DEEP_STUBS);
        logger = mock(GlobalLogger.class);
        userGateway = mock(UserGateway.class);

        SolicitudHandler handler = new SolicitudHandler(
                apiResponseBuilder, solicitudService, mapper, validator, logger
        );

        RouterFunction<ServerResponse> routes = RouterFunctions.route()
                .POST("/api/v1/solicitud", handler::createSolicitud)
                .GET("/api/v1/solicitud", handler::findAll)
                .build();

        client = WebTestClient.bindToRouterFunction(routes).build();
    }

    @Test
    void createSolicitud_returns_400_when_dto_validation_fails() {
        SolicitudDto badDto = new SolicitudDto(
                "sol-X", null, " ", 12, "a@b.com", "estado-001", "loan-1"
        );

        ConstraintViolation<SolicitudDto> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("USR_007");
        when(validator.validate(any(SolicitudDto.class))).thenReturn(Set.of(violation));

        HandlerFilterFunction<ServerResponse, ServerResponse> errorFilter =
                (req, next) -> next.handle(req).onErrorResume(SolicitudValidationException.class, ex ->
                        apiResponseBuilder.build(HttpStatus.BAD_REQUEST, "Errores de validación", List.of("x")));

        RouterFunction<ServerResponse> routes = RouterFunctions.route()
                .POST("/api/v1/solicitud", new SolicitudHandler(apiResponseBuilder, solicitudService, mapper, validator, logger)::createSolicitud)
                .build()
                .filter(errorFilter);

        WebTestClient customClient = WebTestClient.bindToRouterFunction(routes).build();

        when(apiResponseBuilder.build(eq(HttpStatus.BAD_REQUEST), anyString(), anyList()))
                .thenReturn(ServerResponse.status(HttpStatus.BAD_REQUEST).build());

        customClient.post()
                .uri("/api/v1/solicitud")
                .bodyValue(badDto)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createSolicitud_returns_201_with_body_on_success() {

        SolicitudDto dto = new SolicitudDto(
                "sol-1", BigDecimal.TEN, "DOC-1", 12, "mail@test.com", "estado-001", "loan-1"
        );
        Solicitud domain = Solicitud.builder()
                .solicitud_id("sol-1").amount(BigDecimal.TEN).documentId("DOC-1")
                .term(12).email("mail@test.com").stateId("estado-001").loanTypeId("loan-1")
                .build();

        when(validator.validate(any(SolicitudDto.class))).thenReturn(Set.of());
        when(mapper.toSolicitud(any(SolicitudDto.class))).thenReturn(domain);
        when(solicitudService.createSolicitud(domain)).thenReturn(Mono.just(domain));
        when(mapper.toDto(domain)).thenReturn(dto);
        when(apiResponseBuilder.build(eq(HttpStatus.CREATED), anyString(), eq(dto)))
                .thenReturn(ServerResponse.status(HttpStatus.CREATED).bodyValue(dto));

        client.post()
                .uri("/api/v1/solicitud")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(SolicitudDto.class)
                .value(body -> {
                    assertThat(body.document_id()).isEqualTo("DOC-1");
                    assertThat(body.amount()).isEqualByComparingTo(BigDecimal.TEN);
                });

        verify(validator).validate(any(SolicitudDto.class));
        verify(mapper).toSolicitud(any(SolicitudDto.class));
        verify(solicitudService).createSolicitud(any(Solicitud.class));
        verify(mapper).toDto(any(Solicitud.class));
        verify(apiResponseBuilder).build(eq(HttpStatus.CREATED), anyString(), eq(dto));
    }

    @Test
    void findAll_returns_200_with_list() {
        Solicitud a = Solicitud.builder().solicitud_id("ABCD123").build();
        Solicitud b = Solicitud.builder().solicitud_id("BCDE987").build();

        SolicitudResponseDto srDto = new SolicitudResponseDto("ABCD123", null, 0, null, null, null);
        SolicitudResponseDto srDtoDos = new SolicitudResponseDto("BCDE987", null, 0, null, null, null);

        when(solicitudService.getAllSolicitud()).thenReturn(reactor.core.publisher.Flux.just(a, b));
        when(mapper.toSolicitud(a)).thenReturn(srDto);
        when(mapper.toSolicitud(b)).thenReturn(srDtoDos);
        when(apiResponseBuilder.build(
                eq(HttpStatus.OK),
                anyString(),
                eq(List.of(srDto, srDtoDos))
        )).thenReturn(ServerResponse.ok().bodyValue(List.of(srDto, srDtoDos)));

        client.get()
                .uri("/api/v1/solicitud")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(SolicitudResponseDto.class)
                .contains(srDto, srDtoDos);

        verify(solicitudService).getAllSolicitud();
        verify(mapper).toSolicitud(a);
        verify(mapper).toSolicitud(b);
        verify(apiResponseBuilder).build(
                eq(HttpStatus.OK),
                anyString(),
                eq(List.of(srDto, srDtoDos))
        );
    }
}