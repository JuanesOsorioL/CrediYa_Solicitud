package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.UserGateway;
import co.com.crediya_solicitud.model.error.SolicitudErrorCode;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import co.com.crediya_solicitud.usecase.loantypes.LoanTypesUseCase;
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
    private final UserGateway userGateway;


    @Override
    public Mono<Solicitud> createSolicitud(Solicitud solicitud, String token) {
        return userGateway.getUserEmailByDocument(solicitud.getDocument_id(),token)
                .doOnSubscribe(sub -> logger.info("Se inicia llamando a el Web client para validar si existe el documento"))
                .onErrorMap(ExternalServiceException.class, ex ->
                        new SolicitudValidationException(
                                List.of(),
                                List.of(SolicitudErrorCode.AUTH),
                                ex.getBody())
                )
                .flatMap(email -> {
                    logger.warn("se inserta al modelo de Solicitud el email");
                    Solicitud enriched = solicitud.toBuilder().email(email).build();
                    return loanTypesUseCase.findByLoanTypeAndValidateAmount(enriched.getLoanTypeId(), enriched.getAmount())
                            .doOnSubscribe(sub -> logger.info("Se inicia llamando a el caso de uso de tipo prestamo"))
                            .flatMap(exists -> {
                                Solicitud withId = enriched.toBuilder()

                                        .solicitud_id(UUID.randomUUID().toString())
                                        .state_id("estado-001")
                                        .build();
                                return solicitudRepository.save(withId)
                                        .doOnSubscribe(subscription -> logger.info("Se guardo Solicitud en la BD"))
                                        .map(saved -> saved.toBuilder()
                                                .document_id(solicitud.getDocument_id())
                                                .build()
                                        );
                            });
                });
    }

    @Override
    public Flux<Solicitud> getAllSolicitud() {
        return solicitudRepository.findAll()
                .doOnNext(u -> logger.info("Se buscan Solicitudes"));
    }
}
