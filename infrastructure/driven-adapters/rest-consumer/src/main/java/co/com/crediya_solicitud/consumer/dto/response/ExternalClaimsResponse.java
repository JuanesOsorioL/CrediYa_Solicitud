package co.com.crediya_solicitud.consumer.dto.response;

import co.com.crediya_solicitud.consumer.dto.ClaismoDto;

public record ExternalClaimsResponse(
        int status,
        String code,
        String message,
        ClaismoDto body
) {
}
