package co.com.crediya_solicitud.api.dto;

import java.math.BigDecimal;

public record SolicitudRevisionDto(BigDecimal amount,
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
