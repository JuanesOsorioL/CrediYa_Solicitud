package co.com.crediya_solicitud.api.logger;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class GlobalLoggerTest {

    private final GlobalLogger logger = new GlobalLogger();

    @Test
    void testInfo() {
        assertDoesNotThrow(() -> logger.info("This is an info message"));
    }

    @Test
    void testWarn() {
        assertDoesNotThrow(() -> logger.warn("This is a warning message"));
    }

    @Test
    void testWarnTwo() {
        assertDoesNotThrow(() -> logger.warnTwo("This is a warning message", "hola"));
    }

    @Test
    void testError() {
        assertDoesNotThrow(() -> logger.error("This is an error message", new RuntimeException("boom")));
    }

    @Test
    void testErrorOne() {
        assertDoesNotThrow(() -> logger.error("This is an error message"));
    }
}