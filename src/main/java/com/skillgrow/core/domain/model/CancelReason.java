package com.skillgrow.core.domain.model;

/** Por qué terminó cancelada una sesión. */
public enum CancelReason implements WireEnum {
    REJECTED_BY_MENTOR("REJECTED_BY_MENTOR"),
    CANCELLED_BY_MENTOR("CANCELLED_BY_MENTOR"),
    CANCELLED_BY_LEARNER("CANCELLED_BY_LEARNER");

    private final String value;

    CancelReason(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
