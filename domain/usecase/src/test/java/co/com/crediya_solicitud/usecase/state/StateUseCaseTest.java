package co.com.crediya_solicitud.usecase.state;

import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.state.State;
import co.com.crediya_solicitud.model.state.gateways.StateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

class StateUseCaseTest {
    private StateRepository stateRepository;
    private Logger logger;
    private StateUseCase useCase;

    private static final String LOG_INICIO =
            "StateUseCase -> findAll : Se inicia la consulta para traer todos los estados actuales de la BD";
    private static final String LOG_POR_ELEMENTO =
            "StateUseCase -> findAll : se consultaron todos los estados";

    @BeforeEach
    void setUp() {
        stateRepository = mock(StateRepository.class);
        logger = mock(Logger.class);
        useCase = new StateUseCase(stateRepository, logger);
    }

    @Test
    void findAll_emiteEstados_yLoggeaPorCadaElemento() {
        State s1 = mock(State.class);
        State s2 = mock(State.class);

        when(stateRepository.findAll()).thenReturn(Flux.just(s1, s2));

        StepVerifier.create(useCase.findAll())
                .expectNext(s1)
                .expectNext(s2)
                .verifyComplete();

        verify(stateRepository, times(1)).findAll();
        verify(logger, times(1)).info(LOG_INICIO);
        verify(logger, times(2)).info(LOG_POR_ELEMENTO);
        verifyNoMoreInteractions(stateRepository);
    }

    @Test
    void findAll_sinResultados_completaSinLogs() {
        when(stateRepository.findAll()).thenReturn(Flux.empty());
        StepVerifier.create(useCase.findAll()).verifyComplete();
        verify(stateRepository, times(1)).findAll();
        verify(logger, times(1)).info(LOG_INICIO);
        verify(logger, never()).info(LOG_POR_ELEMENTO);
        verifyNoMoreInteractions(stateRepository);
    }

    @Test
    void findAll_errorDelRepositorio_propagadoSinLogs() {
        when(stateRepository.findAll()).thenReturn(Flux.error(new RuntimeException("DB down")));

        StepVerifier.create(useCase.findAll())
                .expectErrorMatches(ex -> ex instanceof RuntimeException && ex.getMessage().contains("DB down"))
                .verify();

        verify(stateRepository, times(1)).findAll();
        verify(logger, times(1)).info(LOG_INICIO);
        verify(logger, never()).info(LOG_POR_ELEMENTO);
        verifyNoMoreInteractions(stateRepository);
    }
}