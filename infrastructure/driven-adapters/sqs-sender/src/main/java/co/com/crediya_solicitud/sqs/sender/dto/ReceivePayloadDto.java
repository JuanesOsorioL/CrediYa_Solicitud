package co.com.crediya_solicitud.sqs.sender.dto;

public record ReceivePayloadDto(
        String solicitudId,
        String newState,
        String snsMessageId,
        String sourceSqsMessageId
) {}
