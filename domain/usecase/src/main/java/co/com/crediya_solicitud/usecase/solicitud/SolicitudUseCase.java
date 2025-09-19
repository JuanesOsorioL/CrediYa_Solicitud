package co.com.crediya_solicitud.usecase.solicitud;

import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.solicitud_revision.SolicitudRevision;
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
public class SolicitudUseCase implements SolicitudService {

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

}
