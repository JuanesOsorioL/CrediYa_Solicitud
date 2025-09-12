package co.com.crediya_solicitud.model.solicitud_revision;

import java.math.BigDecimal;

public record SolicitudRevision(BigDecimal amount,
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
