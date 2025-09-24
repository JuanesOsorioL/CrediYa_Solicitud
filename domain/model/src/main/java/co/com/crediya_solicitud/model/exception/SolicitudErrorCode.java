package co.com.crediya_solicitud.model.exception;

import lombok.Getter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum SolicitudErrorCode {
    GENERIC_ERROR("USR_999", "Por favor intente luego."),
    EMAIL_INVALID("USR_003", "El correo electrónico no es válido"),
    EMAIL_EMPTY("USR_006", "El correo electrónico no puede ser vacío"),
    AMOUNT_EMPTY("USR_007", "El monto no puede ser vacío"),
    DOCUMENT_EMPTY("USR_001", "El documento de identidad no puede ser vacío"),
    TERM_EMPTY("USR_002", "El plazo no puede ser vacío"),
    ID_LOAN_TYPE_EMPTY("USR_008", "El ID tipo estado no puede estar vació"),
    ID_STATE_EMPTY("USR_004", "El ID estado no puede estar vació"),
    SOLICITUD_EMPTY("USR_023", "El ID de Solicitud no puede ser vacío"),

    AUTH("USR_009", "Error en el micro de Auth"),
    AMOUNT_INVALID("USR_010", "El valor del monto(amount), no se encuentra entre en el rango del tipo de préstamo"),
    TOKEN_INVALID("USR_015", "Token invalido"),
    TOKEN_EMPTY("USR_020", "Token no proporcionado"),
    AUTHORIZED_ONLY_CUSTOMER("USR_016", "No tiene permisos, No eres un Cliente"),
    CLAIMS_DOCUMENT_NULL("USR_017", "EL Claims o el Documento son nulos, verificar"),
    JUST_FOR_YOU("USR_018", "Solo pueda crear solicitudes de préstamo para ti mismo."),
    AUTHORIZED_ONLY_ADVISER("USR_019", "No tiene permisos, No eres un Asesor"),
    SOLICITUD_NOT_EXIST("USR_021", "No existe una solicitud con ese ID"),
    SOLICITUD_HAVE_OTHER_STATUS("USR_022", "La solicitud tiene otro estado diferente a Pendiente de revisión o Revision manual"),
    BAD_ESTATUS_ID("USR_024", "El estado debe ser 'estado-002' o 'estado-004'"),
    LOAN_TYPE_NOT_REGISTERED("USR_005", "El tipo de préstamo no está registrado");

    private final String code;
    private final String message;

    SolicitudErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    private static final Map<String, SolicitudErrorCode> CODE_MAP = Stream.of(values())
            .collect(Collectors.toMap(SolicitudErrorCode::getCode, e -> e));


    public static SolicitudErrorCode fromCode(String code) {
        return CODE_MAP.get(code);
    }
}
