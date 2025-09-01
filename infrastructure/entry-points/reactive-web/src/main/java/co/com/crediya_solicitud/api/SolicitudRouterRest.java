package co.com.crediya_solicitud.api;

import co.com.crediya_solicitud.api.config.SolicitudPath;
import co.com.crediya_solicitud.api.exception.GlobalErrorHandler;

import co.com.crediya_solicitud.api.openapi.SolicitudOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springdoc.webflux.core.fn.SpringdocRouteBuilder.route;

@Configuration
public class SolicitudRouterRest {
    @Bean
    public RouterFunction<ServerResponse> routerFunction(SolicitudHandler handler, GlobalErrorHandler errorHandler, SolicitudPath solicitudPath) {
        return route()
                .POST(solicitudPath.getBase(), handler::createSolicitud, SolicitudOpenApi::createSolicitud)
                .build()
                .filter(errorHandler.filter());
    }
}
