package co.com.crediya_solicitud.sqs.sender.send;

import co.com.crediya_solicitud.model.logger.Logger;
import co.com.crediya_solicitud.model.sqs.Decision;
import co.com.crediya_solicitud.model.sqs.SqsSendGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
@RequiredArgsConstructor
public class SolicitudService implements SqsSendGateway {
    private final SQSSender sqsSender;
    private final Logger logger;

    ObjectMapper mapper = new ObjectMapper();

    public Mono<String> notificarCambio(Decision decision) {
        logger.info("SolicitudService -> notificarCambio : inicia el flujo.");
//        String mensaje = """
//                Solicitud %s fue %s%s
//                Asunto: Resultado de la evaluacion del préstamo
//                Correo: %s
//                Id de mensaje: %s
//                """.formatted(
//                decision.solicitudId(),
//                decision.stateId(),
//                (decision.motivo() != null ? " - " + decision.motivo() : ""),
//                decision.email(),
//                java.util.UUID.randomUUID()
//        );

        String json;
        try {
            json = mapper.writeValueAsString(decision);
        } catch (Exception e) {
            return Mono.error(e);
        }

        return sqsSender.send(json)
                .doOnNext(msgId -> logger.info("SolicitudService -> notificarCambio : SQS enviado: " + msgId));
    }
}