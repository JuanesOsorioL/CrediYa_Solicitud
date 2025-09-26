package co.com.crediya_solicitud.model.state.gateways;

import co.com.crediya_solicitud.model.state.State;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StateRepository {
    Flux<State> findAll();

    Mono<String> name(String stateId);
}
