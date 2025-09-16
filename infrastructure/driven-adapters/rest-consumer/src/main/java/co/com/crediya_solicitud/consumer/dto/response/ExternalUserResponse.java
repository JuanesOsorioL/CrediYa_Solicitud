package co.com.crediya_solicitud.consumer.dto.response;

import co.com.crediya_solicitud.consumer.dto.ExternalUserDto;
import lombok.Data;

@Data
public class ExternalUserResponse {
    private int status;
    private String message;
    private ExternalUserDto body;
}
