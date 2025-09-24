package co.com.crediya_solicitud.api.mapper;

import co.com.crediya_solicitud.api.dto.*;
import co.com.crediya_solicitud.model.claims.Claismo;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import co.com.crediya_solicitud.model.solicitud_revision.SolicitudRevision;
import co.com.crediya_solicitud.model.sqs.Decision;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SolicitudDtoMapper {

    SolicitudDto toDto(Solicitud solicitud);

    Solicitud toSolicitud(SolicitudDto solicitudDto);

    SolicitudResponseDto toSolicitud(Solicitud solicitud);

    SolicitudRevisionDto toSolicitudRevision(SolicitudRevision solicitudRevision);

    ClaismoDto toClaismoDto(Claismo claismo);

    Decision toDecision(DecisionDto decisionDto);

}
