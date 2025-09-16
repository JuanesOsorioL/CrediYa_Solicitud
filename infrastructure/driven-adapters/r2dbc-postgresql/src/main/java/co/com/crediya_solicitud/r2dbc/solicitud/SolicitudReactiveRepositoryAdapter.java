package co.com.crediya_solicitud.r2dbc.solicitud;

import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya_solicitud.r2dbc.entities.SolicitudEntity;
import co.com.crediya_solicitud.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public class SolicitudReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Solicitud,
        SolicitudEntity,
        String,
        SolicitudReactiveRepository
        > implements SolicitudRepository {
    public SolicitudReactiveRepositoryAdapter(SolicitudReactiveRepository repository, ObjectMapper mapper) {

        super(repository, mapper, d -> mapper.mapBuilder(d, Solicitud.SolicitudBuilder.class).build());
    }

    @Override
    public Flux<Solicitud> findAllForReview(List<String> stateIds) {
        return repository.findByStateIdIn(stateIds)
                .map(this::toEntity);
    }

    @Override
    public Mono<Long> countAllForReview(List<String> stateIds) {
        return repository.countByStateIdIn(stateIds);
    }

}
