package co.com.crediya_solicitud.usecase.loantypes;


import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.loantypes.gateways.LoanTypesRepository;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.exception.specificexceptions.BadRequestException;
import co.com.crediya_solicitud.model.exception.specificexceptions.NotFoundException;
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
    void whenLoanTypeNotFound_thenPropagates_NotFoundException() {
        String loanTypeId = "LT-404";
        when(repository.findByloanType(loanTypeId)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.findByLoanTypeAndValidateAmount(loanTypeId, BigDecimal.TEN))
                .expectError(NotFoundException.class)
                .verify();

        verify(repository).findByloanType(loanTypeId);
        // El código actual incluye el id y la tilde en “préstamo”:
        verify(logger).info(argThat(msg ->
                msg.startsWith("Se consulta si el tipo de préstamo") && msg.contains(loanTypeId)
        ));
        verifyNoMoreInteractions(repository);
    }

    @Test
    void whenAmountInvalid_thenPropagates_BadRequestException() {
        String loanTypeId = "LT-001";
        BigDecimal amount = new BigDecimal("9999");

        LoanTypes loanType = mock(LoanTypes.class);
        when(loanType.isValidAmount(amount)).thenReturn(false);
        when(loanType.getMinimumAmount()).thenReturn(new BigDecimal("10000"));
        when(loanType.getMaximumAmount()).thenReturn(new BigDecimal("50000"));
        when(repository.findByloanType(loanTypeId)).thenReturn(Mono.just(loanType));

        StepVerifier.create(useCase.findByLoanTypeAndValidateAmount(loanTypeId, amount))
                .expectError(BadRequestException.class)
                .verify();

        verify(repository).findByloanType(loanTypeId);
        verify(logger).info(argThat(msg ->
                msg.startsWith("Se consulta si el tipo de préstamo") && msg.contains(loanTypeId)
        ));
        verify(logger).warn(argThat(msg ->
                msg.contains("no cumple el rango permitido") &&
                        msg.contains(loanType.getMinimumAmount().toString()) &&
                        msg.contains(loanType.getMaximumAmount().toString())
        ));
    }

    @Test
    void whenAmountValid_thenReturnLoanType_andLogs() {
        String loanTypeId = "LT-001";
        BigDecimal amount = new BigDecimal("20000");

        LoanTypes loanType = mock(LoanTypes.class);
        when(loanType.isValidAmount(amount)).thenReturn(true);
        when(loanType.getLoanTypeId()).thenReturn(loanTypeId);
        when(repository.findByloanType(loanTypeId)).thenReturn(Mono.just(loanType));

        StepVerifier.create(useCase.findByLoanTypeAndValidateAmount(loanTypeId, amount))
                .expectNext(loanType)
                .verifyComplete();

        verify(repository).findByloanType(loanTypeId);

        verify(logger).info(argThat(msg ->
                msg.startsWith("Se consulta si el tipo de préstamo") && msg.contains(loanTypeId)
        ));
        verify(logger).info(argThat(msg ->
                msg.contains("es válido") &&
                        msg.contains(loanTypeId) &&
                        msg.contains(amount.toString())
        ));
        verify(logger).info(argThat(msg ->
                msg.contains("Validación OK")
        ));

        verifyNoMoreInteractions(repository);
    }
}