package co.com.crediya_solicitud.r2dbc.loantype;

import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.loantypes.gateways.LoanTypesRepository;
import co.com.crediya_solicitud.r2dbc.entities.LoanTypeEntity;
import co.com.crediya_solicitud.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class LoanTypeReactiveRepositoryAdapter
        extends ReactiveAdapterOperations<LoanTypes, LoanTypeEntity, String, LoanTypeReactiveRepository>
        implements LoanTypesRepository {

    public LoanTypeReactiveRepositoryAdapter(LoanTypeReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.mapBuilder(d, LoanTypes.LoanTypesBuilder.class).build());
    }

    @Override
    public Mono<LoanTypes> findByloanTypeId(String loanTypeId) {
        return repository.findByLoanTypeId(loanTypeId);
    }
}
