package co.com.crediya_solicitud.usecase.state;


import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.state.State;
import co.com.crediya_solicitud.model.state.gateways.StateRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
public class StateUseCase {

    private final StateRepository stateRepository;
    private final Logger logger;

    public Flux<State> findAll() {
        return stateRepository.findAll().doOnNext(stateEntity -> logger.info("StateUseCase -> findAll() : se llama a findAll del repositorio"));
    }


}
