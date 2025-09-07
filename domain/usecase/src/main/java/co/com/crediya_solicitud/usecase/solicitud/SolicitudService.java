package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.TokenDto;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SolicitudService {
    Mono<Solicitud> createSolicitud(Solicitud solicitud, TokenDto token);
    Flux<Solicitud> getAllSolicitud();
}
