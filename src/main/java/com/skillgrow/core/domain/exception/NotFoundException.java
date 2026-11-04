package com.skillgrow.core.domain.exception;

/** 404 NOT_FOUND. {@code what} va con artículo: "La sesión", "El usuario". */
public class NotFoundException extends DomainException {

    public NotFoundException(String what) {
        super("NOT_FOUND", what + " no existe o ya no está disponible.");
    }
}
