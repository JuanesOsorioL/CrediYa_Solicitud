package co.com.crediya_solicitud.usecase.loantypes;

import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.loantypes.gateways.LoanTypesRepository;
import co.com.crediya_solicitud.usecase.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import co.com.crediya_solicitud.usecase.solicitud.logger.Logger;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
public class LoanTypesUseCase  {

    private final LoanTypesRepository loanTypesRepository;
    private final Logger logger;

    public Mono<LoanTypes> findByLoanTypeAndValidateAmount(String loanTypeId, BigDecimal amount) {
        return loanTypesRepository.findByloanTypeId(loanTypeId)
                .doOnSubscribe(sub -> logger.info("Consultando el loan type"))
                .switchIfEmpty(
                        Mono.error(new SolicitudValidationException(
                        List.of(),
                        List.of(SolicitudErrorCode.LOAN_TYPE_NOT_REGISTERED),
                        null)))
                .flatMap(loanType -> validateAmount(loanType, amount));
    }


    private Mono<LoanTypes> validateAmount(LoanTypes loanType, BigDecimal amount) {
        if (amount.compareTo(loanType.getMinimum_amount()) < 0 ||
                amount.compareTo(loanType.getMaximum_amount()) > 0) {
            logger.warn("El monto " + amount + " no cumple con el rango permitido [" +
                    loanType.getMinimum_amount() + " - " + loanType.getMaximum_amount() + "]");
            return Mono.error(new SolicitudValidationException(
                    List.of(),
                    List.of(SolicitudErrorCode.AMOUNT_INVALID),
                    null));
        }

        logger.info("El monto " + amount + " es válido para el tipo de préstamo " + loanType.getLoanTypeId());
        return Mono.just(loanType);
    }
}

