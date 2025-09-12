package co.com.crediya_solicitud.model.exception.specificexceptions;

import co.com.crediya_solicitud.model.exception.DomainException;
import co.com.crediya_solicitud.model.exception.ErrorKind;
import co.com.crediya_solicitud.model.exception.SolicitudErrorCode;

import java.util.List;

public final class BadRequestException extends DomainException {
    public BadRequestException(SolicitudErrorCode code) {
        super(ErrorKind.BAD_REQUEST, code.getCode(), code.getMessage(), List.of(code), null);
    }
}
