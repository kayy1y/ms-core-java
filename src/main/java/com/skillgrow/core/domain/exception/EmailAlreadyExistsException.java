package com.skillgrow.core.domain.exception;

import java.util.List;

/** 400 EMAIL_ALREADY_EXISTS. */
public class EmailAlreadyExistsException extends DomainException {

    public EmailAlreadyExistsException() {
        super("EMAIL_ALREADY_EXISTS",
                "Ya existe una cuenta con ese correo.",
                List.of(new FieldIssue("email", "Este correo ya está registrado. Inicia sesión o usa otro.")));
    }
}
