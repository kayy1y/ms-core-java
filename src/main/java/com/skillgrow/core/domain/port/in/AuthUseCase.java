package com.skillgrow.core.domain.port.in;

import com.skillgrow.core.domain.model.UserProfile;

/** Registro y login. Siempre devuelve el JWT junto con el usuario. */
public interface AuthUseCase {

    AuthResult register(String fullName, String email, String password);

    AuthResult login(String email, String password);

    AuthResult loginWithGoogle(String idToken);

    record AuthResult(String token, UserProfile profile) {}
}
