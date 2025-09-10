package co.com.crediya_solicitud.model.state.gateways;

import co.com.crediya_solicitud.model.state.State;
import reactor.core.publisher.Flux;

public interface StateRepository {
    Flux<State> findAll();

}
