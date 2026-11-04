package com.skillgrow.core.domain.model;

/** Criterio con el que se gana una insignia. rating_average depende de las reseñas (MS2). */
public enum BadgeCriterion implements WireEnum {
    FIRST_SESSION("first_session"),
    COMPLETED_SESSIONS("completed_sessions"),
    STUDENTS_HELPED("students_helped"),
    RATING_AVERAGE("rating_average"),
    ACTIVE_WEEKS("active_weeks");

    private final String value;

    BadgeCriterion(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
