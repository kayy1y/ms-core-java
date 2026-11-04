package com.skillgrow.core.domain.exception;

/** 409 CONFLICT: dos cambios simultáneos sobre el mismo registro. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}
