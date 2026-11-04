package com.skillgrow.core.domain.port.out;

import java.util.Optional;

/** Verifica el idToken que entrega el botón de Google en la web o el móvil. */
public interface GoogleTokenVerifierPort {

    /** empty si el token no es válido, venció o no es para nuestra app. */
    Optional<GoogleIdentity> verify(String idToken);

    record GoogleIdentity(String subject, String email, String name, String pictureUrl) {}
}
