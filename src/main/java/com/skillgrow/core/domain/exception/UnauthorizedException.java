package com.skillgrow.core.domain.exception;

/** 401 UNAUTHORIZED: falta el token o no es válido. */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super("UNAUTHORIZED", message);
    }

    public UnauthorizedException() {
        this("Tu sesión expiró. Vuelve a iniciar sesión.");
    }
}
