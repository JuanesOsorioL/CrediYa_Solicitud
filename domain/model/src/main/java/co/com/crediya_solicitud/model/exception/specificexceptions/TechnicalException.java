package co.com.crediya_solicitud.model.exception.specificexceptions;


import co.com.crediya_solicitud.model.exception.DomainException;
import co.com.crediya_solicitud.model.exception.ErrorKind;
import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;

import java.util.List;

public final class TechnicalException extends DomainException {
    public TechnicalException(String message, Throwable cause) {
        super(ErrorKind.TECHNICAL, SolicitudErrorCode.GENERIC_ERROR.getCode(), message, List.of(SolicitudErrorCode.GENERIC_ERROR), cause);
    }
}
