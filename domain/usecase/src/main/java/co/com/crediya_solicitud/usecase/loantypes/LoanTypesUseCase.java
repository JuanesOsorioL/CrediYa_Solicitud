package co.com.crediya_solicitud.usecase.loantypes;

import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.model.exception.specificexceptions.BadRequestException;
import co.com.crediya_solicitud.model.exception.specificexceptions.NotFoundException;
import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.loantypes.gateways.LoanTypesRepository;
import co.com.crediya_solicitud.model.logger.Logger;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;

@RequiredArgsConstructor
public class LoanTypesUseCase {

    private final LoanTypesRepository loanTypesRepository;
    private final Logger logger;

    public Flux<LoanTypes> findAll() {
        return loanTypesRepository.findAll();
    }

    public Mono<LoanTypes> findByLoanTypeAndValidateAmount(String loanTypeId, BigDecimal amount) {
        // Validaciones rápidas de entrada (400)
        if (loanTypeId == null || loanTypeId.isBlank()) {
            return Mono.error(new BadRequestException(SolicitudErrorCode.ID_LOAN_TYPE_EMPTY));
        }
        if (Objects.isNull(amount)) {
            return Mono.error(new BadRequestException(SolicitudErrorCode.AMOUNT_EMPTY));
        }

        return loanTypesRepository.findByloanType(loanTypeId)
                .doOnSubscribe(s -> logger.info("Se consulta si el tipo de préstamo " + loanTypeId + " existe en BD"))
                .switchIfEmpty(Mono.defer(() -> {
                    logger.warn("Tipo de préstamo no registrado: " + loanTypeId);
                    return Mono.error(new NotFoundException(SolicitudErrorCode.LOAN_TYPE_NOT_REGISTERED));
                }))
                .flatMap(loanType -> validateAmount(loanType, amount))
                .doOnSuccess(lt -> logger.info("Validación OK: loanType = " + loanTypeId + ", amount = " + amount))
                .doOnError(err -> logger.warn("Validación falló: loanType = " + loanTypeId + ", amount = " + amount + ", error = " + err.getMessage()));
    }

    private Mono<LoanTypes> validateAmount(LoanTypes loanType, BigDecimal amount) {
        if (!loanType.isValidAmount(amount)) {
            logger.warn("El monto " + amount + " no cumple el rango permitido [" + loanType.getMinimumAmount() + " - " + loanType.getMaximumAmount() + "]");
            return Mono.error(new BadRequestException(SolicitudErrorCode.AMOUNT_INVALID));
        }
        return Mono.just(loanType)
                .doOnSuccess(lt -> logger.info("El monto " + amount + " es válido para el tipo de préstamo " + lt.getLoanTypeId() + " "));
    }
}