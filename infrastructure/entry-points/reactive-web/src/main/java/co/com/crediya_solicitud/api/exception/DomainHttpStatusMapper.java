package co.com.crediya_solicitud.api.exception;


import co.com.crediya_solicitud.model.exception.ErrorKind;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class DomainHttpStatusMapper {
    public HttpStatus toHttpStatus(ErrorKind kind) {
        return switch (kind) {
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case VALIDATION -> HttpStatus.BAD_REQUEST;       // 400
            case NOT_FOUND -> HttpStatus.NOT_FOUND;          // 404
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;    // 401
            case FORBIDDEN -> HttpStatus.FORBIDDEN;          // 403
            case CONFLICT -> HttpStatus.CONFLICT;            // 409
            case TECHNICAL -> HttpStatus.INTERNAL_SERVER_ERROR; //  502/504
        };
    }
}
