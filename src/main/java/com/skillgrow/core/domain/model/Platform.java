package com.skillgrow.core.domain.model;

/** Plataforma de la reunión virtual. */
public enum Platform implements WireEnum {
    GOOGLE_MEET("google_meet"),
    ZOOM("zoom"),
    TEAMS("teams"),
    OTRA("otra");

    private final String value;

    Platform(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
