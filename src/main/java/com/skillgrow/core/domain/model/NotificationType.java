package com.skillgrow.core.domain.model;

/** Tipos de notificación del contrato. Es el eventType que viaja a la cola. */
public enum NotificationType implements WireEnum {
    SESSION_REQUESTED("SESSION_REQUESTED"),
    SESSION_ACCEPTED("SESSION_ACCEPTED"),
    SESSION_REJECTED("SESSION_REJECTED"),
    SESSION_RESCHEDULED("SESSION_RESCHEDULED"),
    SESSION_CANCELLED("SESSION_CANCELLED"),
    SESSION_COMPLETED("SESSION_COMPLETED"),
    SESSION_REMINDER("SESSION_REMINDER"),
    REVIEW_REQUESTED("REVIEW_REQUESTED"),
    BADGE_EARNED("BADGE_EARNED"),
    REPORT_UPDATED("REPORT_UPDATED");

    private final String value;

    NotificationType(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
