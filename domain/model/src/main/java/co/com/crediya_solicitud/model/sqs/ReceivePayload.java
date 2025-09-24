package co.com.crediya_solicitud.model.sqs;

public record ReceivePayload(
        String solicitudId,
        String newState,
        String snsMessageId,
        String sourceSqsMessageId
) {}
