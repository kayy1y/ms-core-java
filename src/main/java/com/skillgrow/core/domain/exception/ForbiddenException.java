package com.skillgrow.core.domain.exception;

/** 403 FORBIDDEN. */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super("FORBIDDEN", message);
    }

    public ForbiddenException() {
        this("No tienes permiso para realizar esta acción.");
    }
}
