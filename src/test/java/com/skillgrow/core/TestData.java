package com.skillgrow.core;

import com.skillgrow.core.domain.model.AccountStatus;
import com.skillgrow.core.domain.model.Role;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.infrastructure.config.AppProperties;
import java.util.List;
import java.util.Map;

/** Objetos de prueba para los tests de aplicación e infraestructura. */
public final class TestData {

    /** Clave base64 de 48 bytes, solo para tests. */
    public static final String JWT_SECRET = "dGVzdC1zZWNyZXQtcGFyYS1qd3QtZGUtbXMtY29yZS0xMjM0NTY3ODkwYWJjZGVm";

    private TestData() {}

    public static User user(String id, String email, Role role, AccountStatus status) {
        return new User(id, "Valeria Rojas", email, "$2a$10$hash", null, null, "", role, status, null, null,
                List.of(), List.of(), List.of(), Map.of(), 1_000L, 1_000L);
    }

    public static AppProperties props(boolean certCheck, String thumbprint) {
        return new AppProperties("America/Costa_Rica",
                new AppProperties.Jwt(JWT_SECRET, 8, "skillgrow-core"),
                new AppProperties.Google("", false, "aprendiz@skillgrow.test"),
                new AppProperties.Events(false, "", "notifications"),
                new AppProperties.Apim(certCheck, thumbprint),
                new AppProperties.Catalog("stub", "", "", 3000));
    }
}
