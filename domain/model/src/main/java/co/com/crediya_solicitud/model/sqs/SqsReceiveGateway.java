package co.com.crediya_solicitud.model.sqs;

import reactor.core.publisher.Mono;

public interface SqsReceiveGateway {
    Mono<Void> updateStateOfSolicitud(ReceivePayload receivePayload);

}
