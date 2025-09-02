package co.com.crediya_solicitud.api.logger;


import co.com.crediya_solicitud.model.logger.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class GlobalLogger implements Logger {
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(GlobalLogger.class);

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void warnTwo(String message, String body) {
        logger.warn(message,body);
    }

    @Override
    public void error(String message, Throwable exception) {
        logger.error(message, exception);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }
}

