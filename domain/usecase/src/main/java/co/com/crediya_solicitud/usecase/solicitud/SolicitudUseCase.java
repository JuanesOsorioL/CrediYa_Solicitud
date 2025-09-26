package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.model.exception.specificexceptions.ConflictException;
import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.logger.menssage.LogMessageService;
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

@RequiredArgsConstructor
public class SolicitudUseCase implements SolicitudService, SqsReceiveGateway {

    private final SolicitudRepository solicitudRepository;
    private final LoanTypesUseCase loanTypesUseCase;
    private final StateUseCase stateUseCase;
    private final Logger logger;
    private final UserGateway userGateway;

    private static final String PENDING_REVIEW_STATE_DEFAULT = "estado-001";
    private static final String APPROVED_STATE = "estado-004";

    private static final List<String> STATES_BY_UPDATE =
            List.of(PENDING_REVIEW_STATE_DEFAULT, "estado-003");

    @Override
    public Mono<Solicitud> createSolicitud(Solicitud solicitud) {
        LogMessageService.LOS_TOPES_DEL_TIPO_DE_SOLICITUD.info(logger, solicitud.getLoanTypeId());
        return loanTypesUseCase.findByLoanTypeAndValidateAmount(solicitud.getLoanTypeId(), solicitud.getAmount())
                .doOnSubscribe(sub -> LogMessageService.GUARDADO_DE_LA_NUEVA_SOLICITUD.info(logger))
                .flatMap(exists -> {
                    Solicitud withId = solicitud.toBuilder()
                            .stateId(PENDING_REVIEW_STATE_DEFAULT)
                            .build();
                    return solicitudRepository.save(withId)
                            .doOnSubscribe(subscription -> LogMessageService.GUARDO_SOLICITUD_EN_LA_BD.info(logger))
                            .map(saved -> saved.toBuilder()
                                    .documentId(solicitud.getDocumentId())
                                    .build()
                            );
                });
    }


    @Override
    public Flux<SolicitudRevision> getSolicitudByRevision(List<String> status, int page, int size) {

        LogMessageService.INICIA_EL_LLAMANDO_A_CALCULAR_Y_MOSTRAR_LAS_SOLICITUDES.info(logger);

        int p = Math.max(0, page);
        int sz = Math.max(1, size);
        long offset = (long) p * sz;


        return solicitudRepository.countAllForReview(status)
                .flatMapMany(total -> {
                    if (total == 0 || offset >= total) {
                        LogMessageService.PAGINA_LIMPIA.info(logger, total, offset);
                        return Flux.empty();
                    }

                    Flux<Solicitud> pageFlux = solicitudRepository.findAllForReview(status)
                            .doOnSubscribe(s -> LogMessageService.BUSCANDO_SOLICITUDES_PARA_REVISION_CON_ESTADO.info(logger, status))
                            .skip(offset)
                            .take(sz)
                            .cache();

                    Mono<List<String>> emailsMono = pageFlux.map(Solicitud::getEmail)
                            .distinct()
                            .collectList()
                            .doOnNext(list -> LogMessageService.CANTIDAD_DE_EMAIL.info(logger, list.size()));


                    Mono<Map<String, LoanTypes>> loanMapMono =
                            loanTypesUseCase.findAll()
                                    .collectMap(LoanTypes::getLoanTypeId, lt -> lt);

                    Mono<Map<String, State>> stateMapMono =
                            stateUseCase.findAll()
                                    .collectMap(State::getStateId, st -> st);

                    Mono<Map<String, User>> usersByEmail =
                            emailsMono.flatMap(userGateway::getUsersByEmails)
                                    .doOnNext(map -> LogMessageService.CANTIDAD_DE_USUARIOS_ENCONTRADOS_POR_EMAIL.info(logger, map.size()));

                    // Deuda total por email (solo aprobadas)
                    Mono<Map<String, BigDecimal>> deudaByEmailMono =
                            emailsMono.flatMap(emails ->
                                    solicitudRepository.findAllForReview(List.of(APPROVED_STATE))
                                            .filter(s -> emails.contains(s.getEmail()))
                                            .collectList()
                                            .map(solicitudes -> {
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

                                return pageFlux.map(s -> {
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
                });
    }

    @Override
    public Mono<Long> countByStatus(List<String> status) {
        return solicitudRepository.countAllForReview(status);
    }

    @Override
    public Mono<Decision> validateUpdateSolicitud(Decision decision) {
        LogMessageService.INICIA_VALIDATION_SOLICITUD_ID.info(logger, decision.solicitudId());
        final String solicitudId = decision.solicitudId();
        return solicitudRepository.findSolicitud(solicitudId)
                .switchIfEmpty(Mono.defer(() -> {
                    LogMessageService.NO_EXISTE_SOLICITUD_CON_ESE_ID.info(logger);
                    return Mono.error(new ConflictException(SolicitudErrorCode.SOLICITUD_NOT_EXIST));
                }))
                .doOnNext(s -> LogMessageService.SOLICITUD_ENCONTRADA.info(logger))
                .flatMap(solicitudActual ->
                        solicitudRepository.solicitudHavethisstatus(solicitudActual.getSolicitudId(), STATES_BY_UPDATE)//"Pendiente de revisión" "Revision manual"
                                .doOnNext(permitido -> LogMessageService.ESTADO_ACTUAL_PERMITIDO.info(logger, permitido))
                                .filter(Boolean::booleanValue)
                                .switchIfEmpty(Mono.defer(() -> {
                                    LogMessageService.NO_TIENE_EL_ESTADO_REQUERIDO.info(logger);
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
                ).doOnSuccess(d -> LogMessageService.VALIDATION_OK.info(logger));
    }


    @Override
    public Mono<Void> updateStateOfSolicitud(Decision decision) {
        final String id = decision.solicitudId();
        final String newCodState = decision.stateId();
        final String newNameState = decision.nameStateId();

        LogMessageService.SOLICITUD_INICIA_VALIDATION_PARA_ID.info(logger, id);

        return solicitudRepository.findSolicitud(id)
                .switchIfEmpty(Mono.defer(() -> {
                    LogMessageService.NO_EXISTE_SOLICITUD_CON_ID.info(logger, id);
                    return Mono.empty();
                })).doOnNext(sol -> LogMessageService.SE_HALLO_SOLICITUD.info(logger, sol))
                .flatMap(solicitud -> {
                    final String currentCodState = solicitud.getStateId();
                    LogMessageService.LA_SOLICITUD_YA_TIENE_EL_ESTADO_ACTUALIZADO.info(logger);

                    if (currentCodState.equals(newCodState)) {//comparo estados
                        return stateUseCase.findNameByStateId(currentCodState)
                                .defaultIfEmpty(currentCodState)
                                .doOnNext(currentName -> LogMessageService.ESTADO_YA_ESTA.info(logger, currentName, currentCodState, id))
                                .then();
                    }

                    Mono<String> oldNameMono = stateUseCase.findNameByStateId(currentCodState)
                            .defaultIfEmpty(currentCodState);

                    Mono<String> newNameMono = Mono.just(newNameState);

                    return Mono.zip(oldNameMono, newNameMono)
                            .flatMap(tuple -> {
                                String oldName = tuple.getT1();
                                String resolvedNewName = tuple.getT2();
                                LogMessageService.SE_ACTUALIZA_DEL_ESTADO.info(logger);

                                solicitud.setStateId(newCodState);
                                return solicitudRepository.save(solicitud)
                                        .doOnSuccess(saved ->
                                                LogMessageService.ACTUALIZADA_ID.info(logger, saved.getSolicitudId(), currentCodState, newCodState, oldName, resolvedNewName))
                                        .then();
                            });
                }).then()
                .doOnError(e -> LogMessageService.ERROR_PARA_ID.info(logger, id, e.toString()))
                .onErrorResume(e -> Mono.empty());
    }
}

