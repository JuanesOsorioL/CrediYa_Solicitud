package co.com.crediya_solicitud.sqs.sender.receive;


import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.sqs.sender.config.properties.CallbackProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.time.Duration;

@Component
@Log4j2
@RequiredArgsConstructor
public class ReceiveService {
    private final CallbackProperties callbackProps;
    private final SqsAsyncClient client;
    private final Logger logger;


    public Flux<Message> poll() {
        return Mono.defer(this::receiveOnce)
                .repeat()
                .onErrorResume(ex -> {
                    logger.error("ReceiveService -> poll : Error polling SQS", ex);
                    return Mono.delay(Duration.ofSeconds(5)).then(Mono.empty());
                })
                .flatMap(Flux::fromIterable);
    }

    private Mono<java.util.List<Message>> receiveOnce() {
        var req = ReceiveMessageRequest.builder()
                .queueUrl(callbackProps.queueUrl())
                .waitTimeSeconds(callbackProps.waitTimeSeconds())
                .maxNumberOfMessages(callbackProps.maxMessages())
                .build();

        return Mono.fromFuture(client.receiveMessage(req))
                .map(ReceiveMessageResponse::messages);
    }

    public Mono<Void> delete(String receiptHandle) {
        var del = DeleteMessageRequest.builder()
                .queueUrl(callbackProps.queueUrl())
                .receiptHandle(receiptHandle)
                .build();
        return Mono.fromFuture(client.deleteMessage(del)).then();
    }
}

