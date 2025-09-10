package co.com.crediya_solicitud.usecase.loantypes;

import co.com.crediya_solicitud.model.error.SolicitudErrorCode;
import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.loantypes.gateways.LoanTypesRepository;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
public class LoanTypesUseCase {

    private final LoanTypesRepository loanTypesRepository;
    private final Logger logger;

    public Flux<LoanTypes> findAll(){
        return loanTypesRepository.findAll();
    }

    public Mono<LoanTypes> findByLoanTypeAndValidateAmount(String loanTypeId, BigDecimal amount) {
        return loanTypesRepository.findByloanTypeId(loanTypeId)
                .doOnSubscribe(sub -> logger.info("Se consulta si el tipo de prestamo existe en BD"))
                .switchIfEmpty(
                        Mono.error(new SolicitudValidationException(
                                List.of(),
                                List.of(SolicitudErrorCode.LOAN_TYPE_NOT_REGISTERED),
                                null))).doOnNext(error -> logger.error("Tipo de prestamo no existe"))
                .flatMap(loanType -> validateAmount(loanType, amount));
    }


    private Mono<LoanTypes> validateAmount(LoanTypes loanType, BigDecimal amount) {
        if (!loanType.isValidAmount(amount)) {
            logger.warn("El monto " + amount + " no cumple el rango permitido [" + loanType.getMinimumAmount() + " - " + loanType.getMaximumAmount() + "]");
            return Mono.error(new SolicitudValidationException(
                    List.of(),
                    List.of(SolicitudErrorCode.AMOUNT_INVALID),
                    null));
        }
        return Mono.just(loanType).doOnSuccess(entity -> logger.info("El monto " + amount + " es válido para el tipo de préstamo " + entity.getLoanTypeId()));
    }
}

