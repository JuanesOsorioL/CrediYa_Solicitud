package co.com.crediya_solicitud.model.exception.specificexceptions;


import co.com.crediya_solicitud.model.exception.DomainException;
import co.com.crediya_solicitud.model.exception.ErrorKind;
import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;

import java.util.List;

public final class ForbiddenException extends DomainException {
    public ForbiddenException(SolicitudErrorCode code) {
        super(ErrorKind.FORBIDDEN, code.getCode(), code.getMessage(), List.of(code), null);
    }
}
