package co.com.crediya_solicitud.api.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitudDtoTest {


    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void valid_instance_has_no_violations() {
        SolicitudDto dto = new SolicitudDto(
                "sol-123",
                BigDecimal.TEN,
                "123456789",
                12,
                "user@mail.com",
                "estado-001",
                "loan-type-01"
        );

        Set<ConstraintViolation<SolicitudDto>> violations = validator.validate(dto);
        assertThat(violations).isEmpty();
    }

    @Test
    void null_amount_triggers_USR_007() {
        SolicitudDto dto = new SolicitudDto(
                "sol-123",
                null,
                "123456789",
                12,
                "user@mail.com",
                "estado-001",
                "loan-type-01"
        );

        Set<ConstraintViolation<SolicitudDto>> violations = validator.validate(dto);
        assertThat(violations).anySatisfy(v -> {
            assertThat(v.getPropertyPath().toString()).isEqualTo("amount");
            assertThat(v.getMessage()).isEqualTo("USR_007");
        });
    }

    @Test
    void blank_document_id_triggers_USR_001() {
        SolicitudDto dto = new SolicitudDto(
                "sol-123",
                BigDecimal.TEN,
                "  ",
                12,
                "user@mail.com",
                "estado-001",
                "loan-type-01"
        );

        Set<ConstraintViolation<SolicitudDto>> violations = validator.validate(dto);
        assertThat(violations).anySatisfy(v -> {
            assertThat(v.getPropertyPath().toString()).isEqualTo("documentId");
            assertThat(v.getMessage()).isEqualTo("USR_001");
        });
    }

    @Test
    void blank_state_id_triggers_USR_004() {
        SolicitudDto dto = new SolicitudDto(
                "sol-123",
                BigDecimal.TEN,
                "123456789",
                12,
                "user@mail.com",
                "  ",
                "loan-type-01"
        );

        Set<ConstraintViolation<SolicitudDto>> violations = validator.validate(dto);
        assertThat(violations).anySatisfy(v -> {
            assertThat(v.getPropertyPath().toString()).isEqualTo("stateId");
            assertThat(v.getMessage()).isEqualTo("USR_004");
        });
    }

    @Test
    void blank_loanTypeId_triggers_USR_008() {
        SolicitudDto dto = new SolicitudDto(
                "sol-123",
                BigDecimal.TEN,
                "123456789",
                12,
                "user@mail.com",
                "estado-001",
                ""
        );

        Set<ConstraintViolation<SolicitudDto>> violations = validator.validate(dto);
        assertThat(violations).anySatisfy(v -> {
            assertThat(v.getPropertyPath().toString()).isEqualTo("loanTypeId");
            assertThat(v.getMessage()).isEqualTo("USR_008");
        });
    }

    @Test
    void multiple_violations_are_all_reported() {
        SolicitudDto dto = new SolicitudDto(
                null,
                null,
                " ",
                12,
                "user@mail.com",
                "",
                "  "
        );

        Set<ConstraintViolation<SolicitudDto>> violations = validator.validate(dto);
        assertThat(violations).hasSizeGreaterThanOrEqualTo(4);
        assertThat(violations).anySatisfy(v -> assertThat(v.getMessage()).isEqualTo("USR_007"));
        assertThat(violations).anySatisfy(v -> assertThat(v.getMessage()).isEqualTo("USR_001"));
        assertThat(violations).anySatisfy(v -> assertThat(v.getMessage()).isEqualTo("USR_004"));
        assertThat(violations).anySatisfy(v -> assertThat(v.getMessage()).isEqualTo("USR_008"));
    }
}