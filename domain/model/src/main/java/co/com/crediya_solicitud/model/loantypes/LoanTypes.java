package co.com.crediya_solicitud.model.loantypes;

import lombok.*;

import java.math.BigDecimal;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class LoanTypes {
    private String loanTypeId;
    private String name;
    private BigDecimal minimum_amount;
    private BigDecimal maximum_amount;
    private Double interest_rate;
    private Boolean automatic_validation;

    public boolean isValidAmount(BigDecimal amount) {
        return amount.compareTo(minimum_amount) >= 0 &&
                amount.compareTo(maximum_amount) <= 0;
    }
}
