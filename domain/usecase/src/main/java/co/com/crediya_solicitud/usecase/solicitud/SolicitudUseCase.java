package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.model.exception.ExternalServiceException;
import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.solicitud_revision.SolicitudRevision;
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

    private static final String STATE_DEFAULT = "estado-001";
    private static final String APPROVED_STATE = "estado-004";

    @Override
    public Mono<Solicitud> createSolicitud(Solicitud solicitud) {
        return userGateway.getUserEmailByDocument(solicitud.getDocumentId())
                .doOnSubscribe(sub -> logger.info("SolicitudUseCase -> createSolicitud : Se inicia llamando a el Web client para validar si existe el documento"))
                .onErrorMap(ExternalServiceException.class, ex ->
                        new SolicitudValidationException(
                                List.of(),
                                List.of(SolicitudErrorCode.AUTH),
                                ex.getBody())
                )
                .flatMap(email -> {
                    logger.info("SolicitudUseCase -> createSolicitud : se inserta al modelo de Solicitud el email");
                    Solicitud enriched = solicitud.toBuilder().email(email).build();
                    return loanTypesUseCase.findByLoanTypeAndValidateAmount(enriched.getLoanTypeId(), enriched.getAmount())
                            .doOnSubscribe(sub -> logger.info("SolicitudUseCase -> createSolicitud : Se inicia llamando a el caso de uso de tipo préstamo"))
                            .flatMap(exists -> {
                                Solicitud withId = enriched.toBuilder()

                                        .solicitudId(UUID.randomUUID().toString())
                                        .stateId(STATE_DEFAULT)
                                        .build();
                                return solicitudRepository.save(withId)
                                        .doOnSubscribe(subscription -> logger.info("SolicitudUseCase -> createSolicitud : Se guardo Solicitud en la BD"))
                                        .map(saved -> saved.toBuilder()
                                                .documentId(solicitud.getDocumentId())
                                                .build()
                                        );
                            });
                });
    }


    @Override
    public Flux<SolicitudRevision> getSolicitudByRevision(List<String> status, int page, int size) {
        int p = Math.max(0, page);
        int sz = Math.max(1, size);
        long offset = (long) p * sz;

        Mono<Long> totalMono = solicitudRepository.countAllForReview(status)
                .doOnNext(total -> logger.info("getSolicitudByRevision total= " + total + ", status=" + status + ", page=" + p + ", size=" + sz + " "));

        // Valida rango ANTES de armar la página
        Flux<Solicitud> base = totalMono.flatMapMany(total -> {
            if (total == 0 || offset >= total) {
                logger.info("Página vacía: total=" + total + ", offset=" + offset + ",");
                return Flux.empty();
            }
            // Si aún no paginas en BD, usa skip/take sobre el stream ordenado en BD
            return solicitudRepository.findAllForReview(status)
                    .doOnSubscribe(s -> logger.info("Buscando solicitudes para revisión. status=" + status + " "))
                    .skip(offset)
                    .take(sz);
        }).cache(); // lo reutilizamos abajo

        // === Enriquecimientos ===
        Mono<Set<String>> emailsMono = base
                .map(Solicitud::getEmail)
                .map(this::normalizeEmail)
                .filter(e -> e != null && !e.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.toSet())
                .doOnNext(set -> logger.info("emailsMono size=" + set.size() + " "));

        Mono<Map<String, LoanTypes>> loanMapMono =
                loanTypesUseCase.findAll()
                        .collectMap(LoanTypes::getLoanTypeId, lt -> lt);

        Mono<Map<String, State>> stateMapMono =
                stateUseCase.findAll()
                        .collectMap(State::getStateId, st -> st);


        Mono<Map<String, User>> usersByEmailMono =
                emailsMono.flatMap(userGateway::getUsersByEmails)
                        .doOnNext(map -> logger.info("usersByEmailMono size= " + map.size() + " "));

        // Deuda total por email (solo aprobadas)
        Mono<Map<String, BigDecimal>> deudaByEmailMono =
                emailsMono.flatMap(emails ->
                        solicitudRepository.findAllForReview(List.of(APPROVED_STATE))
                                .map(s -> reactor.util.function.Tuples.of(
                                        normalizeEmail(s.getEmail()),
                                        s.getAmount() == null ? BigDecimal.ZERO : s.getAmount()))
                                .filter(t -> t.getT1() != null && emails.contains(t.getT1()))
                                .groupBy(reactor.util.function.Tuple2::getT1)
                                .flatMap(g -> g
                                        .map(reactor.util.function.Tuple2::getT2)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                                        .map(sum -> reactor.util.function.Tuples.of(g.key(), sum)))
                                .collectMap(t -> t.getT1(), t -> t.getT2())
                ).doOnNext(map -> logger.info("deudaByEmail size= " + map.size() + " "));


        return Mono.zip(loanMapMono, stateMapMono, usersByEmailMono, deudaByEmailMono)
                .flatMapMany(t -> {
                    var loanMap = t.getT1();
                    var stateMap = t.getT2();
                    var userByEmail = t.getT3();
                    var deudaByMail = t.getT4();

                    return base.flatMap(s -> {
                        var emailNorm = normalizeEmail(s.getEmail());
                        if (emailNorm == null || emailNorm.isBlank()) return Mono.empty();

                        var user = userByEmail.get(emailNorm);
                        var lt = loanMap.get(s.getLoanTypeId());
                        var st = stateMap.get(s.getStateId());

                        String fullName = user != null ? (user.firstName() + " " + user.lastName()).trim() : null;
                        String loanTypeName = lt != null ? lt.getName() : null;
                        Integer rate = lt != null ? lt.getInterestRate() : null;
                        String stateName = st != null ? st.getName() : null;
                        BigDecimal salary = user != null ? user.baseSalary() : null;
                        BigDecimal debt = deudaByMail.getOrDefault(emailNorm, BigDecimal.ZERO);

                        return Mono.just(new SolicitudRevision(
                                s.getAmount(),
                                s.getTerm(),
                                s.getEmail(),
                                fullName,
                                loanTypeName,
                                rate,
                                stateName,
                                salary,
                                debt

                        ));
                    });
                });
    }

    @Override
    public Mono<Long> countByStatus(List<String> status) {
        return solicitudRepository.countAllForReview(status);
    }

    private String normalizeEmail(String e) {
        return e == null ? null : e.trim().toLowerCase();
    }

}
