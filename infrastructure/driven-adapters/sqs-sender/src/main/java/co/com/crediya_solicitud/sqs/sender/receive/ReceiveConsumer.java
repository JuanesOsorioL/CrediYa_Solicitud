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
        final String receipt = msg.receiptHandle();
        logger.info("ReceiveConsumer -> processMessage : recibido id = "+msg.messageId()+" body = "+msg.body()+" ");

        return Mono.fromCallable(() -> mapper.readValue(msg.body(), Decision.class))
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .doOnNext(d -> logger.info("Decision deserializada : solicitudId = "+d.solicitudId()+", stateId = "+d.stateId()+", email = "+d.email()+" "))

                .flatMap(d -> {
                    boolean tieneError = (d.error() != null && !d.error().isBlank()) || d.codError() != 0;
                    if (tieneError) {
                        logger.warn("Decision llega con error upstream: error = "+d.error()+", codError "+d.codError()+" ");

                        return callbackService.delete(receipt)
                                .doOnSuccess(v -> logger.info("Mensaje borrado (upstream con error). id= "+msg.messageId()+" "))
                                .then();
                    }

                    return sqsReceiveGateway.updateStateOfSolicitud(d)
                            .then(callbackService.delete(receipt))
                            .doOnSuccess(v -> logger.info("Mensaje borrado tras actualizar. id = "+msg.messageId()+" "));
                })

                .onErrorResume(e -> {
                    logger.error("Fallo procesando mensaje id = "+msg.messageId()+" . Se dejará para reintento." + e);
                    return Mono.empty();
                });
    }





//
//    private Mono<Void> processMessage(Message msg) {
//        logger.info("ReceiveConsumer -> processMessage : Se inicia el proceso de captura del mensaje");
//        final String receipt = msg.receiptHandle();
//        logger.info("ReceiveConsumer -> processMessage : Mensaje recibido: id = " + msg.messageId() + " , body = " + msg.body() + " , mensaje completo = " + msg.toString());
//
//        try {
//            Decision decision = mapper.readValue(msg.body(), Decision.class);
//            logger.info("Decision recibida -> processMessage = se deserializa el mensaje a Decision " + decision.solicitudId() + ", stateId=" + decision.stateId() + ", email= " + decision.email());
//            Mono<Void> hola=updateStateOfSolicitud(decision);
//
//
//        } catch (Exception e) {
//            logger.error("ReceiveConsumer -> processMessage : Error deserializa mensaje SQS a Decision", e);
//        }
//
//
//        return callbackService.delete(receipt)
//                .doOnSuccess(v -> logger.info("ReceiveConsumer -> processMessage : SQS (mensaje borrado). id = " + msg.messageId()))
//                .onErrorResume(ex -> {
//                    logger.error("ReceiveConsumer -> processMessage : No se pudo borrar el mensaje. id = " + msg.messageId(), ex);
//                    return Mono.empty();
//                });
//    }

}
