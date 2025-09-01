package co.com.crediya_solicitud.usecase.loantypes;

import co.com.crediya_solicitud.model.loantypes.gateways.LoanTypesRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class LoanTypesUseCase  {

    private final LoanTypesRepository loanTypesRepository;

    public Mono<Boolean> existIdTypeLoan(String loan_type_id) {
        return loanTypesRepository.existsByLoanTypeId(loan_type_id);
    }
}
