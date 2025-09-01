package co.com.crediya_solicitud.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Auth for CrediYa.",
                version = "1.0",
                description = "Swagger documentation using OpenAPI."
        )
)
public class SwaggerConfig {
}
