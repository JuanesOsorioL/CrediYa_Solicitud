package co.com.crediya_solicitud.model.requestRevisionItem;

import java.math.BigDecimal;

public record SolicitudRevision(BigDecimal amount,
                                int term,
                                String email,
                                String name,
                                String loanType,
                                Double interest_rate,
                                String state,
                                BigDecimal baseSalary,
                                BigDecimal total_monthly_debt_approved_applications
) {
}
