package co.com.crediya_solicitud.usecase.solicitud.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class SolicitudValidationException extends RuntimeException {
    private final List<SolicitudErrorCode> infraErrors;
    private final List<SolicitudErrorCode> domainErrors;
   private final List<String> microAuth;

    public SolicitudValidationException(List<SolicitudErrorCode> infraErrors, List<SolicitudErrorCode> domainErrors, List<String> microAuth) {
        super("Errores de validación de solicitud");

        this.infraErrors = infraErrors == null ? List.of() : infraErrors;
        this.domainErrors = domainErrors == null ? List.of() : domainErrors;
        this.microAuth = microAuth == null ? List.of() : microAuth;

    }

}
