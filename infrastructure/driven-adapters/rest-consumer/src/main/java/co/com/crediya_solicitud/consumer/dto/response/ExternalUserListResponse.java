package co.com.crediya_solicitud.consumer.dto.response;

public record ExternalUserListResponse(
        int status,
        String message,
        UsersByEmailResponse body
) {
}
