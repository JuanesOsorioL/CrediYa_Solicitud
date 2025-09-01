package co.com.crediya_solicitud.api.mapper;

import co.com.crediya_solicitud.api.dto.SolicitudDto;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SolicitudDtoMapper {

    SolicitudDto toDto(Solicitud solicitud);

    Solicitud toSolicitud(SolicitudDto solicitudDto);

}
