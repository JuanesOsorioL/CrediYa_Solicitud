package co.com.crediya_solicitud.r2dbc.solicitud;

import co.com.crediya_solicitud.r2dbc.entities.SolicitudEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;


public interface SolicitudReactiveRepository extends ReactiveCrudRepository<SolicitudEntity, String>, ReactiveQueryByExampleExecutor<SolicitudEntity> {

    Flux<SolicitudEntity> findByStateIdIn(Collection<String> stateIds);

    Mono<Long> countByStateIdIn(Collection<String> stateIds);

    Mono<SolicitudEntity> findBySolicitudId(String solicitudId);

    Mono<Boolean> existsBySolicitudIdAndStateIdIn(String solicitudId, Collection<String> stateIds);

    Mono<SolicitudEntity> findSolicitudEntitiesBySolicitudId(String solicitudId);

}

