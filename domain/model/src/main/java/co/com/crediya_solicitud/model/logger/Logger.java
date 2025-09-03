package co.com.crediya_solicitud.model.logger;

public interface Logger {
    void info(String message);

    void warn(String message);

    void warnTwo(String message, String body);

    void error(String message, Throwable exception);

    void error(String message);
}
