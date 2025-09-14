package co.com.crediya_solicitud.api;


import co.com.crediya_solicitud.api.config.SolicitudPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SolicitudRouterRestTest {

    private WebTestClient webTestClient;
    private SolicitudHandler handler;
    private SolicitudPath solicitudPath;

    @BeforeEach
    void setUp() {
        handler = mock(SolicitudHandler.class);
        solicitudPath = mock(SolicitudPath.class);

        when(solicitudPath.getBase()).thenReturn("/api/v1/solicitud");

        RouterFunction<ServerResponse> routerFunction =
                new SolicitudRouterRest().routerFunction(handler, solicitudPath);

        webTestClient = WebTestClient.bindToRouterFunction(routerFunction).build();
    }

    @Test
    void testGETBase_maps_to_findAll_and_returns_body() {
        when(handler.findAll(any())).thenReturn(ServerResponse.ok().bodyValue("[]"));

        webTestClient.get()
                .uri("/api/v1/solicitud")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo("[]");

        verify(handler, times(1)).findAll(any());
        verifyNoMoreInteractions(handler);
    }

    @Test
    void testPOSTBase_maps_to_createSolicitud_and_returns_status() {
        when(handler.createSolicitud(any()))
                .thenReturn(ServerResponse.status(HttpStatus.CREATED).build());

        webTestClient.post()
                .uri("/api/v1/solicitud")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isCreated();

        verify(handler, times(1)).createSolicitud(any());
        verifyNoMoreInteractions(handler);
    }
}


