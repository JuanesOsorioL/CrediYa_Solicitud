package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.model.exception.specificexceptions.ConflictException;
import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.solicitud_revision.SolicitudRevision;
import co.com.crediya_solicitud.model.sqs.Decision;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
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
    void createSolicitud_success_sets_default_state_and_persists() {
        Solicitud input = Solicitud.builder()
                .solicitudId(null)
                .documentId("DOC-1")
                .amount(BigDecimal.valueOf(20000))
                .loanTypeId("LT-1")
                .email(null)
                .term(12)
                .stateId(null)
                .build();

        when(loanTypesUseCase.findByLoanTypeAndValidateAmount("LT-1", BigDecimal.valueOf(20000)))
                .thenReturn(Mono.just(new LoanTypes()));

        when(solicitudRepository.save(any(Solicitud.class))).thenAnswer(inv -> {
            Solicitud arg = inv.getArgument(0);

            return Mono.just(arg.toBuilder().solicitudId("GEN-1").build());
        });

        Mono<Solicitud> result = useCase.createSolicitud(input);

        StepVerifier.create(result)
                .assertNext(saved -> {
                    assertThat(saved.getSolicitudId()).isEqualTo("GEN-1");
                    assertThat(saved.getStateId()).isEqualTo("estado-001");
                    assertThat(saved.getDocumentId()).isEqualTo("DOC-1");
                    assertThat(saved.getEmail()).isNull();
                })
                .verifyComplete();

        verify(loanTypesUseCase).findByLoanTypeAndValidateAmount("LT-1", BigDecimal.valueOf(20000));
        verify(solicitudRepository).save(any(Solicitud.class));
        verifyNoInteractions(userGateway);
    }

    @Test
    void createSolicitud_propagates_validation_error_from_loanTypesUseCase() {
        Solicitud input = Solicitud.builder()
                .documentId("DOC-3")
                .amount(BigDecimal.valueOf(9000))
                .loanTypeId("LT-3")
                .build();


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

        verify(loanTypesUseCase).findByLoanTypeAndValidateAmount("LT-3", BigDecimal.valueOf(9000));
        verifyNoInteractions(solicitudRepository, userGateway);
    }


    @Test
    void getSolicitudByRevision_enriches_with_user_loan_state_and_debt() {
        var status = List.of("estado-001");

        when(solicitudRepository.countAllForReview(eq(status)))
                .thenReturn(Mono.just(1L));

        Solicitud base = Solicitud.builder()
                .solicitudId("S-1")
                .email("user1@mail.com")
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
        when(userGateway.getUsersByEmails(eq(List.of("user1@mail.com"))))
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
                    assertThat(sr.email()).isEqualTo("user1@mail.com");
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
        verify(userGateway).getUsersByEmails(eq(List.of("user1@mail.com")));
        verify(solicitudRepository).findAllForReview(eq(approvedStatus));
    }


    @Test
    void getSolicitudByRevision_returns_empty_when_total_is_zero() {
        var status = List.of("estado-001");

        when(solicitudRepository.countAllForReview(eq(status)))
                .thenReturn(Mono.just(0L));

        when(loanTypesUseCase.findAll()).thenReturn(Flux.empty());
        when(stateUseCase.findAll()).thenReturn(Flux.empty());
        when(userGateway.getUsersByEmails(anyList())).thenReturn(Mono.just(Map.of()));
        when(solicitudRepository.findAllForReview(eq(List.of("estado-004")))).thenReturn(Flux.empty());

        StepVerifier.create(useCase.getSolicitudByRevision(status, 0, 10))
                .verifyComplete();

        verify(solicitudRepository).countAllForReview(eq(status));
        verifyNoMoreInteractions(solicitudRepository);
        verifyNoInteractions(loanTypesUseCase, stateUseCase, userGateway);
    }

    @Test
    void getSolicitudByRevision_returns_empty_when_page_out_of_range() {
        var status = List.of("estado-001");

        when(solicitudRepository.countAllForReview(eq(status)))
                .thenReturn(Mono.just(5L));

        when(loanTypesUseCase.findAll()).thenReturn(Flux.empty());
        when(stateUseCase.findAll()).thenReturn(Flux.empty());
        when(userGateway.getUsersByEmails(anyList())).thenReturn(Mono.just(Map.of()));
        when(solicitudRepository.findAllForReview(eq(List.of("estado-004")))).thenReturn(Flux.empty());

        StepVerifier.create(useCase.getSolicitudByRevision(status, 1, 10))
                .verifyComplete();

        verify(solicitudRepository).countAllForReview(eq(status));
        verify(solicitudRepository, never()).findAllForReview(eq(status));
        verifyNoInteractions(loanTypesUseCase, stateUseCase, userGateway);
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


    @Test
    void validateUpdateSolicitud_success_maps_and_returns_enriched_decision() {
        var solicitudActual = Solicitud.builder()
                .solicitudId("S-1")
                .email("user@mail.com")
                .stateId("estado-001")
                .build();

        when(solicitudRepository.findSolicitud("S-1"))
                .thenReturn(Mono.just(solicitudActual));

        when(solicitudRepository.solicitudHavethisstatus(eq("S-1"), anyList()))
                .thenReturn(Mono.just(true));

        when(stateUseCase.findNameByStateId("estado-003"))
                .thenReturn(Mono.just("Aprobada manual"));

        var in = new Decision("S-1", "estado-003", "", "Motivo X", "", "", 0);

        StepVerifier.create(useCase.validateUpdateSolicitud(in))
                .assertNext(out ->{
                    assertThat(out.solicitudId()).isEqualTo("S-1");
                    assertThat(out.stateId()).isEqualTo("estado-003");
                    assertThat(out.nameStateId()).isEqualTo("Aprobada manual");
                    assertThat(out.motivo()).isEqualTo("Motivo X");
                    assertThat(out.email()).isEqualTo("user@mail.com");
                })
                .verifyComplete();

        verify(solicitudRepository).findSolicitud("S-1");
        verify(solicitudRepository).solicitudHavethisstatus(eq("S-1"), anyList());
        verify(stateUseCase).findNameByStateId("estado-003");
    }

    @Test
    void validateUpdateSolicitud_throws_conflict_when_solicitud_not_found() {
        when(solicitudRepository.findSolicitud("NOPE"))
                .thenReturn(Mono.empty());

        var in = new Decision("NOPE", "estado-003", "", "Motivo", "", "", 0);

        StepVerifier.create(useCase.validateUpdateSolicitud(in))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(ConflictException.class);
                    assertThat(((ConflictException) err).code())
                            .isEqualTo(SolicitudErrorCode.SOLICITUD_NOT_EXIST.getCode());
                })
                .verify();

        verify(solicitudRepository).findSolicitud("NOPE");
        verify(solicitudRepository, never()).solicitudHavethisstatus(anyString(), anyList());
        verifyNoInteractions(stateUseCase);
    }

    @Test
    void validateUpdateSolicitud_throws_conflict_when_state_not_permitted() {
        var solicitudActual = Solicitud.builder()
                .solicitudId("S-2")
                .email("mail@x.com")
                .stateId("estado-002")
                .build();

        when(solicitudRepository.findSolicitud("S-2"))
                .thenReturn(Mono.just(solicitudActual));

        when(solicitudRepository.solicitudHavethisstatus(eq("S-2"), anyList()))
                .thenReturn(Mono.just(false));

        var in = new Decision("S-2", "estado-003", "", "Motivo", "", "", 0);

        StepVerifier.create(useCase.validateUpdateSolicitud(in))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(ConflictException.class);
                    assertThat(((ConflictException) err).code())
                            .isEqualTo(SolicitudErrorCode.SOLICITUD_HAVE_OTHER_STATUS.getCode());
                })
                .verify();

        verify(solicitudRepository).findSolicitud("S-2");
        verify(solicitudRepository).solicitudHavethisstatus(eq("S-2"), anyList());
        verifyNoInteractions(stateUseCase);
    }

    @Test
    void updateStateOfSolicitud_completes_when_not_found() {
        when(solicitudRepository.findSolicitud("UNKNOWN"))
                .thenReturn(Mono.empty());

        var in = new Decision("UNKNOWN", "estado-003", "Aprobada manual", "Motivo", "mail@x.com", "", 0);

        StepVerifier.create(useCase.updateStateOfSolicitud(in))
                .verifyComplete();

        verify(solicitudRepository).findSolicitud("UNKNOWN");
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void updateStateOfSolicitud_noop_when_state_already_equals() {
        var s = Solicitud.builder()
                .solicitudId("S-3")
                .stateId("estado-003")
                .build();

        when(solicitudRepository.findSolicitud("S-3"))
                .thenReturn(Mono.just(s));

        when(stateUseCase.findNameByStateId("estado-003"))
                .thenReturn(Mono.just("Aprobada manual"));

        var in = new Decision("S-3", "estado-003", "Aprobada manual", "Motivo", "mail@x.com", "", 0);

        StepVerifier.create(useCase.updateStateOfSolicitud(in))
                .verifyComplete();

        verify(solicitudRepository).findSolicitud("S-3");
        verify(stateUseCase).findNameByStateId("estado-003");
        verify(solicitudRepository, never()).save(any());
    }

    @Test
    void updateStateOfSolicitud_updates_and_saves_when_different() {
        var s = Solicitud.builder()
                .solicitudId("S-4")
                .stateId("estado-001")
                .build();

        when(solicitudRepository.findSolicitud("S-4"))
                .thenReturn(Mono.just(s));

        when(stateUseCase.findNameByStateId("estado-001"))
                .thenReturn(Mono.just("En revisión"));

        when(solicitudRepository.save(any(Solicitud.class)))
                .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        var in = new Decision("S-4", "estado-003", "Aprobada manual", "Motivo", "mail@x.com", "", 0);

        StepVerifier.create(useCase.updateStateOfSolicitud(in))
                .verifyComplete();

        verify(solicitudRepository).findSolicitud("S-4");
        verify(stateUseCase).findNameByStateId("estado-001");
        verify(solicitudRepository).save(argThat(saved ->
                "S-4".equals(saved.getSolicitudId()) &&
                        "estado-003".equals(saved.getStateId())
        ));
    }
}