package co.com.crediya_solicitud.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.Mockito.mock;

public class UseCasesConfigTest {

    @Test
    void testUseCaseBeansExist() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class)) {
            String[] beanNames = context.getBeanDefinitionNames();

            boolean useCaseBeanFound = false;
            for (String beanName : beanNames) {
                if (beanName.endsWith("UseCase")) {
                    useCaseBeanFound = true;
                    break;
                }
            }

            assertTrue(useCaseBeanFound, "No beans ending with 'Use Case' were found");
        }
    }

    @Configuration
    @Import(UseCasesConfig.class)
    static class TestConfig {

        @Bean
        public MyUseCase myUseCase() {
            return new MyUseCase();
        }

        @Bean
        co.com.crediya_solicitud.model.loantypes.gateways.LoanTypesRepository loanTypesRepository() {
            return mock(co.com.crediya_solicitud.model.loantypes.gateways.LoanTypesRepository.class);
        }

        @Bean
        co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository solicitudRepository() {
            return mock(co.com.crediya_solicitud.model.solicitud.gateways.SolicitudRepository.class);
        }

        @Bean
        co.com.crediya_solicitud.model.logger.Logger logger() {
            return mock(co.com.crediya_solicitud.model.logger.Logger.class);
        }

        @Bean
        co.com.crediya_solicitud.model.UserGateway userGateway() {
            return mock(co.com.crediya_solicitud.model.UserGateway.class);
        }


    }

    static class MyUseCase {
        public String execute() {
            return "MyUseCase Test";
        }
    }
}