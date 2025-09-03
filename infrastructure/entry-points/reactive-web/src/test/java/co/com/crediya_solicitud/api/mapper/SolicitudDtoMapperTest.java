package co.com.crediya_solicitud.api.mapper;

import co.com.crediya_solicitud.api.dto.SolicitudDto;
import co.com.crediya_solicitud.api.dto.SolicitudResponseDto;
import co.com.crediya_solicitud.model.solicitud.Solicitud;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitudDtoMapperTest {

    private final SolicitudDtoMapper mapper = Mappers.getMapper(SolicitudDtoMapper.class);

    @Test
    void toDto_maps_all_fields() {
        Solicitud source = Solicitud.builder()
                .solicitud_id("sol-123")
                .amount(BigDecimal.TEN)
                .document_id("123456789")
                .term(12)
                .email("mail@test.com")
                .state_id("estado-001")
                .loanTypeId("loan-type-01")
                .build();

        SolicitudDto dto = mapper.toDto(source);

        assertThat(dto.solicitud_id()).isEqualTo("sol-123");
        assertThat(dto.amount()).isEqualByComparingTo(BigDecimal.TEN);
        assertThat(dto.document_id()).isEqualTo("123456789");
        assertThat(dto.term()).isEqualTo(12);
        assertThat(dto.email()).isEqualTo("mail@test.com");
        assertThat(dto.state_id()).isEqualTo("estado-001");
        assertThat(dto.loanTypeId()).isEqualTo("loan-type-01");
    }

    @Test
    void toSolicitud_fromDto_maps_all_fields() {
        SolicitudDto dto = new SolicitudDto(
                "sol-456",
                BigDecimal.valueOf(25),
                "ABC123",
                24,
                "user@test.com",
                "estado-002",
                "loan-type-02"
        );

        Solicitud target = mapper.toSolicitud(dto);

        assertThat(target.getSolicitud_id()).isEqualTo("sol-456");
        assertThat(target.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(25));
        assertThat(target.getDocument_id()).isEqualTo("ABC123");
        assertThat(target.getTerm()).isEqualTo(24);
        assertThat(target.getEmail()).isEqualTo("user@test.com");
        assertThat(target.getState_id()).isEqualTo("estado-002");
        assertThat(target.getLoanTypeId()).isEqualTo("loan-type-02");
    }

    @Test
    void toSolicitudResponseDto_maps_all_fields() {
        Solicitud source = Solicitud.builder()
                .solicitud_id("sol-789")
                .amount(BigDecimal.valueOf(99))
                .term(36)
                .email("x@y.com")
                .state_id("estado-003")
                .loanTypeId("loan-type-03")
                .build();

        SolicitudResponseDto resp = mapper.toSolicitud(source);

        assertThat(resp.solicitud_id()).isEqualTo("sol-789");
        assertThat(resp.amount()).isEqualByComparingTo(BigDecimal.valueOf(99));
        assertThat(resp.term()).isEqualTo(36);
        assertThat(resp.email()).isEqualTo("x@y.com");
        assertThat(resp.state_id()).isEqualTo("estado-003");
        assertThat(resp.loanTypeId()).isEqualTo("loan-type-03");
    }
}