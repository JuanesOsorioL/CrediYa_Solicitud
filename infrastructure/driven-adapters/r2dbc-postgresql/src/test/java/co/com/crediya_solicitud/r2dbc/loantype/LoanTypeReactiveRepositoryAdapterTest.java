package co.com.crediya_solicitud.r2dbc.loantype;

import co.com.crediya_solicitud.model.loantypes.LoanTypes;
import co.com.crediya_solicitud.r2dbc.entities.LoanTypeEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivecommons.utils.ObjectMapper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LoanTypeReactiveRepositoryAdapterTest {

    @Test
    @DisplayName("Debe instanciar el adapter sin errores")
    void shouldInstantiateAdapter() {
        LoanTypeReactiveRepository repoMock = Mockito.mock(LoanTypeReactiveRepository.class);
        ObjectMapper mapperMock = Mockito.mock(ObjectMapper.class);

        LoanTypeReactiveRepositoryAdapter adapter =
                new LoanTypeReactiveRepositoryAdapter(repoMock, mapperMock);

        assert adapter != null;
    }

    @Test
    @DisplayName("Debe delegar correctamente en el repositorio al buscar por loanTypeId")
    void shouldDelegateFindByLoanTypeId() {
        LoanTypeReactiveRepository repoMock = Mockito.mock(LoanTypeReactiveRepository.class);
        ObjectMapper mapperMock = Mockito.mock(ObjectMapper.class);

        LoanTypeEntity entity = new LoanTypeEntity(
                "loan01",
                "Préstamo Personal",
                BigDecimal.valueOf(1000),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(0.05),
                true
        );

        LoanTypes expectedLoanType = LoanTypes.builder()
                .loanTypeId("loan01")
                .name("Préstamo Personal")
                .minimumAmount(BigDecimal.valueOf(1000))
                .maximumAmount(BigDecimal.valueOf(5000))
                .interest_rate(0.05)
                .automatic_validation(true)
                .build();

        when(mapperMock.mapBuilder(eq(entity), eq(LoanTypes.LoanTypesBuilder.class)))
                .thenReturn(expectedLoanType.toBuilder());

        when(repoMock.findByLoanTypeId("loan01"))
                .thenReturn(Mono.just(expectedLoanType));

        LoanTypeReactiveRepositoryAdapter adapter =
                new LoanTypeReactiveRepositoryAdapter(repoMock, mapperMock);

        StepVerifier.create(adapter.findByloanTypeId("loan01"))
                .expectNextMatches(result ->
                        result.getLoanTypeId().equals("loan01")
                                && result.getName().equals("Préstamo Personal")
                                && result.getInterest_rate().equals(0.05)
                                && result.getAutomaticValidation()
                )
                .verifyComplete();

        verify(repoMock, times(1)).findByLoanTypeId("loan01");
    }
}