package co.com.crediya_solicitud.model.solicitud.gateways;

import co.com.crediya_solicitud.model.solicitud.Solicitud;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SolicitudRepository {

    Mono<Solicitud> save(Solicitud solicitud);

    Flux<Solicitud> findAllForReview(List<String> stateIds);

    Flux<Solicitud> findAll();

    Mono<Long> countAllForReview(List<String> stateIds);

    Mono<Solicitud> findSolicitud(String solicitudId);

    Mono<Boolean> solicitudHavethisstatus(String solicitudId, List<String> status);

    Mono<Solicitud> existSolicitudById(String solicitudId);

}
