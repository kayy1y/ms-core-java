package com.skillgrow.core.domain.exception;

/** Un campo inválido: { field, message } como lo lee la web en details[]. */
public record FieldIssue(String field, String message) {}
