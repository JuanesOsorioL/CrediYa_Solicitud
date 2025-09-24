package co.com.crediya_solicitud.model.sqs;

public record Decision(
        String solicitudId,
        String stateId,
        String nameStateId,
        String motivo,
        String email,
        String error,
        int codError
) {
}
