package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.error.SolicitudErrorCode;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.requestRevisionItem.SolicitudRevision;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.state.State;
import co.com.crediya_solicitud.model.user.User;
import co.com.crediya_solicitud.usecase.exception.SolicitudValidationException;
import co.com.crediya_solicitud.usecase.loantypes.LoanTypesUseCase;
import co.com.crediya_solicitud.usecase.state.StateUseCase;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RequiredArgsConstructor
public class SolicitudUseCase implements SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final LoanTypesUseCase loanTypesUseCase;
    private final StateUseCase stateUseCase;

    private final Logger logger;
    private final UserGateway userGateway;

    private final String stateDefault = "estado-001";


    private String normalizeEmail(String e) {
        return e == null ? null : e.trim().toLowerCase(java.util.Locale.ROOT);
    }


    @Override
    public Mono<Solicitud> createSolicitud(Solicitud solicitud) {
        return userGateway.getUserEmailByDocument(solicitud.getDocumentId())
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
                                        .stateId(stateDefault)
                                        .build();
                                return solicitudRepository.save(withId)
                                        .doOnSubscribe(subscription -> logger.info("Se guardo Solicitud en la BD"))
                                        .map(saved -> saved.toBuilder()
                                                .documentId(solicitud.getDocumentId())
                                                .build()
                                        );
                            });
                });
    }


    @Override
    public Flux<SolicitudRevision> getAllSolicitud() {

        Flux<Solicitud> base = solicitudRepository.findAllForReview(
                List.of(stateDefault, "estado-002", "estado-003")
        ).doOnNext(s -> logger.info("Buscando documentos de Solicitudes para revisión"));

        Mono<Set<String>> emailsMono = base
                .map(Solicitud::getEmail)
                .map(this::normalizeEmail)
                .filter(e -> e != null && !e.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.toSet());

        Mono<Map<String, LoanTypes>> loanMapMono =
                loanTypesUseCase.findAll()
                        .collectMap(LoanTypes::getLoanTypeId, lt -> lt);

        Mono<Map<String, State>> stateMapMono =
                stateUseCase.findAll()
                        .collectMap(State::getStateId, st -> st);

        Mono<Map<String, User>> usersByEmailMono =
                emailsMono.flatMap(userGateway::getUsersByEmails);

        Mono<Map<String, BigDecimal>> deudaByEmailMono =
                emailsMono.flatMap(emails ->
                        solicitudRepository.findAllForReview(List.of("estado-004"))
                                .doOnNext(s -> logger.info("Buscando Solicitudes aprobadas"))
                                .map(Solicitud::getEmail)
                                .map(this::normalizeEmail)
                                .filter(e -> e != null && emails.contains(e))
                                .zipWith(
                                        solicitudRepository.findAllForReview(List.of("estado-004"))
                                                .map(Solicitud::getAmount)
                                                .map(a -> a == null ? BigDecimal.ZERO : a)
                                        , (e, amount) -> reactor.util.function.Tuples.of(e, amount))
                                .groupBy(t -> t.getT1())
                                .flatMap(g -> g
                                        .map(t -> t.getT2())
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                                        .map(sum -> reactor.util.function.Tuples.of(g.key(), sum))
                                )
                                .collectMap(t -> t.getT1(), t -> t.getT2())
                );

        Flux<Solicitud> baseFluxStream = base;

        return Mono.zip(loanMapMono, stateMapMono, usersByEmailMono, deudaByEmailMono)
                .flatMapMany(t -> {
                    var loanMap = t.getT1();
                    var stateMap = t.getT2();
                    var userByMail = t.getT3();
                    var deudaByMail = t.getT4();

                    return baseFluxStream.flatMap(s -> {
                        var emailNorm = normalizeEmail(s.getEmail());
                        if (emailNorm == null || emailNorm.isBlank()) {
                            return Mono.empty();
                        }

                        var user = userByMail.get(emailNorm);
//                        if (user == null) {
//                            return Mono.empty();
//                        }

                        var lt = loanMap.get(s.getLoanTypeId());
                        var st = stateMap.get(s.getStateId());
                        var deuda = deudaByMail.getOrDefault(emailNorm, BigDecimal.ZERO);

                        return Mono.just(new SolicitudRevision(
                                s.getAmount(),
                                s.getTerm(),
                                s.getEmail(),
                                (user.firstName() + " " + user.lastName()).trim(),
                                lt != null ? lt.getName() : null,
                                lt != null ? lt.getInterestRate() : null,
                                st != null ? st.getName() : null,
                                user.baseSalary(),
                                deuda
                        ));
                    });
                });
    }
}
