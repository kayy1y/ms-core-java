package com.skillgrow.core.domain.exception;

import java.util.List;

/** 400 VALIDATION_ERROR. */
public class ValidationException extends DomainException {

    public static final String DEFAULT_MESSAGE = "Revisa los campos marcados.";

    public ValidationException(List<FieldIssue> details, String message) {
        super("VALIDATION_ERROR", message, details);
    }

    public ValidationException(List<FieldIssue> details) {
        this(details, DEFAULT_MESSAGE);
    }

    public static ValidationException of(String field, String message) {
        return new ValidationException(List.of(new FieldIssue(field, message)));
    }

    /** Lanza la excepción si la lista tiene algún problema. */
    public static void throwIfAny(List<FieldIssue> issues) {
        if (!issues.isEmpty()) {
            throw new ValidationException(issues);
        }
    }
}
