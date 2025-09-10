package co.com.crediya_solicitud.consumer.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class ExternalErrorResponse{
    private Integer status;
    private String message;
    private List<String> body;
}
