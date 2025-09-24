package co.com.crediya_solicitud.r2dbc.state;

import co.com.crediya_solicitud.r2dbc.entities.StateEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;


public interface StateReactiveRepository extends ReactiveCrudRepository<StateEntity, String>, ReactiveQueryByExampleExecutor<StateEntity> {
    Mono<StateEntity> findByStateId(String stateId);
}

