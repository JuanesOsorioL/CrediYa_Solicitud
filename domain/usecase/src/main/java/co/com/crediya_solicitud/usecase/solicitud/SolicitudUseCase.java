package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya_solicitud.usecase.loantypes.LoanTypesUseCase;
import co.com.crediya_solicitud.usecase.solicitud.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.usecase.solicitud.exception.SolicitudValidationException;
import co.com.crediya_solicitud.usecase.solicitud.logger.Logger;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class SolicitudUseCase implements SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final LoanTypesUseCase loanTypesUseCase;
    private final Logger logger;


    @Override
    public Mono<Solicitud> createSolicitud(Solicitud solicitud) {
        return loanTypesUseCase.existIdTypeLoan(solicitud.getLoanTypeId())
                .flatMap(exists -> {
                    if (!exists) {
                        logger.info("Tipo prestamo si existe");

                        return Mono.error(new SolicitudValidationException(List.of(), List.of(SolicitudErrorCode.LOAN_TYPE_NOT_REGISTERED)));
                    }
                    logger.info("El Tipo prestamo ingresado No existe, se agrega un UUID para guardarlo");
                    Solicitud withId = solicitud.toBuilder()
                          .solicitud_id(UUID.randomUUID().toString())
                            .state_id("estado-001")
                            .build();
                    return solicitudRepository.save(withId)
                    .doOnNext(u -> logger.info("Solicitud guardada con id "));
                });
    }

    @Override
    public Flux<Solicitud> getAllSolicitud() {
        return solicitudRepository.findAll()
                .doOnNext(u -> logger.info("Se buscan Solicitudes"));
    }
}
