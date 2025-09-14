package co.com.crediya_solicitud.consumer.dto.response;

import co.com.crediya_solicitud.consumer.dto.ExternalUserDto;

import java.util.Map;

public record UsersByEmailResponse(Map<String, ExternalUserDto> users) {


}
