package co.com.crediya_solicitud.api.utils;


import co.com.crediya_solicitud.api.dto.ApiRespons;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class ApiResponseBuilder {
    public <T> Mono<ServerResponse> build(HttpStatus status, String message, T body) {
        ApiRespons<T> response = new ApiRespons<>();
        response.setStatus(status.value());
        response.setMessage(message);
        response.setBody(body);

        return ServerResponse.status(status)
                .contentType(MediaType.parseMediaType("application/json; charset=UTF-8"))
                .bodyValue(response);
    }
}
