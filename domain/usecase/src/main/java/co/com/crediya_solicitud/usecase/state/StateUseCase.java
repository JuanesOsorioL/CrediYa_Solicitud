package co.com.crediya_solicitud.usecase.state;


import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.state.State;
import co.com.crediya_solicitud.model.state.gateways.StateRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class StateUseCase {

    private final StateRepository stateRepository;
    private final Logger logger;

    public Flux<State> findAll() {
        logger.info("StateUseCase -> findAll : Se inicia la consulta para traer todos los estados actuales de la BD");
        return stateRepository.findAll()
                .doOnNext(states -> logger.info("StateUseCase -> findAll : se consultaron todos los estados"));
    }

    public Mono<String> findNameByStateId(String stateId) {
        logger.info("StateUseCase -> findNameByStateId : Se inicia la consulta para traer el nombre del estado por medio del stateId = "+stateId);
        return stateRepository.name(stateId)
                .doOnNext(name -> logger.info("StateUseCase -> findNameByStateId : se encontró el nombre del estado " + name));
    }

}
