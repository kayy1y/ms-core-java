package com.skillgrow.core.domain.exception;

import java.util.List;

/** 400 DAY_UNAVAILABLE: el día ya no tiene cupo o no es un día de atención del mentor. */
public class DayUnavailableException extends DomainException {

    public DayUnavailableException() {
        super("DAY_UNAVAILABLE",
                "Ese día acaba de ocuparse. Elige otro de los disponibles.",
                List.of(new FieldIssue("date", "El día ya no está disponible.")));
    }
}
