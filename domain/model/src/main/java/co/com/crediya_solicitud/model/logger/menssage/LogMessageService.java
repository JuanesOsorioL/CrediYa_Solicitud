package co.com.crediya_solicitud.model.logger.menssage;

import co.com.crediya_solicitud.model.logger.Logger;

public enum LogMessageService {

    GUARDADO_DE_LA_NUEVA_SOLICITUD("SolicitudUseCase -> createSolicitud : Cumple se continua con el guardado de la nueva solicitud"),
    GUARDO_SOLICITUD_EN_LA_BD("SolicitudUseCase -> createSolicitud : Se guardo Solicitud en la BD"),
    BUSCANDO_SOLICITUDES_PARA_REVISION_CON_ESTADO("SolicitudUseCase -> getSolicitudByRevision : Buscando solicitudes para revisión. con estado = "),
    CANTIDAD_DE_EMAIL("SolicitudUseCase -> getSolicitudByRevision : cantidad de email = "),
    CANTIDAD_DE_USUARIOS_ENCONTRADOS_POR_EMAIL("SolicitudUseCase -> getSolicitudByRevision :  cantidad de usuarios encontrados por email = "),
    INICIA_EL_LLAMANDO_A_CALCULAR_Y_MOSTRAR_LAS_SOLICITUDES("SolicitudUseCase -> getSolicitudByRevision : Se inicia el llamando a calcular y mostrar las solicitudes"),
    LOS_TOPES_DEL_TIPO_DE_SOLICITUD("SolicitudUseCase -> createSolicitud : Se inicia el llamando a el caso de uso de tipo préstamo para validar los topes del tipo de solicitud : "),
    INICIA_VALIDATION_SOLICITUD_ID("SolicitudUseCase -> validateUpdateSolicitud : inicia validación solicitudId = "),
    NO_EXISTE_SOLICITUD_CON_ESE_ID("SolicitudUseCase -> validateUpdateSolicitud : No existe solicitud con ese ID"),
    SOLICITUD_ENCONTRADA("SolicitudUseCase -> validateUpdateSolicitud : Solicitud encontrada"),
    ESTADO_ACTUAL_PERMITIDO("SolicitudUseCase -> validateUpdateSolicitud : Estado actual permitido? "),
    NO_TIENE_EL_ESTADO_REQUERIDO("SolicitudUseCase -> validateUpdateSolicitud : Solicitud no tiene el estado requerido"),
    VALIDATION_OK("SolicitudUseCase -> validateUpdateSolicitud : Validación OK"),
    SOLICITUD_INICIA_VALIDATION_PARA_ID("SolicitudUseCase -> updateStateOfSolicitud : inicia validación para ID = "),
    NO_EXISTE_SOLICITUD_CON_ID("SolicitudUseCase -> updateStateOfSolicitud : No existe solicitud con ID = "),
    SE_HALLO_SOLICITUD("SolicitudUseCase -> updateStateOfSolicitud : se encontró solicitud"),
    LA_SOLICITUD_YA_TIENE_EL_ESTADO_ACTUALIZADO("SolicitudUseCase -> updateStateOfSolicitud : se valida si la solicitud ya tiene el estado actualizado"),
    SE_ACTUALIZA_DEL_ESTADO("SolicitudUseCase -> updateStateOfSolicitud : se actualiza del estado "),
    ACTUALIZADA_ID("SolicitudUseCase -> updateStateOfSolicitud : Actualizada ID = %s  %s -> %s ( %s  ->  %s )"),
    ERROR_PARA_ID("SolicitudUseCase -> updateStateOfSolicitud : error para ID = %s -> %s"),
    ESTADO_YA_ESTA("SolicitudUseCase -> updateStateOfSolicitud : Estado ya es %s ( %s ). No se actualiza. ID = %s "),
    PAGINA_LIMPIA("SolicitudUseCase -> getSolicitudByRevision : Página vacía: total = %d, offset = %d"),

    //Handler

    ERRORES_DE_JAKARTA_DEL_REQUEST("SolicitudHandler -> validarInfra : se verifican los errores de jakarta del request"),
    ERRORES_INFRAESTRUCTURALES_DETECTADOS("SolicitudHandler -> validarInfra : Errores infraestructurales detectados"),
    ES_UN_CUSTOMER_CLIENTE("SolicitudHandler -> createSolicitud : es un Customer(cliente)"),
    PETICION_PARA_CREAR_SOLICITUD_DTO_RECIBIDO("SolicitudHandler -> createSolicitud : Nueva petición para crear solicitud, Dto recibido."),
    NO_SE_ENCONTRARON_ERRORES_JAKARTA("SolicitudHandler -> createSolicitud : No se encontraron Errores jakarta"),
    TRANSFORMADO_A_DOMINIO_SOLICITUD("SolicitudHandler -> createSolicitud : transformado a dominio (Solicitud)"),
    SERVICE_CREATE_SOLICITUD("SolicitudHandler -> createSolicitud : Invocando a solicitudService.createSolicitud"),
    DTO_PARA_LA_RESPUESTA("SolicitudHandler -> createSolicitud : transformado a dto para la respuesta"),
    CREADO_EXITOSAMENTE("SolicitudHandler -> createSolicitud : Usuario creado exitosamente"),
    PAGINATION("createSolicitud -> findAll : params -> status = %s, page = %d , size = %d , offset = %d "),
    CANTIDAD_SOLICITUDES("SolicitudHandler -> findAll : cantidad de solicitudes con status = %s, total = %d "),
    INICIA_EL_FLUJO_UPDATE("SolicitudHandler -> updateSolicitud : inicia el flujo."),
    SOLICITUD_ES_UN_ASESOR("SolicitudHandler -> updateSolicitud : es un Asesor"),
    DTO_RECIBIDO("SolicitudHandler -> updateSolicitud : DTO recibido : %s "),
    VALIDATE_OK_UPDATE("SolicitudHandler -> updateSolicitud : validaciones OK"),
    SOLICITUD_ENVIADA_CORRECTAMENTE("SolicitudHandler -> updateSolicitud : Solicitud de actualización enviada exitosamente"),
    PARAMETROS_DE_PAGINACION_INVALIDOS_LA_PAGINA_SOLICITADA_ESTA_FUERA_DE_RANGO("Parámetros de paginación inválidos: la página solicitada está fuera de rango"),
    SOLICITUD_CREADA_EXITOSAMENTE("Solicitud creada exitosamente"),
    SOLICITUD_DE_ACTUALIZACION_ENVIADA_EXITOSAMENTE("Solicitud de actualización enviada exitosamente"),
    SOLICITUDES_RECUPERADAS_EXITOSAMENTE("Solicitudes recuperadas exitosamente"),
    NO_HAY_RESULTADOS_PARA_EL_ESTADO_SOLICITADO("No hay resultados para el estado solicitado"),
    INICIA_EL_FLUJO("SolicitudHandler -> createSolicitud : inicia el flujo.");

    private final String template;

    LogMessageService(String template) {
        this.template = template;
    }

    public String fmt(Object... args) {
        return String.format(template, args);
    }

    public void info(Logger logger, Object... args) {
        logger.info(fmt(args));
    }

}
