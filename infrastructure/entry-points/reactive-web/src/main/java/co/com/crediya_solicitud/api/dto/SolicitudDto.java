package co.com.crediya_solicitud.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SolicitudDto(
        String solicitud_id,
        @NotNull(message = "USR_007")
        BigDecimal amount,

        @NotBlank(message = "USR_001")
        String document_id,

        @NotNull(message = "USR_002")
        int term,

        String email,

        @NotBlank(message = "USR_004")
        String state_id,
        @NotBlank(message = "USR_008")
        String loanTypeId) {
}
