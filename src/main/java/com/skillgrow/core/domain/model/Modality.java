package com.skillgrow.core.domain.model;

/** Modalidad de una sesión concreta. */
public enum Modality implements WireEnum {
    VIRTUAL("virtual"),
    PRESENCIAL("presencial");

    private final String value;

    Modality(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
