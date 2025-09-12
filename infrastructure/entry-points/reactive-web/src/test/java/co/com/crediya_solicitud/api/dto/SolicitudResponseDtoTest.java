package co.com.crediya_solicitud.api.dto;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;

class SolicitudResponseDtoTest {

    @Test
    void builds_and_exposes_components() {
        SolicitudResponseDto dto = new SolicitudResponseDto(
                "sol-123",
                BigDecimal.TEN,
                24,
                "mail@test.com",
                "estado-001",
                "loan-type-01"
        );

        assertThat(dto.solicitudId()).isEqualTo("sol-123");
        assertThat(dto.amount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(dto.term()).isEqualTo(24);
        assertThat(dto.email()).isEqualTo("mail@test.com");
        assertThat(dto.stateId()).isEqualTo("estado-001");
        assertThat(dto.loanTypeId()).isEqualTo("loan-type-01");
    }
}
