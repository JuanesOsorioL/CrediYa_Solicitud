package co.com.crediya_solicitud.model;

public record ClaimsDto
        (
                String FistName,               // 'iss' - El emisor del token
                String sub,              // 'sub' - El sujeto del token
                String Rol,        // 'aud' - La audiencia del token
                String exp,             // 'exp' - La fecha de expiración del token
                String LastName,
                String Document,// 'nbf' - La fecha antes de la cual el token no es válido
                String iat,               // 'iat' - La fecha en la que fue emitido el token
                String jti                    // 'jti' - El identificador único del token
        ) {
}

