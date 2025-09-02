package co.com.crediya_solicitud.r2dbc.solicitud;

import co.com.crediya_solicitud.r2dbc.entities.SolicitudEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;


public interface SolicitudReactiveRepository extends ReactiveCrudRepository<SolicitudEntity, String>, ReactiveQueryByExampleExecutor<SolicitudEntity> {

}
