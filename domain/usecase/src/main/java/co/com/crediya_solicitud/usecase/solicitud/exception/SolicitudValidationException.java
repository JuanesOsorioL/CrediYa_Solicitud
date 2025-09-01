package co.com.crediya_solicitud.usecase.solicitud.exception;

import lombok.Getter;

import java.util.List;
@Getter
public class SolicitudValidationException extends  RuntimeException{
    private final List<SolicitudErrorCode> infraErrors;
    private final List<SolicitudErrorCode> domainErrors;

    public SolicitudValidationException(List<SolicitudErrorCode> infraErrors, List<SolicitudErrorCode> domainErrors) {
        super("Errores de validación de solicitud");
        this.infraErrors = infraErrors;
        this.domainErrors = domainErrors;
    }

}
