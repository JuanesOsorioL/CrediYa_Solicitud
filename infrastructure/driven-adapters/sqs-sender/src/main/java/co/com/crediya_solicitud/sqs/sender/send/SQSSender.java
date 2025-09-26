package co.com.crediya_solicitud.sqs.sender.send;


import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.sqs.sender.config.properties.NotifierProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

@Service
@RequiredArgsConstructor
public class SQSSender {
    private final NotifierProperties properties;
    private final SqsAsyncClient client;
    private final Logger logger;

    public Mono<String> send(String message) {
        return Mono.fromCallable(() -> buildRequest(message))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> logger.info("SQSSender -> send : Message sent " + response.messageId()))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }
}
