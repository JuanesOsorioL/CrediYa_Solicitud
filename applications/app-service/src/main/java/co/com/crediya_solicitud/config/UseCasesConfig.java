package co.com.crediya_solicitud.config;

import co.com.crediya_solicitud.model.solicitud.gateways.UserGateway;
import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya_solicitud.usecase.loantypes.LoanTypesUseCase;
import co.com.crediya_solicitud.usecase.solicitud.SolicitudUseCase;
import co.com.crediya_solicitud.usecase.state.StateUseCase;
import org.springframework.context.annotation.*;

@Configuration
@ComponentScan(basePackages = "co.com.crediya_solicitud.usecase",
//        includeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = { LoanTypesUseCase.class, StateUseCase.class })
//        },
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class UseCasesConfig {
    @Bean
    @Primary
    public SolicitudUseCase solicitudUseCase(SolicitudRepository solicitudRepository, LoanTypesUseCase loanTypesUseCase, StateUseCase stateUseCase, Logger logger, UserGateway userGateway) {
        return new SolicitudUseCase(solicitudRepository, loanTypesUseCase, stateUseCase, logger, userGateway);
    }

}
