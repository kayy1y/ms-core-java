package com.skillgrow.core.domain.exception;

/** 400 INVALID_CREDENTIALS. No dice cuál de los dos datos falló, a propósito. */
public class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "El correo o la contraseña no son correctos.");
    }
}
