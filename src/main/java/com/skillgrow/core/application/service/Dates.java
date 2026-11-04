package com.skillgrow.core.application.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/** Días de calendario "YYYY-MM-DD" que llegan del cliente. */
final class Dates {

    private Dates() {}

    /** null si viene vacío o con otro formato. */
    static LocalDate parseOrNull(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
