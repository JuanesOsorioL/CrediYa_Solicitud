package co.com.crediya_solicitud.r2dbc.state;

import co.com.crediya_solicitud.r2dbc.entities.StateEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;


public interface StateReactiveRepository extends ReactiveCrudRepository<StateEntity, String>, ReactiveQueryByExampleExecutor<StateEntity> {


}

