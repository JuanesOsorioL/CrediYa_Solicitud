package co.com.crediya_solicitud.api.exception;

import co.com.crediya_solicitud.model.exception.ErrorKind;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class DomainHttpStatusMapperTest {

    private final DomainHttpStatusMapper mapper = new DomainHttpStatusMapper();

    @ParameterizedTest
    @CsvSource({
            "BAD_REQUEST, BAD_REQUEST",
            "VALIDATION, BAD_REQUEST",
            "NOT_FOUND, NOT_FOUND",
            "UNAUTHORIZED, UNAUTHORIZED",
            "FORBIDDEN, FORBIDDEN",
            "CONFLICT, CONFLICT",
            "TECHNICAL, INTERNAL_SERVER_ERROR"
    })
    void toHttpStatus_maps_correctly(String kindName, String expectedStatus) {
        var kind = ErrorKind.valueOf(kindName);
        var status = mapper.toHttpStatus(kind);
        assertThat(status).isEqualTo(HttpStatus.valueOf(expectedStatus));
    }
}