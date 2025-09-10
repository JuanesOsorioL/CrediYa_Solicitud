package co.com.crediya_solicitud.api.dto;

import java.math.BigDecimal;

public record SolicitudResponseDto(
        String solicitud_id,
        BigDecimal amount,
        int term,
        String email,
        String state_id,
        String loanTypeId
) {
}
