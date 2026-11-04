package com.skillgrow.core.application.service;

import com.skillgrow.core.domain.exception.ForbiddenException;
import com.skillgrow.core.domain.exception.EmailAlreadyExistsException;
import com.skillgrow.core.domain.exception.InvalidCredentialsException;
import com.skillgrow.core.domain.exception.ValidationException;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.port.in.AuthUseCase;
import com.skillgrow.core.domain.port.out.GoogleTokenVerifierPort;
import com.skillgrow.core.domain.port.out.GoogleTokenVerifierPort.GoogleIdentity;
import com.skillgrow.core.domain.port.out.PasswordHasherPort;
import com.skillgrow.core.domain.port.out.TokenIssuerPort;
import com.skillgrow.core.domain.port.out.UserRepositoryPort;
import java.time.Clock;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Registro, login con correo y login con Google. Todos terminan emitiendo el JWT. */
@Transactional
public class AuthService implements AuthUseCase {

    private final UserRepositoryPort users;
    private final PasswordHasherPort hasher;
    private final TokenIssuerPort tokens;
    private final GoogleTokenVerifierPort google;
    private final Profiles profiles;
    private final Clock clock;

    public AuthService(UserRepositoryPort users, PasswordHasherPort hasher, TokenIssuerPort tokens,
                       GoogleTokenVerifierPort google, Profiles profiles, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.tokens = tokens;
        this.google = google;
        this.profiles = profiles;
        this.clock = clock;
    }

    @Override
    public AuthResult register(String fullName, String email, String password) {
        String cleanEmail = User.validateRegistration(fullName, email, password);
        if (users.existsByEmail(cleanEmail)) {
            throw new EmailAlreadyExistsException();
        }
        User user = users.save(User.register(UUID.randomUUID().toString(), fullName, cleanEmail,
                hasher.hash(password), null, null, clock.millis()));
        return result(user);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResult login(String email, String password) {
        User user = users.findByEmail(User.normalizeEmail(email)).orElseThrow(InvalidCredentialsException::new);
        if (!hasher.matches(password, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return result(requireActive(user));
    }

    @Override
    public AuthResult loginWithGoogle(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw ValidationException.of("idToken", "Falta el token de Google.");
        }
        GoogleIdentity id = google.verify(idToken).orElseThrow(InvalidCredentialsException::new);
        long now = clock.millis();
        String email = User.normalizeEmail(id.email());
        User user = users.findByGoogleSub(id.subject())
                .or(() -> users.findByEmail(email).map(existing -> {
                    existing.linkGoogle(id.subject(), now);
                    return users.save(existing);
                }))
                .orElseGet(() -> users.save(User.register(UUID.randomUUID().toString(),
                        id.name() != null && !id.name().isBlank() ? id.name() : email.split("@")[0],
                        email, null, id.subject(), id.pictureUrl(), now)));
        return result(requireActive(user));
    }

    private static User requireActive(User user) {
        if (!user.isActive()) {
            throw new ForbiddenException("Tu cuenta está suspendida. Contacta al equipo de SkillGrow.");
        }
        return user;
    }

    private AuthResult result(User user) {
        return new AuthResult(tokens.issue(user), profiles.of(user));
    }
}
