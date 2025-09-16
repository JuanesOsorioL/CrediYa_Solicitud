package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.solicitud_revision.SolicitudRevision;
import co.com.crediya_solicitud.model.state.State;
import co.com.crediya_solicitud.model.user.User;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import co.com.crediya_solicitud.usecase.loantypes.LoanTypesUseCase;
import co.com.crediya_solicitud.usecase.state.StateUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SolicitudUseCaseTest {

    private SolicitudRepository solicitudRepository;
    private LoanTypesUseCase loanTypesUseCase;
    private Logger logger;
    private UserGateway userGateway;
    private StateUseCase stateUseCase;
    private SolicitudUseCase useCase;

    @BeforeEach
    void setUp() {
        solicitudRepository = mock(SolicitudRepository.class);
        loanTypesUseCase = mock(LoanTypesUseCase.class);
        logger = mock(Logger.class);
        userGateway = mock(UserGateway.class);
        stateUseCase = mock(StateUseCase.class);
        useCase = new SolicitudUseCase(solicitudRepository, loanTypesUseCase, stateUseCase, logger, userGateway);
    }

    @Test
    void createSolicitud_success_flow_persists_and_returns_saved_with_original_document() {
        Solicitud input = Solicitud.builder()
                .solicitudId(null)
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
                    assertThat(saved.getSolicitudId()).isNotBlank();
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
                new ExternalServiceException(500, "external-failure", "Auth error", List.of("auth-missing"));

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

        when(userGateway.getUserEmailByDocument("DOC-3"))
                .thenReturn(Mono.just("a@b.com"));

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
    void getSolicitudByRevision_enriches_with_user_loan_state_and_debt() {
        var status = List.of("estado-001");

        when(solicitudRepository.countAllForReview(eq(status)))
                .thenReturn(Mono.just(1L));

        Solicitud base = Solicitud.builder()
                .solicitudId("S-1")
                .email("USER1@Mail.com")
                .amount(new BigDecimal("10000"))
                .term(12)
                .loanTypeId("LT-1")
                .stateId("estado-001")
                .build();

        when(solicitudRepository.findAllForReview(eq(status)))
                .thenReturn(Flux.just(base));

        LoanTypes lt1 = new LoanTypes();
        lt1.setLoanTypeId("LT-1");
        lt1.setName("Libre");
        lt1.setInterestRate(15);

        when(loanTypesUseCase.findAll()).thenReturn(Flux.just(lt1));

        State st = new State();
        st.setStateId("estado-001");
        st.setName("En revisión");

        when(stateUseCase.findAll()).thenReturn(Flux.just(st));

        User u = new User("1112", "Ana", "Perez", null, null, "1111", "user1@mail.com", "ddsdsds", "Customer", new BigDecimal("2000"));
        when(userGateway.getUsersByEmails(eq(Set.of("user1@mail.com"))))
                .thenReturn(Mono.just(Map.of("user1@mail.com", u)));

        var approvedStatus = List.of("estado-004");
        Solicitud ap1 = Solicitud.builder().email("user1@mail.com").amount(new BigDecimal("1000")).build();
        Solicitud ap2 = Solicitud.builder().email("user1@mail.com").amount(new BigDecimal("500")).build();

        when(solicitudRepository.findAllForReview(eq(approvedStatus)))
                .thenReturn(Flux.just(ap1, ap2));

        Flux<SolicitudRevision> out = useCase.getSolicitudByRevision(status, 0, 10);

        StepVerifier.create(out)
                .assertNext(sr -> {
                    assertThat(sr.amount()).isEqualByComparingTo("10000");
                    assertThat(sr.term()).isEqualTo(12);
                    assertThat(sr.email()).isEqualTo("USER1@Mail.com");
                    assertThat(sr.fullName()).isEqualTo("Ana Perez");
                    assertThat(sr.loanTypeName()).isEqualTo("Libre");
                    assertThat(sr.interestRate()).isEqualTo(15);
                    assertThat(sr.stateName()).isEqualTo("En revisión");
                    assertThat(sr.baseSalary()).isEqualByComparingTo("2000");
                    assertThat(sr.debt()).isEqualByComparingTo("1500");
                })
                .verifyComplete();

        verify(solicitudRepository).countAllForReview(eq(status));
        verify(solicitudRepository).findAllForReview(eq(status));
        verify(loanTypesUseCase).findAll();
        verify(stateUseCase).findAll();
        verify(userGateway).getUsersByEmails(eq(Set.of("user1@mail.com")));
        verify(solicitudRepository).findAllForReview(eq(approvedStatus));
    }


    @Test
    void getSolicitudByRevision_returns_empty_when_total_is_zero() {
        var status = List.of("estado-001");

        when(solicitudRepository.countAllForReview(eq(status)))
                .thenReturn(Mono.just(0L));

        when(loanTypesUseCase.findAll()).thenReturn(Flux.empty());
        when(stateUseCase.findAll()).thenReturn(Flux.empty());
        when(userGateway.getUsersByEmails(anySet())).thenReturn(Mono.just(Map.of()));
        when(solicitudRepository.findAllForReview(eq(List.of("estado-004")))).thenReturn(Flux.empty());

        StepVerifier.create(useCase.getSolicitudByRevision(status, 0, 10))
                .verifyComplete();

        verify(solicitudRepository).countAllForReview(eq(status));
    }

    @Test
    void getSolicitudByRevision_returns_empty_when_page_out_of_range() {
        var status = List.of("estado-001");

        when(solicitudRepository.countAllForReview(eq(status)))
                .thenReturn(Mono.just(5L));

        when(loanTypesUseCase.findAll()).thenReturn(Flux.empty());
        when(stateUseCase.findAll()).thenReturn(Flux.empty());
        when(userGateway.getUsersByEmails(anySet())).thenReturn(Mono.just(Map.of()));
        when(solicitudRepository.findAllForReview(eq(List.of("estado-004")))).thenReturn(Flux.empty());

        StepVerifier.create(useCase.getSolicitudByRevision(status, 1, 10))
                .verifyComplete();

        verify(solicitudRepository).countAllForReview(eq(status));
        verify(solicitudRepository, never()).findAllForReview(eq(status));
    }

    @Test
    void countByStatus_delegates_to_repository() {
        var status = List.of("estado-001", "estado-002");
        when(solicitudRepository.countAllForReview(eq(status))).thenReturn(Mono.just(42L));

        StepVerifier.create(useCase.countByStatus(status))
                .expectNext(42L)
                .verifyComplete();

        verify(solicitudRepository).countAllForReview(eq(status));
    }
}