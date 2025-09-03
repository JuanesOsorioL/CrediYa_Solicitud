package co.com.crediya_solicitud.model.solicitud.gateways;

import co.com.crediya_solicitud.model.solicitud.Solicitud;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SolicitudRepository {
    Mono<Solicitud> save(Solicitud solicitud);

    Flux<Solicitud> findAll();
}
