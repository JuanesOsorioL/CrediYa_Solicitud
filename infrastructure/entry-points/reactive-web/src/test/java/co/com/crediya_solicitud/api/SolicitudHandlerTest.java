package co.com.crediya_solicitud.api;

import co.com.crediya_solicitud.api.dto.ClaismoDto;
import co.com.crediya_solicitud.api.dto.DecisionDto;
import co.com.crediya_solicitud.api.dto.SolicitudDto;
import co.com.crediya_solicitud.api.dto.SolicitudRevisionDto;
import co.com.crediya_solicitud.api.logger.GlobalLogger;
import co.com.crediya_solicitud.api.mapper.SolicitudDtoMapper;
import co.com.crediya_solicitud.api.utils.ApiResponseBuilder;
import co.com.crediya_solicitud.api.utils.ValidateResponseToken;
import co.com.crediya_solicitud.model.claims.Claismo;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.exception.specificexceptions.UnauthorizedException;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.solicitud_revision.SolicitudRevision;
import co.com.crediya_solicitud.model.sqs.Decision;
import co.com.crediya_solicitud.model.sqs.SqsSendGateway;
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
    private SqsSendGateway sqsSendGateway;

    private static final String AUTH = "Bearer token-prueba";
    private HandlerFilterFunction<ServerResponse, ServerResponse> unauthorizedFilter;
    private HandlerFilterFunction<ServerResponse, ServerResponse> solicitudValidationToBadRequestFilter;

    @BeforeEach
    void setup() {
        apiResponseBuilder = mock(ApiResponseBuilder.class);
        solicitudService = mock(SolicitudService.class);
        mapper = mock(SolicitudDtoMapper.class);
        validator = mock(Validator.class, Answers.RETURNS_DEEP_STUBS);
        logger = mock(GlobalLogger.class);
        userGateway = mock(UserGateway.class);
        validateResponseToken = mock(ValidateResponseToken.class);
        sqsSendGateway = mock(SqsSendGateway.class);

        unauthorizedFilter =
                (req, next) -> next.handle(req)
                        .onErrorResume(UnauthorizedException.class, ex -> ServerResponse.status(HttpStatus.UNAUTHORIZED).build());

        solicitudValidationToBadRequestFilter =
                (req, next) -> next.handle(req)
                        .onErrorResume(SolicitudValidationException.class,
                                e -> apiResponseBuilder.build(HttpStatus.BAD_REQUEST, "Errores de validación", List.of("VALIDATION")) // la lista es simbólica
                        );

        var handler = new SolicitudHandler(
                apiResponseBuilder, solicitudService, mapper, validator, logger, userGateway, validateResponseToken, sqsSendGateway);

        RouterFunction<ServerResponse> routes = RouterFunctions.route()
                .POST("/api/v1/solicitud", handler::createSolicitud)
                .GET("/api/v1/solicitud", handler::findAll)
                .PUT("/api/v1/solicitud", handler::updateSolicitud)
                .build()
                .filter(unauthorizedFilter);

        client = WebTestClient.bindToRouterFunction(routes).build();

        when(apiResponseBuilder.build(eq(HttpStatus.UNAUTHORIZED), anyString(), isNull()))
                .thenReturn(ServerResponse.status(HttpStatus.UNAUTHORIZED).build());
        when(apiResponseBuilder.build(eq(HttpStatus.BAD_REQUEST), anyString(), any()))
                .thenReturn(ServerResponse.badRequest().build());
        when(apiResponseBuilder.build(eq(HttpStatus.OK), anyString(), any()))
                .thenReturn(ServerResponse.ok().build());
        when(apiResponseBuilder.build(eq(HttpStatus.CREATED), anyString(), any()))
                .thenReturn(ServerResponse.status(HttpStatus.CREATED).build());
    }

    @Test
    void createSolicitud_returns_201_on_success() {
        var claims = new Claismo("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        var claimsDto = new ClaismoDto("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        when(userGateway.validateTokenAndGetClaims()).thenReturn(Mono.just(claims));
        when(mapper.toClaismoDto(claims)).thenReturn(claimsDto);
        when(validateResponseToken.isCustomer(claimsDto)).thenReturn(Mono.just(claimsDto));
        when(validateResponseToken.isOwner(eq(claimsDto), anyString())).thenReturn(Mono.just(claimsDto));

        var dtoIn = new SolicitudDto("sol-1", BigDecimal.TEN, "DOC-1", 12, "mail@test.com", "estado-001", "loan-1");
        var inSolicitud = new Solicitud("sol-1", BigDecimal.TEN, 12, "mail@test.com", "estado-001", "loan-1", "DOC-1");


        when(validator.validate(any(SolicitudDto.class))).thenReturn(Set.of());

        when(mapper.toSolicitud(any(SolicitudDto.class))).thenReturn(inSolicitud);

        when(solicitudService.createSolicitud(inSolicitud)).thenReturn(Mono.just(inSolicitud));

        var dtoOut = new Object();
        when(mapper.toDto(inSolicitud)).thenReturn(dtoIn);

        when(apiResponseBuilder.build(eq(HttpStatus.CREATED), contains("exitosamente"), same(dtoOut)))
                .thenReturn(ServerResponse.status(HttpStatus.CREATED).build());

        client.post()
                .uri("/api/v1/solicitud")
                .header("Authorization", AUTH)
                .bodyValue(dtoIn)
                .exchange()
                .expectStatus().isCreated();

        verify(logger, atLeastOnce()).info(contains("inicia el flujo."));
    }


    @Test
    void create_Solicitud_returns_400_when_dto_validation_fails() {
        SolicitudDto badDto = new SolicitudDto(
                "sol-X", null, " ", 12, "a@b.com", "estado-001", "loan-1"
        );

        ClaismoDto claismoDto = new ClaismoDto(
                "juan", "a@b.com", "Admin", "fecha", "esteban", "1111111", "fecha", "aaaa"
        );

        Claismo claims = new Claismo("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");


        ConstraintViolation<SolicitudDto> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("USR_007");
        when(validator.validate(any(SolicitudDto.class))).thenReturn(Set.of(violation));

        when(userGateway.validateTokenAndGetClaims())
                .thenReturn(Mono.just(claims));
        when(mapper.toClaismoDto(claims)).thenReturn(claismoDto);
        when(validateResponseToken.isCustomer(any()))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
        when(validateResponseToken.isOwner(any(), anyString()))
                .thenReturn(Mono.just(claismoDto));

        RouterFunction<ServerResponse> routes = RouterFunctions.route()
                .POST("/api/v1/solicitud",
                        new SolicitudHandler(apiResponseBuilder, solicitudService, mapper, validator, logger, userGateway, validateResponseToken, sqsSendGateway)::createSolicitud)
                .build()
                .filter(unauthorizedFilter)
                .filter(solicitudValidationToBadRequestFilter);

        WebTestClient customClient = WebTestClient.bindToRouterFunction(routes).build();

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

        SolicitudDto dto = new SolicitudDto("sol-1", BigDecimal.TEN, "DOC-1", 12, "mail@test.com", "estado-001", "loan-1");

        client.post()
                .uri("/api/v1/solicitud")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isUnauthorized();
    }


    @Test
    void findAll_returns_200_with_page_content() {

        ClaismoDto claimsDto = new ClaismoDto("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        Claismo claims = new Claismo("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        when(userGateway.validateTokenAndGetClaims()).thenReturn(Mono.just(claims));
        when(mapper.toClaismoDto(claims)).thenReturn(claimsDto);
        when(validateResponseToken.isAdviser(any())).thenReturn(Mono.just(claimsDto));

        SolicitudRevision solicitudRevision1 = new SolicitudRevision(BigDecimal.TEN, 3, "sol-1", "juan", "DOC-1", 12, "mail@test.com", BigDecimal.TEN, BigDecimal.TEN);
        SolicitudRevision solicitudRevision2 = new SolicitudRevision(BigDecimal.ONE, 6, "sol-2", "maria", "DOC-2", 10, "maria@test.com", BigDecimal.ONE, BigDecimal.ZERO);

        SolicitudRevisionDto solicitudRevisionDto1 = new SolicitudRevisionDto(BigDecimal.TEN, 3, "sol-1", "juan", "DOC-1", 12, "mail@test.com", BigDecimal.TEN, BigDecimal.TEN);
        SolicitudRevisionDto solicitudRevisionDto2 = new SolicitudRevisionDto(BigDecimal.ONE, 6, "sol-2", "maria", "DOC-2", 10, "maria@test.com", BigDecimal.ONE, BigDecimal.ZERO);

        when(solicitudService.countByStatus(anyList())).thenReturn(Mono.just(2L));
        when(solicitudService.getSolicitudByRevision(anyList(), eq(0), eq(10)))
                .thenReturn(Flux.just(solicitudRevision1, solicitudRevision2));
        when(mapper.toSolicitudRevision(solicitudRevision1)).thenReturn(solicitudRevisionDto1);
        when(mapper.toSolicitudRevision(solicitudRevision2)).thenReturn(solicitudRevisionDto2);

        var expectedPage = new PageImpl<>(List.of(solicitudRevisionDto1, solicitudRevisionDto2), PageRequest.of(0, 10), 2);
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
        ClaismoDto claimsDto = new ClaismoDto("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        Claismo claims = new Claismo("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        when(userGateway.validateTokenAndGetClaims()).thenReturn(Mono.just(claims));
        when(mapper.toClaismoDto(any(Claismo.class))).thenReturn(claimsDto);
        when(validateResponseToken.isAdviser(any())).thenReturn(Mono.just(claimsDto));

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
        ClaismoDto claimsDto = new ClaismoDto("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        Claismo claims = new Claismo("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        when(userGateway.validateTokenAndGetClaims()).thenReturn(Mono.just(claims));
        when(mapper.toClaismoDto(claims)).thenReturn(claimsDto);
        when(validateResponseToken.isAdviser(any())).thenReturn(Mono.just(claimsDto));

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

        ClaismoDto claimsDto = new ClaismoDto("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        Claismo claims = new Claismo("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        when(userGateway.validateTokenAndGetClaims()).thenReturn(Mono.just(claims));
        when(mapper.toClaismoDto(claims)).thenReturn(claimsDto);
        when(validateResponseToken.isAdviser(any())).thenReturn(Mono.just(claimsDto));

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

        RouterFunction<ServerResponse> routes = RouterFunctions.route()
                .GET("/api/v1/solicitud",
                        new SolicitudHandler(apiResponseBuilder, solicitudService, mapper, validator, logger, userGateway, validateResponseToken, sqsSendGateway)::findAll)
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
        when(userGateway.validateTokenAndGetClaims()).thenReturn(Mono.error(ex));

        doReturn(Mono.just(0L)).when(solicitudService).countByStatus(anyList());
        doReturn(Flux.empty()).when(solicitudService)
                .getSolicitudByRevision(anyList(), anyInt(), anyInt());

        RouterFunction<ServerResponse> routes = RouterFunctions.route()
                .GET("/api/v1/solicitud",
                        new SolicitudHandler(apiResponseBuilder, solicitudService, mapper, validator, logger, userGateway, validateResponseToken, sqsSendGateway)::findAll)
                .build()
                .filter(unauthorizedFilter)
                .filter(solicitudValidationToBadRequestFilter);

        WebTestClient customClient = WebTestClient.bindToRouterFunction(routes).build();

        when(apiResponseBuilder.build(eq(HttpStatus.BAD_REQUEST), anyString(), anyList()))
                .thenReturn(ServerResponse.badRequest().build());

        customClient.get()
                .uri("/api/v1/solicitud?page=0&size=10&status=estado-001")
                .header("Authorization", "Bearer token-prueba")
                .exchange()
                .expectStatus().isBadRequest();
    }


    @Test
    void updateSolicitud_returns_200_on_success() {
        var claims = new Claismo("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        var claimsDto = new ClaismoDto("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        when(userGateway.validateTokenAndGetClaims()).thenReturn(Mono.just(claims));
        when(mapper.toClaismoDto(claims)).thenReturn(claimsDto);
        when(validateResponseToken.isAdviser(claimsDto)).thenReturn(Mono.just(claimsDto));

        var dto = new DecisionDto("10", "01", "rechazado", "nn", "adv@test.com", "", 0);
        when(validator.validate(any(DecisionDto.class))).thenReturn(Set.of());

        var decisionDom = new Decision("10", "01", "rechazado", "nn", "adv@test.com", "", 0);
        when(mapper.toDecision(dto)).thenReturn(decisionDom);

        Map<String, Object> payloadEnviado = Map.of("sent", true);

        when(solicitudService.validateUpdateSolicitud(decisionDom)).thenReturn(Mono.just(decisionDom));
        when(sqsSendGateway.notificarCambio(decisionDom)).thenReturn(Mono.just("decisionDom"));

        when(apiResponseBuilder.build(eq(HttpStatus.OK), contains("exitosamente"), same(payloadEnviado)))
                .thenReturn(ServerResponse.ok().build());

        client.put()
                .uri("/api/v1/solicitud")
                .header("Authorization", AUTH)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk();

        verify(solicitudService).validateUpdateSolicitud(decisionDom);
        verify(sqsSendGateway).notificarCambio(decisionDom);
    }

    @Test
    void updateSolicitud_returns_400_when_validation_fails() {
        var claims = new Claismo("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        var claimsDto = new ClaismoDto("asesor", "adv@test.com", "Adviser", "f", "n", "doc", "f", "id");
        when(userGateway.validateTokenAndGetClaims()).thenReturn(Mono.just(claims));
        when(mapper.toClaismoDto(claims)).thenReturn(claimsDto);
        when(validateResponseToken.isAdviser(claimsDto)).thenReturn(Mono.just(claimsDto));

        @SuppressWarnings("unchecked")
        ConstraintViolation<DecisionDto> violation = mock(ConstraintViolation.class);
        when(violation.getMessage()).thenReturn("USR_007"); // asegúrate que exista en tu enum
        when(validator.validate(any(DecisionDto.class))).thenReturn(Set.of(violation));

        var handler = new SolicitudHandler(apiResponseBuilder, solicitudService, mapper, validator, logger, userGateway, validateResponseToken, sqsSendGateway);
        var routes = RouterFunctions.route()
                .PUT("/api/v1/solicitud", handler::updateSolicitud)
                .build()
                .filter(unauthorizedFilter)
                .filter(solicitudValidationToBadRequestFilter);

        var customClient = WebTestClient.bindToRouterFunction(routes).build();

        when(apiResponseBuilder.build(eq(HttpStatus.BAD_REQUEST), anyString(), anyList()))
                .thenReturn(ServerResponse.badRequest().build());

        var dto = new DecisionDto("10", "01", "rechazado", "nn", "adv@test.com", "", 0);

        customClient.put()
                .uri("/api/v1/solicitud")
                .header("Authorization", AUTH)
                .bodyValue(dto)
                .exchange()
                .expectStatus().isBadRequest();

        verifyNoInteractions(solicitudService, sqsSendGateway);
    }

    @Test
    void updateSolicitud_returns_401_when_missing_token() {
        var handler = new SolicitudHandler(apiResponseBuilder, solicitudService, mapper, validator, logger, userGateway, validateResponseToken, sqsSendGateway);
        var routes = RouterFunctions.route()
                .PUT("/api/v1/solicitud", handler::updateSolicitud)
                .build()
                .filter(unauthorizedFilter);

        var customClient = WebTestClient.bindToRouterFunction(routes).build();

        var dto = new DecisionDto("10", "01", "rechazado", "nn", "adv@test.com", "", 0);

        customClient.put()
                .uri("/api/v1/solicitud")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}