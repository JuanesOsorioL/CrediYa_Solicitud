package co.com.crediya_solicitud.api;

import co.com.crediya_solicitud.api.dto.SolicitudDto;
import co.com.crediya_solicitud.api.dto.SolicitudResponseDto;
import co.com.crediya_solicitud.api.dto.SolicitudRevisionDto;
import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.api.mapper.SolicitudDtoMapper;
import co.com.crediya_solicitud.api.utils.ApiResponseBuilder;
import co.com.crediya_solicitud.api.utils.ValidateResponseToken;
import co.com.crediya_solicitud.model.claims.ClaismoDto;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.solicitud_revision.SolicitudRevision;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import co.com.crediya_solicitud.usecase.solicitud.SolicitudService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
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
    private ValidateResponseToken validateResponseToken;

    private static final String AUTH = "Bearer token-prueba";

    @BeforeEach
    void setup() {
        apiResponseBuilder = mock(ApiResponseBuilder.class);
        solicitudService = mock(SolicitudService.class);
        mapper = mock(SolicitudDtoMapper.class);
        validator = mock(Validator.class, Answers.RETURNS_DEEP_STUBS);
        logger = mock(GlobalLogger.class);
        userGateway = mock(UserGateway.class);
        validateResponseToken = mock(ValidateResponseToken.class);


        SolicitudHandler handler = new SolicitudHandler(
                apiResponseBuilder, solicitudService, mapper, validator, logger, userGateway, validateResponseToken);

        RouterFunction<ServerResponse> routes = RouterFunctions.route()
                .POST("/api/v1/solicitud", handler::createSolicitud)
                .GET("/api/v1/solicitud", handler::findAll)
                .build();

        client = WebTestClient.bindToRouterFunction(routes).build();
    }

    @Test
    void create_Solicitud_returns_400_when_dto_validation_fails() {
        SolicitudDto badDto = new SolicitudDto(
                "sol-X", null, " ", 12, "a@b.com", "estado-001", "loan-1"
        );

        ClaismoDto claismoDto = new ClaismoDto(
                "juan", "a@b.com", "Admin", "fecha", "esteban", "1111111", "fecha", "aaaa"
        );

        ConstraintViolation<SolicitudDto> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("USR_007");
        when(validator.validate(any(SolicitudDto.class))).thenReturn(Set.of(violation));

        when(userGateway.validateTokenAndGetClaims(anyString()))
                .thenReturn(Mono.just(claismoDto));
        when(validateResponseToken.isCustomer(any()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(validateResponseToken.isOwner(any(), anyString()))
                .thenReturn(Mono.just(claismoDto));

        HandlerFilterFunction<ServerResponse, ServerResponse> errorFilter =
                (req, next) -> next.handle(req).onErrorResume(SolicitudValidationException.class, ex ->
                        apiResponseBuilder.build(HttpStatus.BAD_REQUEST, "Errores de validación", List.of("x")));

        RouterFunction<ServerResponse> routes = RouterFunctions.route()
                .POST("/api/v1/solicitud",
                        new SolicitudHandler(apiResponseBuilder, solicitudService, mapper, validator, logger, userGateway, validateResponseToken)::createSolicitud)
                .build()
                .filter(errorFilter);

        WebTestClient customClient = WebTestClient.bindToRouterFunction(routes).build();

        when(apiResponseBuilder.build(eq(HttpStatus.UNAUTHORIZED), anyString(), isNull()))
                .thenReturn(ServerResponse.status(HttpStatus.UNAUTHORIZED).build());

        when(apiResponseBuilder.build(eq(HttpStatus.BAD_REQUEST), anyString(), anyList()))
                .thenReturn(ServerResponse.status(HttpStatus.BAD_REQUEST).build());

        customClient.post()
                .uri("/api/v1/solicitud")
                .header("Authorization", AUTH)
                .bodyValue(badDto)
                .exchange()
                .expectStatus().isBadRequest();
    }


    @Test
    void createSolicitud_returns_401_when_no_token() {
        when(apiResponseBuilder.build(eq(HttpStatus.UNAUTHORIZED), anyString(), isNull()))
                .thenReturn(ServerResponse.status(HttpStatus.UNAUTHORIZED).build());

        SolicitudDto dto = new SolicitudDto("sol-1", BigDecimal.TEN, "DOC-1", 12, "mail@test.com", "estado-001", "loan-1");

        client.post()
                .uri("/api/v1/solicitud")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isUnauthorized();
    }


    @Test
    void findAll_returns_200_with_page_content() {

        ClaismoDto claims = new ClaismoDto("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        when(userGateway.validateTokenAndGetClaims(anyString())).thenReturn(Mono.just(claims));
        when(validateResponseToken.isAdviser(any())).thenReturn(Mono.just(claims));

        SolicitudRevision solicitudRevision1 = new SolicitudRevision(BigDecimal.TEN, 3, "sol-1", "juan", "DOC-1", 12, "mail@test.com", BigDecimal.TEN, BigDecimal.TEN);
        SolicitudRevision solicitudRevision2 = new SolicitudRevision(BigDecimal.ONE, 6, "sol-2", "maria", "DOC-2", 10, "maria@test.com", BigDecimal.ONE, BigDecimal.ZERO);

        SolicitudRevisionDto solicitudRevisionDto1 = new SolicitudRevisionDto(BigDecimal.TEN, 3, "sol-1", "juan", "DOC-1", 12, "mail@test.com", BigDecimal.TEN, BigDecimal.TEN);
        SolicitudRevisionDto solicitudRevisionDto2 = new SolicitudRevisionDto(BigDecimal.ONE, 6, "sol-2", "maria", "DOC-2", 10, "maria@test.com", BigDecimal.ONE, BigDecimal.ZERO);

        var srA = new SolicitudResponseDto("ABCD123", null, 0, null, null, null);
        var srB = new SolicitudResponseDto("BCDE987", null, 0, null, null, null);

        when(solicitudService.countByStatus(anyList())).thenReturn(Mono.just(2L));
        when(solicitudService.getSolicitudByRevision(anyList(), eq(0), eq(10))).thenReturn(Flux.just(solicitudRevision1, solicitudRevision2));
        when(mapper.toSolicitudRevision(solicitudRevision1)).thenReturn(solicitudRevisionDto1);
        when(mapper.toSolicitudRevision(solicitudRevision2)).thenReturn(solicitudRevisionDto2);

        var expectedPage = new PageImpl<>(List.of(srA, srB), PageRequest.of(0, 10), 2);
        when(apiResponseBuilder.build(eq(HttpStatus.OK), anyString(), any(PageImpl.class)))
                .thenReturn(ServerResponse.ok().bodyValue(expectedPage));

        client.get()
                .uri("/api/v1/solicitud?page=0&size=10&status=estado-001")
                .header("Authorization", AUTH)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.totalElements").isEqualTo(2);

        verify(solicitudService).countByStatus(anyList());
        verify(solicitudService).getSolicitudByRevision(anyList(), eq(0), eq(10));
        verify(mapper).toSolicitudRevision(same(solicitudRevision1));
        verify(mapper).toSolicitudRevision(same(solicitudRevision2));
    }


    @Test
    void findAll_returns_200_when_no_results() {
        ClaismoDto claims = new ClaismoDto("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        when(userGateway.validateTokenAndGetClaims(anyString())).thenReturn(Mono.just(claims));
        when(validateResponseToken.isAdviser(any())).thenReturn(Mono.just(claims));

        when(solicitudService.countByStatus(anyList())).thenReturn(Mono.just(0L));
        when(apiResponseBuilder.build(eq(HttpStatus.OK), contains("No hay resultados"), any()))
                .thenReturn(ServerResponse.ok().build());

        client.get()
                .uri("/api/v1/solicitud")
                .header("Authorization", AUTH)
                .exchange()
                .expectStatus().isOk();

        ArgumentCaptor<List<String>> statusesCap = ArgumentCaptor.forClass(List.class);
        verify(solicitudService).countByStatus(statusesCap.capture());
        assertThat(statusesCap.getValue()).containsExactly("estado-001");
    }

    @Test
    void findAll_returns_400_when_page_out_of_range() {
        ClaismoDto claims = new ClaismoDto("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        when(userGateway.validateTokenAndGetClaims(anyString())).thenReturn(Mono.just(claims));
        when(validateResponseToken.isAdviser(any())).thenReturn(Mono.just(claims));

        when(solicitudService.countByStatus(anyList())).thenReturn(Mono.just(5L));
        when(apiResponseBuilder.build(eq(HttpStatus.BAD_REQUEST), contains("fuera de rango"), any()))
                .thenReturn(ServerResponse.badRequest().build());

        client.get()
                .uri("/api/v1/solicitud?page=2&size=10&status=estado-001")
                .header("Authorization", AUTH)
                .exchange()
                .expectStatus().isBadRequest();

        verify(solicitudService, never()).getSolicitudByRevision(anyList(), anyInt(), anyInt());
    }


    @Test
    void findAll_normalizes_page_and_size_and_calls_revision_with_defaults() {

        ClaismoDto claims = new ClaismoDto("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        when(userGateway.validateTokenAndGetClaims(anyString())).thenReturn(Mono.just(claims));
        when(validateResponseToken.isAdviser(any())).thenReturn(Mono.just(claims));

        when(solicitudService.countByStatus(anyList())).thenReturn(Mono.just(1L));
        when(solicitudService.getSolicitudByRevision(anyList(), anyInt(), anyInt()))
                .thenReturn(Flux.empty());
        when(apiResponseBuilder.build(eq(HttpStatus.OK), anyString(), any(PageImpl.class)))
                .thenReturn(ServerResponse.ok().build());

        client.get()
                .uri("/api/v1/solicitud?page=-5&size=0")
                .header("Authorization", AUTH)
                .exchange()
                .expectStatus().isOk();

        ArgumentCaptor<Integer> pageCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<List<String>> statusesCap = ArgumentCaptor.forClass(List.class);

        verify(solicitudService).getSolicitudByRevision(statusesCap.capture(), pageCap.capture(), sizeCap.capture());
        assertThat(pageCap.getValue()).isEqualTo(0);
        assertThat(sizeCap.getValue()).isEqualTo(1);
        assertThat(statusesCap.getValue()).containsExactly("estado-001");
    }


    @Test
    void findAll_returns_401_when_missing_token() {

        HandlerFilterFunction<ServerResponse, ServerResponse> unauthorizedFilter =
                (req, next) -> Mono.defer(() -> next.handle(req))
                        .onErrorResume(
                                co.com.crediya_solicitud.model.exception.specificexceptions.UnauthorizedException.class,
                                ex -> ServerResponse.status(HttpStatus.UNAUTHORIZED).build()
                        );

        RouterFunction<ServerResponse> routes = RouterFunctions.route()
                .GET("/api/v1/solicitud",
                        new SolicitudHandler(apiResponseBuilder, solicitudService, mapper, validator, logger, userGateway, validateResponseToken)::findAll)
                .build()
                .filter(unauthorizedFilter);

        WebTestClient customClient = WebTestClient.bindToRouterFunction(routes).build();

        customClient.get()
                .uri("/api/v1/solicitud")
                .exchange()
                .expectStatus().isUnauthorized();
    }


    @Test
    void findAll_maps_external_service_exception_to_400() {
        ExternalServiceException ex = mock(ExternalServiceException.class);
        doReturn(List.of(Map.of("error", "downstream"))).when(ex).getBody();
        when(userGateway.validateTokenAndGetClaims(anyString())).thenReturn(Mono.error(ex));

        doReturn(Mono.just(0L)).when(solicitudService).countByStatus(anyList());
        doReturn(Flux.empty()).when(solicitudService)
                .getSolicitudByRevision(anyList(), anyInt(), anyInt());

        HandlerFilterFunction<ServerResponse, ServerResponse> errorFilter =
                (req, next) -> next.handle(req).onErrorResume(SolicitudValidationException.class, e ->
                        apiResponseBuilder.build(HttpStatus.BAD_REQUEST, "Auth error", List.of("AUTH")));

        RouterFunction<ServerResponse> routes = RouterFunctions.route()
                .GET("/api/v1/solicitud",
                        new SolicitudHandler(apiResponseBuilder, solicitudService, mapper, validator, logger, userGateway, validateResponseToken)::findAll)
                .build()
                .filter(errorFilter);

        WebTestClient customClient = WebTestClient.bindToRouterFunction(routes).build();

        when(apiResponseBuilder.build(eq(HttpStatus.BAD_REQUEST), anyString(), anyList()))
                .thenReturn(ServerResponse.badRequest().build());

        customClient.get()
                .uri("/api/v1/solicitud?page=0&size=10&status=estado-001")
                .header("Authorization", "Bearer token-prueba")
                .exchange()
                .expectStatus().isBadRequest();
    }
}