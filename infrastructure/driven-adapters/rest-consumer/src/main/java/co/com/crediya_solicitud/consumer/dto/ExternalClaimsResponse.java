package co.com.crediya_solicitud.consumer.dto;

import co.com.crediya_solicitud.model.ClaimsDto;

public record ExternalClaimsResponse(
        int status,
        String message,
        ClaimsDto body
) {
}
