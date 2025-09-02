package co.com.crediya_solicitud.consumer.dto;

import co.com.crediya_solicitud.consumer.UserResponseBody;
import lombok.Data;

@Data
public class ExternalUserResponse {
    private int status;
    private String message;
    private UserResponseBody body;
}
