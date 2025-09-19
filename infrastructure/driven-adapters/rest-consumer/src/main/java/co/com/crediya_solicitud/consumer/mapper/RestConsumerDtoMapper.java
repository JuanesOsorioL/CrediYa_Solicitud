package co.com.crediya_solicitud.consumer.mapper;


import co.com.crediya_solicitud.consumer.dto.ClaismoDto;
import co.com.crediya_solicitud.consumer.dto.ExternalUserDto;
import co.com.crediya_solicitud.model.claims.Claismo;
import co.com.crediya_solicitud.model.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RestConsumerDtoMapper {

    User toDomain(ExternalUserDto externalUserDto);

    Claismo toClaismo(ClaismoDto claismoDto);
}
