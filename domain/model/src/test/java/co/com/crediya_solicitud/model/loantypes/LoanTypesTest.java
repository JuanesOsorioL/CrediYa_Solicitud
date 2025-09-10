package co.com.crediya_solicitud.model.loantypes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LoanTypesTest {

    @Test
    @DisplayName("Debe retornar true cuando el monto está dentro del rango")
    void shouldReturnTrueWhenAmountIsWithinRange() {
        LoanTypes loanType = LoanTypes.builder()
                .loanTypeId("1")
                .name("Personal Loan")
                .minimumAmount(BigDecimal.valueOf(1000))
                .maximumAmount(BigDecimal.valueOf(5000))
                .interest_rate(0.05)
                .automatic_validation(true)
                .build();

        boolean result = loanType.isValidAmount(BigDecimal.valueOf(3000));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Debe retornar true cuando el monto es exactamente igual al mínimo")
    void shouldReturnTrueWhenAmountEqualsMinimum() {
        LoanTypes loanType = LoanTypes.builder()
                .minimumAmount(BigDecimal.valueOf(1000))
                .maximumAmount(BigDecimal.valueOf(5000))
                .build();

        boolean result = loanType.isValidAmount(BigDecimal.valueOf(1000));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Debe retornar true cuando el monto es exactamente igual al máximo")
    void shouldReturnTrueWhenAmountEqualsMaximum() {
        LoanTypes loanType = LoanTypes.builder()
                .minimumAmount(BigDecimal.valueOf(1000))
                .maximumAmount(BigDecimal.valueOf(5000))
                .build();

        boolean result = loanType.isValidAmount(BigDecimal.valueOf(5000));

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("Debe retornar false cuando el monto es menor que el mínimo")
    void shouldReturnFalseWhenAmountIsLessThanMinimum() {
        LoanTypes loanType = LoanTypes.builder()
                .minimumAmount(BigDecimal.valueOf(1000))
                .maximumAmount(BigDecimal.valueOf(5000))
                .build();

        boolean result = loanType.isValidAmount(BigDecimal.valueOf(999));

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Debe retornar false cuando el monto es mayor que el máximo")
    void shouldReturnFalseWhenAmountIsGreaterThanMaximum() {
        LoanTypes loanType = LoanTypes.builder()
                .minimumAmount(BigDecimal.valueOf(1000))
                .maximumAmount(BigDecimal.valueOf(5000))
                .build();

        boolean result = loanType.isValidAmount(BigDecimal.valueOf(6000));

        assertThat(result).isFalse();
    }
}