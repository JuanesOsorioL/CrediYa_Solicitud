package co.com.crediya_solicitud.api.openapi;


import co.com.crediya_solicitud.api.dto.SolicitudDto;
import lombok.experimental.UtilityClass;
import org.springdoc.core.fn.builders.operation.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.ErrorResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.content.Builder.contentBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;
import static org.springdoc.core.fn.builders.schema.Builder.schemaBuilder;

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

/*
    public Builder findAll(Builder builder) {
        return builder
                .operationId("findAll")
                .description("Obtiene todos los usuarios registrados")
                .tag("User")
                .response(responseBuilder().responseCode(String.valueOf(HttpStatus.OK.value()))
                        .description("Operación exitosa")
                        .content(contentBuilder().mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(User.class))))
                .response(responseBuilder().responseCode(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                        .description("Error interno")
                        .content(contentBuilder().mediaType(MediaType.APPLICATION_JSON_VALUE)
                                .schema(schemaBuilder().implementation(ErrorResponse.class))));
    }*/
}