package co.com.crediya_solicitud.usecase.exception;


import co.com.crediya_solicitud.model.error.SolicitudErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SolicitudValidationExceptionTest {

    @Test
    void constructor_defaults_to_empty_lists_when_args_are_null() {
        SolicitudValidationException ex =
                new SolicitudValidationException(null, null, null);

        assertEquals("Errores de validación de solicitud", ex.getMessage());
        assertNotNull(ex.getInfraErrors());
        assertNotNull(ex.getDomainErrors());
        assertNotNull(ex.getMicroAuth());

        assertTrue(ex.getInfraErrors().isEmpty());
        assertTrue(ex.getDomainErrors().isEmpty());
        assertTrue(ex.getMicroAuth().isEmpty());
    }

    @Test
    void constructor_keeps_provided_lists_when_not_null() {
        List<SolicitudErrorCode> infra = List.of(SolicitudErrorCode.AUTH);
        List<SolicitudErrorCode> domain = List.of(SolicitudErrorCode.AMOUNT_INVALID);
        List<String> micro = List.of("auth-001", "auth-002");

        SolicitudValidationException ex =
                new SolicitudValidationException(infra, domain, micro);

        assertEquals("Errores de validación de solicitud", ex.getMessage());
        assertEquals(infra, ex.getInfraErrors());
        assertEquals(domain, ex.getDomainErrors());
        assertEquals(micro, ex.getMicroAuth());
    }

    @Test
    void can_be_caught_with_assertThrows_and_inspected() {
        SolicitudValidationException thrown = assertThrows(
                SolicitudValidationException.class,
                () -> {
                    throw new SolicitudValidationException(
                            List.of(SolicitudErrorCode.AUTH),
                            List.of(SolicitudErrorCode.LOAN_TYPE_NOT_REGISTERED),
                            List.of("missing-scope"));
                });

        assertEquals("Errores de validación de solicitud", thrown.getMessage());
        assertEquals(List.of(SolicitudErrorCode.AUTH), thrown.getInfraErrors());
        assertEquals(List.of(SolicitudErrorCode.LOAN_TYPE_NOT_REGISTERED), thrown.getDomainErrors());
        assertEquals(List.of("missing-scope"), thrown.getMicroAuth());
    }
}