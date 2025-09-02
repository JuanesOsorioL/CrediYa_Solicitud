package co.com.crediya_solicitud.config;

import co.com.crediya_solicitud.model.UserGateway;
import co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository;
import co.com.crediya_solicitud.usecase.loantypes.LoanTypesUseCase;
import co.com.crediya_solicitud.usecase.solicitud.SolicitudUseCase;
import co.com.crediya_solicitud.usecase.solicitud.logger.Logger;
import org.springframework.context.annotation.*;

@Configuration
@ComponentScan(basePackages = "co.com.crediya_solicitud.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class UseCasesConfig {
        @Bean
        @Primary
        public SolicitudUseCase solicitudUseCase(SolicitudRepository solicitudRepository, LoanTypesUseCase loanTypesUseCase, Logger logger, UserGateway userGateway) {
                return new SolicitudUseCase(solicitudRepository,loanTypesUseCase, logger,userGateway);
        }

}
