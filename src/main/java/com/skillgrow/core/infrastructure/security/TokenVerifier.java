package com.skillgrow.core.infrastructure.security;

import java.util.Optional;

/** Valida un JWT y devuelve el usuario; empty si es inválido o venció. */
public interface TokenVerifier {

    Optional<CurrentUser> verify(String token);
}
