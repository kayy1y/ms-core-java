package com.skillgrow.core.infrastructure.adapter.out.security;

import com.skillgrow.core.domain.port.out.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** Contraseñas con BCrypt (costo 10). Nunca se guarda la contraseña en texto plano. */
@Component
class BcryptPasswordHasher implements PasswordHasherPort {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        return hash != null && rawPassword != null && encoder.matches(rawPassword, hash);
    }
}
