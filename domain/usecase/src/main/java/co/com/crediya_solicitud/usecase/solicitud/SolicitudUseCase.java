package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.model.exception.specificexceptions.ConflictException;
import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.solicitud_revision.SolicitudRevision;
import co.com.crediya_solicitud.model.sqs.Decision;
import co.com.crediya_solicitud.model.sqs.SqsReceiveGateway;
import co.com.crediya_solicitud.model.state.State;
import co.com.crediya_solicitud.model.user.User;
import co.com.crediya_solicitud.usecase.loantypes.LoanTypesUseCase;
import co.com.crediya_solicitud.usecase.state.StateUseCase;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class SolicitudUseCase implements SolicitudService, SqsReceiveGateway {

    public static final String GUARDADO_DE_LA_NUEVA_SOLICITUD = "SolicitudUseCase -> createSolicitud : Cumple se continua con el guardado de la nueva solicitud";
    public static final String GUARDO_SOLICITUD_EN_LA_BD = "SolicitudUseCase -> createSolicitud : Se guardo Solicitud en la BD";
    public static final String BUSCANDO_SOLICITUDES_PARA_REVISION_CON_ESTADO = "SolicitudUseCase -> getSolicitudByRevision : Buscando solicitudes para revisión. con estado = ";
    public static final String CANTIDAD_DE_EMAIL = "SolicitudUseCase -> getSolicitudByRevision : cantidad de email = ";
    public static final String CANTIDAD_DE_USUARIOS_ENCONTRADOS_POR_EMAIL = "SolicitudUseCase -> getSolicitudByRevision :  cantidad de usuarios encontrados por email = ";

    private final SolicitudRepository solicitudRepository;
    private final LoanTypesUseCase loanTypesUseCase;
    private final StateUseCase stateUseCase;
    private final Logger logger;
    private final UserGateway userGateway;

    private static final String STATE_DEFAULT = "estado-001";
    private static final String APPROVED_STATE = "estado-004";

    private static final List<String> STATES_BY_UPDATE =
            List.of("estado-001", "estado-003");

    @Override
    public Mono<Solicitud> createSolicitud(Solicitud solicitud) {
        logger.info("SolicitudUseCase -> createSolicitud : Se inicia el llamando a el caso de uso de tipo préstamo para validar los topes del tipo de solicitud : " + solicitud.getLoanTypeId());
        return loanTypesUseCase.findByLoanTypeAndValidateAmount(solicitud.getLoanTypeId(), solicitud.getAmount())
                .doOnSubscribe(sub -> logger.info(GUARDADO_DE_LA_NUEVA_SOLICITUD))
                .flatMap(exists -> {
                    Solicitud withId = solicitud.toBuilder()
                            .solicitudId(UUID.randomUUID().toString())
                            .stateId(STATE_DEFAULT)
                            .build();
                    return solicitudRepository.save(withId)
                            .doOnSubscribe(subscription -> logger.info(GUARDO_SOLICITUD_EN_LA_BD))
                            .map(saved -> saved.toBuilder()
                                    .documentId(solicitud.getDocumentId())
                                    .build()
                            );
                });
    }


    @Override
    public Flux<SolicitudRevision> getSolicitudByRevision(List<String> status, int page, int size) {
        logger.info("SolicitudUseCase -> getSolicitudByRevision : Se inicia el llamando a calcular y mostrar las solicitudes");

        int p = Math.max(0, page);
        int sz = Math.max(1, size);
        long offset = (long) p * sz;

        Mono<Long> countSolicitud = solicitudRepository.countAllForReview(status)
                .doOnNext(total -> logger.info("SolicitudUseCase -> getSolicitudByRevision : total = " + total + ", status = " + status + ", page = " + p + ", size = " + sz));

        // Valida rango ANTES de armar la página
        Flux<Solicitud> totalSolicitud = countSolicitud.flatMapMany(total -> {
            if (total == 0 || offset >= total) {
                logger.info("SolicitudUseCase -> getSolicitudByRevision : Página vacía: total=" + total + ", offset=" + offset + ",");
                return Flux.empty();
            }
            // buscar las solicitudes de ese o esos estados
            return solicitudRepository.findAllForReview(status)
                    .doOnSubscribe(s -> logger.info(BUSCANDO_SOLICITUDES_PARA_REVISION_CON_ESTADO + status))
                    .skip(offset)
                    .take(sz);
        }).cache();

        //recolecta solo los email que se mostraran
        Mono<List<String>> onlyEmailNecesary = totalSolicitud
                .map(Solicitud::getEmail)
                .distinct()
                .collectList()
                .doOnNext(set -> logger.info(CANTIDAD_DE_EMAIL + set.size()));

        Mono<Map<String, LoanTypes>> loanMapMono =
                loanTypesUseCase.findAll()
                        .collectMap(LoanTypes::getLoanTypeId, lt -> lt);

        Mono<Map<String, State>> stateMapMono =
                stateUseCase.findAll()
                        .collectMap(State::getStateId, st -> st);

        Mono<Map<String, User>> usersByEmail =
                onlyEmailNecesary.flatMap(userGateway::getUsersByEmails)
                        .doOnNext(map -> logger.info(CANTIDAD_DE_USUARIOS_ENCONTRADOS_POR_EMAIL + map.size()));

        // Deuda total por email (solo aprobadas), paso a paso
        Mono<Map<String, BigDecimal>> deudaByEmailMono =
                onlyEmailNecesary.flatMap(emails ->
                        solicitudRepository.findAllForReview(List.of(APPROVED_STATE)) // Traer todas las solicitudes aprobadas
                                .filter(s -> emails.contains(s.getEmail())) // Quedan solo con las que coinciden con el email
                                .collectList()   // se en lista
                                .map(solicitudes -> {  // Recorrer y sumar montos por email en un Map
                                    Map<String, BigDecimal> porEmail = new HashMap<>();
                                    for (Solicitud s : solicitudes) {
                                        String email = s.getEmail();
                                        BigDecimal monto = s.getAmount();
                                        porEmail.merge(email, monto, BigDecimal::add);
                                    }
                                    return porEmail;
                                })
                );

        return Mono.zip(loanMapMono, stateMapMono, usersByEmail, deudaByEmailMono)
                .flatMapMany(t -> {
                    var loanMap = t.getT1();
                    var stateMap = t.getT2();
                    var userByEmail = t.getT3();
                    var deudaByMail = t.getT4();

                    return totalSolicitud.map(s -> {
                        var email = s.getEmail();
                        var user = userByEmail.get(email);
                        var lt = loanMap.get(s.getLoanTypeId());
                        var st = stateMap.get(s.getStateId());

                        return new SolicitudRevision(
                                s.getAmount(),
                                s.getTerm(),
                                s.getEmail(),
                                (user.firstName() + " " + user.lastName()).trim(),
                                lt.getName(),
                                lt.getInterestRate(),
                                st.getName(),
                                user.baseSalary(),
                                deudaByMail.getOrDefault(email, BigDecimal.ZERO)
                        );
                    });
                });
    }

    @Override
    public Mono<Long> countByStatus(List<String> status) {
        return solicitudRepository.countAllForReview(status);
    }

    @Override
    public Mono<Decision> validateUpdateSolicitud(Decision decision) {
        logger.info("SolicitudUseCase -> validateUpdateSolicitud : inicia validación (solicitudId = " + decision.solicitudId() + ")");
        final String solicitudId = decision.solicitudId();
        return solicitudRepository.findSolicitud(solicitudId)
                .switchIfEmpty(Mono.defer(() -> {
                    logger.info("SolicitudUseCase -> validateUpdateSolicitud : No existe solicitud con ese ID");
                    return Mono.error(new ConflictException(SolicitudErrorCode.SOLICITUD_NOT_EXIST));
                }))
                .doOnNext(s -> logger.info("SolicitudUseCase -> validateUpdateSolicitud : Solicitud encontrada"))
                .flatMap(solicitudActual ->
                        solicitudRepository.solicitudHavethisstatus(solicitudActual.getSolicitudId(), STATES_BY_UPDATE)
                                .doOnNext(permitido -> logger.info(
                                        "SolicitudUseCase -> validateUpdateSolicitud : Estado actual permitido? " + permitido + " "))
                                .filter(Boolean::booleanValue)
                                .switchIfEmpty(Mono.defer(() -> {
                                    logger.info("SolicitudUseCase -> validateUpdateSolicitud : Solicitud no tiene el estado requerido");
                                    return Mono.error(new ConflictException(SolicitudErrorCode.SOLICITUD_HAVE_OTHER_STATUS));
                                }))
                                .thenReturn(solicitudActual)
                ).flatMap(solicitudActual ->
                        stateUseCase.findNameByStateId(decision.stateId())
                                .map(nombreNuevoEstado -> new Decision(
                                        solicitudActual.getSolicitudId(),
                                        decision.stateId(),
                                        nombreNuevoEstado,
                                        decision.motivo(),
                                        solicitudActual.getEmail(),
                                        "",
                                        0
                                ))
                ).doOnSuccess(d -> logger.info("SolicitudUseCase -> validateUpdateSolicitud : Validación OK"));
    }


    @Override
    public Mono<Void> updateStateOfSolicitud(Decision decision) {
        final String id = decision.solicitudId();
        final String newCodState = decision.stateId();
        final String newNameState = decision.nameStateId();

        logger.info("SolicitudUseCase -> updateStateOfSolicitud : inicia validación para ID = " + id);

        return solicitudRepository.findSolicitud(id)
                .switchIfEmpty(Mono.defer(() -> {
                    logger.info("SolicitudUseCase -> updateStateOfSolicitud : No existe solicitud con ID = " + id);
                    return Mono.empty();
                })).doOnNext(sol -> logger.info("SolicitudUseCase -> updateStateOfSolicitud : se encontro solicitud" + sol))
                .flatMap(solicitud -> {
                    final String currentCodState = solicitud.getStateId();
                    logger.info("SolicitudUseCase -> updateStateOfSolicitud : se valida si la solicitud ya tiene el estado actualizado");
                    if (currentCodState.equals(newCodState)) {
                        return stateUseCase.findNameByStateId(currentCodState)
                                .defaultIfEmpty(currentCodState)
                                .doOnNext(currentName ->
                                        logger.info("SolicitudUseCase -> updateStateOfSolicitud : Estado ya es " + currentName + " (" + currentCodState + "). No se actualiza. ID = " + id)
                                )
                                .then();
                    }

                    Mono<String> oldNameMono = stateUseCase.findNameByStateId(currentCodState)
                            .defaultIfEmpty(currentCodState);

                    Mono<String> newNameMono = Mono.just(newNameState);

                    return Mono.zip(oldNameMono, newNameMono)
                            .flatMap(tuple -> {
                                String oldName = tuple.getT1();
                                String resolvedNewName = tuple.getT2();
                                logger.info("SolicitudUseCase -> updateStateOfSolicitud : se actualiza del estado ");
                                solicitud.setStateId(newCodState);
                                return solicitudRepository.save(solicitud)
                                        .doOnSuccess(saved -> logger.info(
                                                "SolicitudUseCase -> updateStateOfSolicitud : Actualizada ID = " + saved.getSolicitudId() + " " + currentCodState + " -> " + newCodState + " ( " + oldName + " -> " + resolvedNewName + " ) "))
                                        .then();
                            });
                }).then()
                .doOnError(e -> logger.error("SolicitudUseCase -> updateStateOfSolicitud : error para ID = " + id + "  -> " + e.toString() + " "))
                .onErrorResume(e -> Mono.empty());
    }
}

