package com.skillgrow.core.infrastructure.adapter.in.rest;

import com.skillgrow.core.domain.port.in.AuthUseCase;
import com.skillgrow.core.domain.port.in.AuthUseCase.AuthResult;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AuthRequests.GoogleLoginRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AuthRequests.LoginRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AuthRequests.RegisterRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.AuthResponse;
import com.skillgrow.core.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import com.skillgrow.core.infrastructure.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Auth", description = "Registro y login; devuelven el JWT que valida APIM")
public class AuthController {

    private final AuthUseCase auth;

    public AuthController(AuthUseCase auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear cuenta con correo y contraseña")
    public AuthResponse register(@RequestBody RegisterRequest body) {
        return toResponse(auth.register(body.fullName(), body.email(), body.password()));
    }

    @PostMapping("/login")
    @Operation(summary = "Login con correo y contraseña")
    public AuthResponse login(@RequestBody LoginRequest body) {
        return toResponse(auth.login(body.email(), body.password()));
    }

    @PostMapping("/google")
    @Operation(summary = "Login con el idToken de Google")
    public AuthResponse google(@RequestBody GoogleLoginRequest body) {
        return toResponse(auth.loginWithGoogle(body.idToken()));
    }

    /** El JWT no tiene estado en el servidor: cerrar sesión es borrarlo en el cliente. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Cerrar sesión")
    public void logout(CurrentUser user) {
        // Nada que invalidar en el servidor.
    }

    private static AuthResponse toResponse(AuthResult r) {
        return new AuthResponse(r.token(), UserRestMapper.toResponse(r.profile()));
    }
}
