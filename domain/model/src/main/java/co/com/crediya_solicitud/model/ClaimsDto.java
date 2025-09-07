package co.com.crediya_solicitud.model;

public record ClaimsDto
        (
                String FistName,
                String sub,
                String Rol,
                String exp,
                String LastName,
                String Document,
                String iat,
                String jti
        ) {
}

