package co.com.crediya_solicitud.api.dto;

import java.math.BigDecimal;

public record SolicitudRevisionDto(BigDecimal amount,
                                   int term,
                                   String email,
                                   String fullName,
                                   String loanTypeName,
                                   Integer interestRate,
                                   String stateName,
                                   BigDecimal baseSalary,
                                   BigDecimal debt
) {
}
