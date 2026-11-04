package com.skillgrow.core.domain.model;

/** Nivel con el que un mentor enseña una habilidad. */
public enum SkillLevel implements WireEnum {
    BASICO("basico"),
    INTERMEDIO("intermedio"),
    AVANZADO("avanzado");

    private final String value;

    SkillLevel(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
