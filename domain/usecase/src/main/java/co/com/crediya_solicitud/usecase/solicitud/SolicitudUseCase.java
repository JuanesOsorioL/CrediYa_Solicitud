package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.UserGateway;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya_solicitud.usecase.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import co.com.crediya_solicitud.usecase.loantypes.LoanTypesUseCase;
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
    private final UserGateway userGateway;


    @Override
    public Mono<Solicitud> createSolicitud(Solicitud solicitud) {

        return userGateway.getUserEmailByDocument(solicitud.getDocument_id())
                .onErrorMap(ExternalServiceException.class, ex ->
                        new SolicitudValidationException(
                                List.of(),
                                List.of(SolicitudErrorCode.AUTH),
                                ex.getBody())
                )
                .flatMap(email -> {
                    Solicitud enriched = solicitud.toBuilder().email(email).build();
                    return loanTypesUseCase.findByLoanTypeAndValidateAmount(enriched.getLoanTypeId(), enriched.getAmount())
                            .flatMap(exists -> {
                                Solicitud withId = enriched.toBuilder()
                                        .solicitud_id(UUID.randomUUID().toString())
                                        .state_id("estado-001")
                                        .build();
                                return solicitudRepository.save(withId).map(saved -> saved.toBuilder()
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
