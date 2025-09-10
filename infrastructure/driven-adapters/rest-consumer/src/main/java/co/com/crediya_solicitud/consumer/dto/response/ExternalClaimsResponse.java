package co.com.crediya_solicitud.consumer.dto.response;

import co.com.crediya_solicitud.model.claims.Claims;

public record ExternalClaimsResponse(
        int status,
        String message,
        Claims body
) {
}
