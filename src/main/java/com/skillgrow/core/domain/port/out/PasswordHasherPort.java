package com.skillgrow.core.domain.port.out;

/** Hash de contraseñas (BCrypt en la implementación). */
public interface PasswordHasherPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hash);
}
