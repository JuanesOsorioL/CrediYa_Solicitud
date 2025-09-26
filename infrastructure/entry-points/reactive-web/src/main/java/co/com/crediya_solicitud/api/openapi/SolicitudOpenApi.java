package co.com.crediya_solicitud.api.openapi;


import co.com.crediya_solicitud.api.dto.DecisionDto;
import co.com.crediya_solicitud.api.dto.SolicitudDto;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import lombok.experimental.UtilityClass;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.ErrorResponse;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;
import static org.springdoc.core.fn.builders.securityrequirement.Builder.securityRequirementBuilder;

@UtilityClass
public class SolicitudOpenApi {

    public Builder createSolicitud(Builder builder) {
        return builder
                .operationId("createSolicitud")
                .description("Crea un nueva solicitud en el sistema")
                .tag("Solicitud")
                .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder().mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(SolicitudDto.class))))
                .response(responseBuilder().responseCode(String.valueOf(HttpStatus.CREATED.value()))
                        .description("Solicitud creado exitosamente")
                        .content(contentBuilder().mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(SolicitudDto.class))))
                .response(responseBuilder().responseCode(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                        .description("Petición inválida")
                        .content(contentBuilder().mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(ErrorResponse.class))));
    }


    public Builder findAll(Builder builder) {
        return builder
                .operationId("findAll")
                .tag("Solicitud")
                .description("Lista solicitudes por estado y paginación")

                .parameter(parameterBuilder()
                        .in(ParameterIn.QUERY)
                        .name("status")
                        .description("Estados separados por coma, ej: estado-003,estado-001")
                        .required(false)
                        .schema(schemaBuilder().implementation(String.class))
                        .example("estado-003,estado-001"))

                // page
                .parameter(parameterBuilder()
                        .in(ParameterIn.QUERY)
                        .name("page")
                        .description("Índice de página (>= 0)")
                        .required(false)
                        .schema(schemaBuilder().implementation(Integer.class))
                        .example("0"))

                // size
                .parameter(parameterBuilder()
                        .in(ParameterIn.QUERY)
                        .name("size")
                        .description("Tamaño de página (>= 1)")
                        .required(false)
                        .schema(schemaBuilder().implementation(Integer.class))
                        .example("10"))

                .response(responseBuilder().responseCode(String.valueOf(HttpStatus.OK.value()))
                        .description("Operación exitosa")
                        .content(contentBuilder().mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(SolicitudDto.class))))
                .response(responseBuilder().responseCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                        .description("Error interno")
                        .content(contentBuilder().mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(ErrorResponse.class))))
                .security(securityRequirementBuilder().name("bearerAuth"));
    }


    public Builder updateSolicitud(Builder builder) {
        return builder
                .operationId("updateSolicitud")
                .description("Actualiza el estado de una solicitud. Envía el cambio a SQS y devuelve el id del mensaje publicado.")
                .tag("Solicitud")
                .requestBody(requestBodyBuilder()
                        .required(true)
                        .content(contentBuilder()
                                .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(DecisionDto.class))
                        )
                )
                .response(responseBuilder()
                        .responseCode(String.valueOf(HttpStatus.OK.value()))
                        .description("Solicitud de actualización enviada exitosamente")
                        .content(contentBuilder()
                                .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(String.class))
                        )
                )
                .response(responseBuilder()
                        .responseCode(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                        .description("Petición inválida")
                        .content(contentBuilder()
                                .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(ErrorResponse.class))
                        )
                )
                .response(responseBuilder()
                        .responseCode(String.valueOf(HttpStatus.CONFLICT.value()))
                        .description("Conflicto en la actualización de estado")
                        .content(contentBuilder()
                                .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(ErrorResponse.class))
                        )
                )
                .response(responseBuilder()
                        .responseCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                        .description("Error interno del servidor")
                        .content(contentBuilder()
                                .mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(ErrorResponse.class))
                        )
                )
                .security(securityRequirementBuilder().name("bearerAuth"));
    }
}