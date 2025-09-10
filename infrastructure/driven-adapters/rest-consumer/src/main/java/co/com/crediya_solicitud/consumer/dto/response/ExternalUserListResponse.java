package co.com.crediya_solicitud.consumer.dto.response;

import co.com.crediya_solicitud.consumer.dto.ExternalUserDto;

import java.util.Map;

public record ExternalUserListResponse(
        int status,
        String message,
        Map<String, ExternalUserDto> body
) {
}
