package co.com.crediya_solicitud.model.exception.specificexceptions;


import co.com.crediya_solicitud.model.exception.DomainException;
import co.com.crediya_solicitud.model.exception.ErrorKind;
import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;

import java.util.List;

public final class ConflictException extends DomainException {
    public ConflictException(SolicitudErrorCode code) {
        super(ErrorKind.CONFLICT, code.getCode(), code.getMessage(), List.of(code), null);
    }
}
