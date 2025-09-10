package co.com.crediya_solicitud.usecase.loantypes;

import co.com.crediya_solicitud.model.error.SolicitudErrorCode;
import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.loantypes.gateways.LoanTypesRepository;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LoanTypesUseCaseTest {

    private LoanTypesRepository repository;
    private Logger logger;
    private LoanTypesUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(LoanTypesRepository.class);
        logger = mock(Logger.class);
        useCase = new LoanTypesUseCase(repository, logger);
    }

    @Test
    void whenLoanTypeNotFound_thenEmitValidationException_LOAN_TYPE_NOT_REGISTERED() {
        String loanTypeId = "LT-404";
        when(repository.findByloanTypeId(loanTypeId)).thenReturn(Mono.empty());

        Mono<LoanTypes> result = useCase.findByLoanTypeAndValidateAmount(loanTypeId, BigDecimal.TEN);

        StepVerifier.create(result)
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(SolicitudValidationException.class);
                    SolicitudValidationException sve = (SolicitudValidationException) err;
                    assertThat(sve.getDomainErrors()).contains(SolicitudErrorCode.LOAN_TYPE_NOT_REGISTERED);
                })
                .verify();

        verify(repository).findByloanTypeId(loanTypeId);
        verify(logger).info("Se consulta si el tipo de prestamo existe en BD");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenAmountInvalid_thenEmitValidationException_AMOUNT_INVALID() {
        String loanTypeId = "LT-001";
        BigDecimal amount = new BigDecimal("9999");

        LoanTypes loanType = mock(LoanTypes.class);
        when(loanType.isValidAmount(amount)).thenReturn(false);
        when(loanType.getMinimumAmount()).thenReturn(new BigDecimal("10000"));
        when(loanType.getMaximumAmount()).thenReturn(new BigDecimal("50000"));
        when(repository.findByloanTypeId(loanTypeId)).thenReturn(Mono.just(loanType));

        Mono<LoanTypes> result = useCase.findByLoanTypeAndValidateAmount(loanTypeId, amount);

        StepVerifier.create(result)
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(SolicitudValidationException.class);
                    SolicitudValidationException sve = (SolicitudValidationException) err;
                    assertThat(sve.getDomainErrors()).contains(SolicitudErrorCode.AMOUNT_INVALID);
                })
                .verify();

        verify(repository).findByloanTypeId(loanTypeId);
        verify(logger).info("Se consulta si el tipo de prestamo existe en BD");
        verify(logger).warn("El monto " + amount + " no cumple el rango permitido [" +
                loanType.getMinimumAmount() + " - " + loanType.getMaximumAmount() + "]");
    }

    @Test
    void whenAmountValid_thenReturnLoanType() {
        String loanTypeId = "LT-001";
        BigDecimal amount = new BigDecimal("20000");

        LoanTypes loanType = mock(LoanTypes.class);
        when(loanType.isValidAmount(amount)).thenReturn(true);
        when(loanType.getLoanTypeId()).thenReturn(loanTypeId);
        when(repository.findByloanTypeId(loanTypeId)).thenReturn(Mono.just(loanType));

        Mono<LoanTypes> result = useCase.findByLoanTypeAndValidateAmount(loanTypeId, amount);

        StepVerifier.create(result)
                .expectNext(loanType)
                .verifyComplete();

        verify(repository).findByloanTypeId(loanTypeId);
        verify(logger).info("Se consulta si el tipo de prestamo existe en BD");
        verify(logger).info("El monto " + amount + " es válido para el tipo de préstamo " + loanTypeId);
        verifyNoMoreInteractions(repository);
    }
}