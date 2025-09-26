package co.com.crediya_solicitud.sqs.sender.send;

import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;
import co.com.crediya_solicitud.model.exception.specificexceptions.ConflictException;
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
    public static final String INICIA_EL_FLUJO = "SolicitudService -> notificarCambio : inicia el flujo.";
    public static final String ERROR_EN_LA_CREACION_DEL_JSON = "SolicitudService -> notificarCambio :error en la creacion del Json ";
    public static final String SQS_ENVIADO = "SolicitudService -> notificarCambio : SQS enviado: ";
    private final SQSSender sqsSender;
    private final Logger logger;

    ObjectMapper mapper = new ObjectMapper();

    public Mono<String> notificarCambio(Decision decision) {
        logger.info(INICIA_EL_FLUJO);

        String json;
        try {
            json = mapper.writeValueAsString(decision);
        } catch (Exception e) {
            logger.info(ERROR_EN_LA_CREACION_DEL_JSON + e.getMessage());
            return Mono.error(new ConflictException(SolicitudErrorCode.ERROR_CREATE_JSON));
        }

        return sqsSender.send(json)
                .doOnNext(msgId -> logger.info(SQS_ENVIADO + msgId));
    }
}