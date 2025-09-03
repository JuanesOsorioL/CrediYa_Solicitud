package co.com.crediya_solicitud.model.loantypes.gateways;

import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface LoanTypesRepository {
    Flux<LoanTypes> findAll();

    Mono<LoanTypes> findByloanTypeId(String loanTypeId);
}
