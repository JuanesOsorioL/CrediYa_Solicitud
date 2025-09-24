package co.com.crediya_solicitud.sqs.sender.receive;

import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.sqs.Decision;
import co.com.crediya_solicitud.model.sqs.SqsReceiveGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

@Component
@RequiredArgsConstructor
public class ReceiveConsumer {

    private final ReceiveService callbackService;
    private final Logger logger;
    private final SqsReceiveGateway sqsReceiveGateway;
    private Disposable subscription;

    ObjectMapper mapper = new ObjectMapper();

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        this.subscription = callbackService.poll()
                .flatMap(this::processMessage, 4)
                .doOnSubscribe(s -> logger.info("ReceiveConsumer -> start : ReceiveConsumer iniciado"))
                .doOnError(e -> logger.error("ReceiveConsumer -> start :  Error en stream de callback", e))
                .subscribe();
    }

    private Mono<Void> processMessage(Message msg) {
        logger.info("ReceiveConsumer -> processMessage : inicia el proceso de capturacion del mensaje");
        final String receipt = msg.receiptHandle();

        try {
            // 👇 deserializa el JSON del body a tu record Decision
            Decision d = mapper.readValue(msg.body(), Decision.class);

            // ahora puedes usarlo normalmente
            logger.info("Decision recibida -> solicitudId= " + d.solicitudId() + ", stateId=" + d.stateId() + ", email= " + d.email());

            // aquí puedes llamar tu servicio de dominio:
            // sqsReceiveGateway.updateStateOfSolicitud(d);

            // sqsReceiveGateway.updateStateOfSolicitud(msg.body())
            logger.info("ReceiveConsumer -> processMessage : Mensaje recibido: id = " + msg.messageId() + " , body = " + msg.body() + " , mensaje completo = " + msg.toString());


        } catch (Exception e) {
            logger.error("Error deserializando mensaje SQS a Decision", e);
            // opcional: decidir si borras el mensaje o lo dejas para reintento
        }


        return callbackService.delete(receipt)
                .doOnSuccess(v -> logger.info("ReceiveConsumer -> processMessage : SQS (mensaje borrado). id = " + msg.messageId()))

                .onErrorResume(ex -> {
                    logger.error("ReceiveConsumer -> processMessage : No se pudo borrar el mensaje. id = " + msg.messageId(), ex);
                    return Mono.empty();
                });
    }
}
