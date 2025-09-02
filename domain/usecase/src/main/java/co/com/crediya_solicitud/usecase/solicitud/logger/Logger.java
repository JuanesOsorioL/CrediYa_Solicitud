package co.com.crediya_solicitud.usecase.solicitud.logger;

public interface Logger {
    void info(String message);

    void warn(String message);

    void warnTwo(String message, String body);

    void error(String message, Throwable exception);
}
