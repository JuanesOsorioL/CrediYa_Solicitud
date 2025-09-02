package co.com.crediya_solicitud.model.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class ExternalServiceException extends RuntimeException {
    private final Integer status;
    private final String customMessage;
    private final List<String> body;

    public ExternalServiceException(Integer status, String customMessage, List<String> body) {
        super(customMessage);
        this.status = status;
        this.customMessage = customMessage;
        this.body = body;
    }
}
