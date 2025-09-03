package co.com.crediya_solicitud.r2dbc.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PostgresqlConnectionPropertiesTest {
    @Test
    void mustHoldPropertiesCorrectly() {
        PostgresqlConnectionProperties properties = new PostgresqlConnectionProperties(
                "localhost",
                5432,
                "my_db",
                "public",
                "user",
                "secret"
        );

        assertThat(properties.host()).isEqualTo("localhost");
        assertThat(properties.port()).isEqualTo(5432);
        assertThat(properties.database()).isEqualTo("my_db");
        assertThat(properties.schema()).isEqualTo("public");
        assertThat(properties.username()).isEqualTo("user");
        assertThat(properties.password()).isEqualTo("secret");
    }
}