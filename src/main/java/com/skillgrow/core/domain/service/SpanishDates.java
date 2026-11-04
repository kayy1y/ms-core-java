package com.skillgrow.core.domain.service;

import java.time.LocalDate;

/** Fechas en texto para las notificaciones: «lunes 5 de octubre». */
public final class SpanishDates {

    private static final String[] DAYS = {"lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo"};
    private static final String[] MONTHS = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "setiembre", "octubre", "noviembre", "diciembre"};

    private SpanishDates() {}

    public static String whenText(LocalDate date) {
        return DAYS[date.getDayOfWeek().getValue() - 1] + " " + date.getDayOfMonth() + " de "
                + MONTHS[date.getMonthValue() - 1];
    }
}
