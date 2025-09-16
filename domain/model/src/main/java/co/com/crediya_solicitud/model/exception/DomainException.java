package co.com.crediya_solicitud.model.exception;

import java.util.List;

public abstract class DomainException extends RuntimeException {
    private final ErrorKind kind;
    private final String code;
    private final List<SolicitudErrorCode> errors;

    protected DomainException(ErrorKind kind, String code, String message, List<SolicitudErrorCode> errors, Throwable cause) {
        super(message, cause);
        this.kind = kind;
        this.code = code;
        this.errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public ErrorKind kind() { return kind; }
    public String code() { return code; }
    public List<SolicitudErrorCode> errors() { return errors; }
}
