package co.com.crediya_solicitud.api.dto;


import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitudRevisionDtoTest {

    @Test
    void createsRecordAndExposesComponents() {
        var dto = new SolicitudRevisionDto(
                new BigDecimal("10000.50"), 12, "user@mail.com", "Jane Doe",
                "Libre inversión", 24, "APROBADA",
                new BigDecimal("2000.00"), new BigDecimal("500.00")
        );

        assertThat(dto.amount()).isEqualByComparingTo("10000.50");
        assertThat(dto.term()).isEqualTo(12);
        assertThat(dto.email()).isEqualTo("user@mail.com");
        assertThat(dto.fullName()).isEqualTo("Jane Doe");
        assertThat(dto.loanTypeName()).isEqualTo("Libre inversión");
        assertThat(dto.interestRate()).isEqualTo(24);
        assertThat(dto.stateName()).isEqualTo("APROBADA");
        assertThat(dto.baseSalary()).isEqualByComparingTo("2000.00");
        assertThat(dto.debt()).isEqualByComparingTo("500.00");
    }

    @Test
    void equalsAndHashCodeWorkOutOfTheBox() {
        var a = new SolicitudRevisionDto(
                new BigDecimal("10"), 3, "a@a.com", "Ana",
                "Tipo", 12, "OK", new BigDecimal("1"), new BigDecimal("2")
        );
        var b = new SolicitudRevisionDto(
                new BigDecimal("10"), 3, "a@a.com", "Ana",
                "Tipo", 12, "OK", new BigDecimal("1"), new BigDecimal("2")
        );
        var c = new SolicitudRevisionDto(
                new BigDecimal("11"), 3, "a@a.com", "Ana",
                "Tipo", 12, "OK", new BigDecimal("1"), new BigDecimal("2")
        );

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
    }
}