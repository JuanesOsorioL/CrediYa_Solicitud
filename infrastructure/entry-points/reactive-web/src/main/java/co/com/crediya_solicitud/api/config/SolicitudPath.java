package co.com.crediya_solicitud.api.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "routes.solicitud")
public class SolicitudPath {
    private String base;
}
