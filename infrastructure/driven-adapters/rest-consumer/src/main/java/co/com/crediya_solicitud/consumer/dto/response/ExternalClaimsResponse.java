package co.com.crediya_solicitud.consumer.dto.response;

public record ExternalClaimsResponse<T>(
        int status,
        String code,
        String message,
        T body
) {
}
