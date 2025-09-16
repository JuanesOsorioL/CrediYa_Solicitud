package co.com.crediya_solicitud.model.exception.specificexceptions;


import co.com.crediya_solicitud.model.exception.DomainException;
import co.com.crediya_solicitud.model.exception.ErrorKind;
import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;

import java.util.List;

public final class NotFoundException extends DomainException {
    public NotFoundException(SolicitudErrorCode code) {
        super(ErrorKind.NOT_FOUND, code.getCode(), code.getMessage(), List.of(code), null);
    }
}
