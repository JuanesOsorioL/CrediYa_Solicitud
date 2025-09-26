package co.com.crediya_solicitud.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DecisionDto(
        @NotBlank(message = "USR_023")
        String solicitudId,
        @NotBlank(message = "USR_004")
        @Pattern(regexp = "estado-002|estado-004",
                message = "USR_024")
        String stateId,
        String nameStateId,
        String motivo,
        String email,
        String error,
        int codError
) {
}
