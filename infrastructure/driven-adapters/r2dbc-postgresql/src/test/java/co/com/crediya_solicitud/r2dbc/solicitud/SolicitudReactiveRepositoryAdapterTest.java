package co.com.crediya_solicitud.r2dbc.solicitud;

import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.r2dbc.entities.SolicitudEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.reactivecommons.utils.ObjectMapper;

import java.math.BigDecimal;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SolicitudReactiveRepositoryAdapterTest {

    @Test
    @DisplayName("Debe mapear SolicitudEntity a Solicitud por medio de la función de mapeo")
    void shouldMapEntityToDomainUsingMappingFunction() {

        SolicitudReactiveRepository repoMock = Mockito.mock(SolicitudReactiveRepository.class);
        ObjectMapper mapperMock = Mockito.mock(ObjectMapper.class);

        SolicitudEntity entity = new SolicitudEntity();
        entity.setSolicitudId("sol123");
        entity.setAmount(BigDecimal.valueOf(2000));
        entity.setTerm(12);
        entity.setEmail("correo@test.com");
        entity.setState_id("PENDIENTE");
        entity.setLoanTypeId("type01");

        Solicitud expected = Solicitud.builder()
                .solicitud_id("sol123")
                .amount(BigDecimal.valueOf(2000))
                .term(12)
                .email("correo@test.com")
                .stateId("PENDIENTE")
                .loanTypeId("type01")
                .build();

        when(mapperMock.mapBuilder(eq(entity), eq(Solicitud.SolicitudBuilder.class)))
                .thenReturn(expected.toBuilder());

        SolicitudReactiveRepositoryAdapter adapter =
                new SolicitudReactiveRepositoryAdapter(repoMock, mapperMock);

        Function<SolicitudEntity, Solicitud> mappingFunction =
                d -> mapperMock.mapBuilder(d, Solicitud.SolicitudBuilder.class).build();

        Solicitud result = mappingFunction.apply(entity);

        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
        verify(mapperMock, times(1)).mapBuilder(eq(entity), eq(Solicitud.SolicitudBuilder.class));
    }
}