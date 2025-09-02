package co.com.crediya_solicitud.api;



import co.com.crediya_solicitud.api.config.SolicitudPath;
import co.com.crediya_solicitud.api.exception.GlobalErrorHandler;
import co.com.crediya_solicitud.api.utils.ApiResponseBuilder;
import co.com.crediya_solicitud.usecase.solicitud.SolicitudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class SolicitudRouterRestTest {

    private WebTestClient webTestClient;
    private SolicitudHandler handler;
    private GlobalErrorHandler errorHandler;
    private SolicitudPath solicitudPath;

    @BeforeEach
    void setUp() {

        ApiResponseBuilder apiResponseBuilder = Mockito.mock(ApiResponseBuilder.class);
        SolicitudService solicitudService = Mockito.mock(SolicitudService.class);

        handler = Mockito.mock(SolicitudHandler.class);
        errorHandler = Mockito.mock(GlobalErrorHandler.class);
        solicitudPath = Mockito.mock(SolicitudPath.class);

        when(solicitudPath.getBase()).thenReturn("/api/v1/solicitud");

        when(errorHandler.filter()).thenReturn((request, next) -> next.handle(request));


        SolicitudRouterRest routerRest = new SolicitudRouterRest();
        RouterFunction<ServerResponse> routerFunction =
                routerRest.routerFunction(handler, errorHandler, solicitudPath);

        this.webTestClient = WebTestClient.bindToRouterFunction(routerFunction).build();
    }

    @Test
    void testGETBase() {
        when(handler.findAll(any()))
                .thenReturn(Mono.just(ServerResponse.ok().bodyValue("[]").block()));

        webTestClient.get()
                .uri("/api/v1/solicitud")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo("[]");
    }

    @Test
    void testPOSTBase() {

        when(handler.createSolicitud(any()))
                .thenReturn(Mono.just(ServerResponse.status(HttpStatus.OK).build().block()));
        webTestClient.post()
                .uri("/api/v1/solicitud")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isOk();
    }
}


