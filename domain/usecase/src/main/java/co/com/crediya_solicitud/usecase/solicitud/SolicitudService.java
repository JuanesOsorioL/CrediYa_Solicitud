package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.solicitud.Solicitud;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SolicitudService {
    Mono<Solicitud> createSolicitud(Solicitud solicitud,String token);
    Flux<Solicitud> getAllSolicitud();
}
