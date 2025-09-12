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
    private BigDecimal minimumAmount;
    private BigDecimal maximumAmount;
    private Integer interestRate;
    private Boolean automaticValidation;

    public boolean isValidAmount(BigDecimal amount) {
        return amount.compareTo(minimumAmount) >= 0 &&
                amount.compareTo(maximumAmount) <= 0;
    }
}
