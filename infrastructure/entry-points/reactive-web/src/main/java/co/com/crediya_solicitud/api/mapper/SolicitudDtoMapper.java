package co.com.crediya_solicitud.api.mapper;

import co.com.crediya_solicitud.api.dto.ClaismoDto;
import co.com.crediya_solicitud.api.dto.SolicitudDto;
import co.com.crediya_solicitud.api.dto.SolicitudResponseDto;
import co.com.crediya_solicitud.api.dto.SolicitudRevisionDto;
import co.com.crediya_solicitud.model.claims.Claismo;
import co.com.crediya_solicitud.model.solicitud_revision.SolicitudRevision;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SolicitudDtoMapper {

    SolicitudDto toDto(Solicitud solicitud);

    Solicitud toSolicitud(SolicitudDto solicitudDto);

    SolicitudResponseDto toSolicitud(Solicitud solicitud);

    SolicitudRevisionDto toSolicitudRevision(SolicitudRevision solicitudRevision);

    ClaismoDto toClaismoDto(Claismo claismo);

}
