package com.skillgrow.core.domain.exception;

/** 400 INVALID_STATUS_TRANSITION: la acción no está permitida en el estado actual de la sesión. */
public class InvalidStatusTransitionException extends DomainException {

    public InvalidStatusTransitionException(String message) {
        super("INVALID_STATUS_TRANSITION", message);
    }

    public InvalidStatusTransitionException() {
        this("Esta acción no está disponible para el estado actual de la sesión.");
    }
}
