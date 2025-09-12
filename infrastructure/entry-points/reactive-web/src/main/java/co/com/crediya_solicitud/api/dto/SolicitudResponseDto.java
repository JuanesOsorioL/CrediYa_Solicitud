package co.com.crediya_solicitud.api.dto;

import java.math.BigDecimal;

public record SolicitudResponseDto(
        String solicitudId,
        BigDecimal amount,
        int term,
        String email,
        String stateId,
        String loanTypeId
) {
}
