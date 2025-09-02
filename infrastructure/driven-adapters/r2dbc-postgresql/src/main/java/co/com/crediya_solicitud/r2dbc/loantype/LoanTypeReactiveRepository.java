package co.com.crediya_solicitud.r2dbc.loantype;

import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.r2dbc.entities.LoanTypeEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface LoanTypeReactiveRepository
        extends ReactiveCrudRepository<LoanTypeEntity, String>,
        ReactiveQueryByExampleExecutor<LoanTypeEntity> {

   // Mono<Boolean> existsByLoanTypeId(String loanTypeId);


    Mono<LoanTypes> findByLoanTypeId(String loanTypeId);


}
