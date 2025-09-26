package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud_revision.SolicitudRevision;
import co.com.crediya_solicitud.model.sqs.Decision;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SolicitudService {
    Mono<Solicitud> createSolicitud(Solicitud solicitud);

    Flux<SolicitudRevision> getSolicitudByRevision(List<String> status, int page, int size);

    Mono<Long> countByStatus(List<String> status);

    Mono<Decision> validateUpdateSolicitud(Decision decision);

}
