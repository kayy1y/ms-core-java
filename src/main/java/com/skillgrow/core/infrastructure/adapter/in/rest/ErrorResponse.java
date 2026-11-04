package com.skillgrow.core.infrastructure.adapter.in.rest;

import com.skillgrow.core.domain.exception.FieldIssue;
import java.util.List;

/**
 * Formato de error estándar. Une el del doc 03 (code, message, details, timestampEpochMs, path) con lo
 * que ya lee la web de la Fase I (status y details[].message).
 */
public record ErrorResponse(int status, String code, String message, List<FieldIssue> details,
                            long timestampEpochMs, String path) {}
