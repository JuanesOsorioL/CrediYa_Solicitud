package co.com.crediya_solicitud.api.utils;

import co.com.crediya_solicitud.api.dto.SolicitudRevisionDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
class ApiResponsTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void builderAndGettersWork() {
        var body = new SolicitudRevisionDto(
                new BigDecimal("100"), 6, "mail@test.com", "Juan",
                "Consumo", 18, "CREADA", new BigDecimal("1200"), new BigDecimal("300")
        );

        var resp = ApiRespons.<SolicitudRevisionDto>builder()
                .status(201)
                .message("Creada")
                .body(body)
                .build();

        assertThat(resp.getStatus()).isEqualTo(201);
        assertThat(resp.getMessage()).isEqualTo("Creada");
        assertThat(resp.getBody()).isSameAs(body);
    }

    @Test
    void equalsHashCodeToStringProvidedByLombokData() {
        var r1 = ApiRespons.<String>builder().status(200).message("OK").body("x").build();
        var r2 = ApiRespons.<String>builder().status(200).message("OK").body("x").build();
        var r3 = ApiRespons.<String>builder().status(400).message("Bad").body("y").build();

        assertThat(r1).isEqualTo(r2);
        assertThat(r1).hasSameHashCodeAs(r2);
        assertThat(r1).isNotEqualTo(r3);
        assertThat(r1.toString()).contains("status=200").contains("message=OK").contains("body=x");
    }

    @Test
    void jsonSerializationAndDeserializationWithGenericBody() throws Exception {
        var body = new SolicitudRevisionDto(
                new BigDecimal("999.99"), 10, "a@b.com", "María",
                "Vehículo", 16, "APROBADA", new BigDecimal("3000"), new BigDecimal("0")
        );

        var original = ApiRespons.<SolicitudRevisionDto>builder()
                .status(200).message("OK").body(body).build();

        String json = mapper.writeValueAsString(original);
        assertThat(json).contains("\"status\":200");
        assertThat(json).contains("\"message\":\"OK\"");
        assertThat(json).contains("\"email\":\"a@b.com\"");

        var type = new TypeReference<ApiRespons<SolicitudRevisionDto>>() {};
        var roundtrip = mapper.readValue(json, type);

        assertThat(roundtrip.getStatus()).isEqualTo(200);
        assertThat(roundtrip.getMessage()).isEqualTo("OK");
        assertThat(roundtrip.getBody().fullName()).isEqualTo("María");
        assertThat(roundtrip.getBody().amount()).isEqualByComparingTo("999.99");
    }
}