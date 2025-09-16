package co.com.crediya_solicitud.r2dbc.state;

import co.com.crediya_solicitud.model.state.State;
import co.com.crediya_solicitud.model.state.gateways.StateRepository;
import co.com.crediya_solicitud.r2dbc.entities.StateEntity;
import co.com.crediya_solicitud.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StateReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        State,
        StateEntity,
        String,
        StateReactiveRepository
        > implements StateRepository {
    public StateReactiveRepositoryAdapter(StateReactiveRepository repository, ObjectMapper mapper) {

        super(repository, mapper, d -> mapper.mapBuilder(d, State.StateBuilder.class).build());
    }

}
