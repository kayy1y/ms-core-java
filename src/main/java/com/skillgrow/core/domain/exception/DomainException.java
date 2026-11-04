package com.skillgrow.core.domain.exception;

import java.util.List;

/**
 * Error de negocio. Lleva el código del contrato (VALIDATION_ERROR, DAY_UNAVAILABLE…) y, si aplica,
 * los campos con problema. El adaptador REST decide el status HTTP según el tipo.
 */
public abstract class DomainException extends RuntimeException {

    private final String code;
    private final List<FieldIssue> details;

    protected DomainException(String code, String message, List<FieldIssue> details) {
        super(message);
        this.code = code;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    protected DomainException(String code, String message) {
        this(code, message, List.of());
    }

    public String code() {
        return code;
    }

    public List<FieldIssue> details() {
        return details;
    }
}
