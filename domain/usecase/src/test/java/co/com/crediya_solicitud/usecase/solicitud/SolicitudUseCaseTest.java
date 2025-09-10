package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.error.SolicitudErrorCode;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import co.com.crediya_solicitud.usecase.loantypes.LoanTypesUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SolicitudUseCaseTest {

    private SolicitudRepository solicitudRepository;
    private LoanTypesUseCase loanTypesUseCase;
    private Logger logger;
    private UserGateway userGateway;

    private SolicitudUseCase useCase;

    @BeforeEach
    void setUp() {
        solicitudRepository = mock(SolicitudRepository.class);
        loanTypesUseCase = mock(LoanTypesUseCase.class);
        logger = mock(Logger.class);
        userGateway = mock(UserGateway.class);
        useCase = new SolicitudUseCase(solicitudRepository, loanTypesUseCase, logger, userGateway);
    }

    @Test
    void createSolicitud_success_flow_persists_and_returns_saved_with_original_document() {
        Solicitud input = Solicitud.builder()
                .solicitud_id(null)
                .documentId("DOC-1")
                .amount(BigDecimal.valueOf(20000))
                .loanTypeId("LT-1")
                .email(null)
                .term(12)
                .stateId(null)
                .build();

        when(userGateway.getUserEmailByDocument("DOC-1")).thenReturn(Mono.just("user@mail.com"));

        when(loanTypesUseCase.findByLoanTypeAndValidateAmount("LT-1", BigDecimal.valueOf(20000)))
                .thenReturn(Mono.just(mock(LoanTypes.class))).thenReturn(Mono.just(mock(LoanTypes.class)));

        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> {
            Solicitud arg = inv.getArgument(0);
            return Mono.just(arg);
        });

        Mono<Solicitud> result = useCase.createSolicitud(input);

        StepVerifier.create(result)
                .assertNext(saved -> {
                    assertThat(saved.getSolicitud_id()).isNotBlank();
                    assertThat(saved.getStateId()).isEqualTo("estado-001");
                    assertThat(saved.getDocumentId()).isEqualTo("DOC-1");
                    assertThat(saved.getEmail()).isEqualTo("user@mail.com");
                })
                .verifyComplete();

        verify(userGateway).getUserEmailByDocument("DOC-1");
        verify(loanTypesUseCase).findByLoanTypeAndValidateAmount("LT-1", BigDecimal.valueOf(20000));
        verify(solicitudRepository).save(any(Solicitud.class));
    }

    @Test
    void createSolicitud_maps_external_service_exception_to_validation_AUTH() {
        Solicitud input = Solicitud.builder()
                .documentId("DOC-2")
                .amount(BigDecimal.valueOf(15000))
                .loanTypeId("LT-2")
                .build();

        ExternalServiceException upstream =
                new ExternalServiceException(1, "external-failure", java.util.List.of("auth-missing"));

        when(userGateway.getUserEmailByDocument("DOC-2")).thenReturn(Mono.error(upstream));

        Mono<Solicitud> result = useCase.createSolicitud(input);

        StepVerifier.create(result)
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(SolicitudValidationException.class);
                    var sve = (SolicitudValidationException) err;
                    assertThat(sve.getDomainErrors()).contains(SolicitudErrorCode.AUTH);
                    assertThat(sve.getMicroAuth()).contains("auth-missing");
                })
                .verify();

        verify(userGateway).getUserEmailByDocument("DOC-2");
        verifyNoInteractions(loanTypesUseCase);
        verifyNoInteractions(solicitudRepository);
    }

    @Test
    void createSolicitud_propagates_validation_error_from_loanTypesUseCase() {
        Solicitud input = Solicitud.builder()
                .documentId("DOC-3")
                .amount(BigDecimal.valueOf(9000))
                .loanTypeId("LT-3")
                .build();

        when(userGateway.getUserEmailByDocument("DOC-3")).thenReturn(Mono.just("a@b.com"));

        SolicitudValidationException sve =
                new SolicitudValidationException(List.of(), List.of(SolicitudErrorCode.LOAN_TYPE_NOT_REGISTERED), List.of());
        when(loanTypesUseCase.findByLoanTypeAndValidateAmount("LT-3", BigDecimal.valueOf(9000)))
                .thenReturn(Mono.error(sve));

        Mono<Solicitud> result = useCase.createSolicitud(input);

        StepVerifier.create(result)
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(SolicitudValidationException.class);
                    assertThat(((SolicitudValidationException) err).getDomainErrors())
                            .contains(SolicitudErrorCode.LOAN_TYPE_NOT_REGISTERED);
                })
                .verify();

        verify(userGateway).getUserEmailByDocument("DOC-3");
        verify(loanTypesUseCase).findByLoanTypeAndValidateAmount("LT-3", BigDecimal.valueOf(9000));
        verifyNoInteractions(solicitudRepository);
    }

    @Test
    void getAllSolicitud_emits_items_and_logs() {
        Solicitud a = Solicitud.builder().solicitud_id(UUID.randomUUID().toString()).build();
        Solicitud b = Solicitud.builder().solicitud_id(UUID.randomUUID().toString()).build();

        when(solicitudRepository.findAll()).thenReturn(Flux.just(a, b));

        StepVerifier.create(useCase.getAllSolicitud())
                .expectNext(a)
                .expectNext(b)
                .verifyComplete();

        verify(solicitudRepository).findAll();
        verifyNoMoreInteractions(solicitudRepository);
    }
}