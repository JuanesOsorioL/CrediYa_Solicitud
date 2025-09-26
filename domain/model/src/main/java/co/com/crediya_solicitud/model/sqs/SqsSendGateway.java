package co.com.crediya_solicitud.model.sqs;

import reactor.core.publisher.Mono;

//-> interfaz para realizar el llamado el envio a la cola de SQS de AWS
public interface SqsSendGateway {

    Mono<String> notificarCambio(Decision decision);

}
