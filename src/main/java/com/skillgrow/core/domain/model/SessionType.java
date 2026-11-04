package com.skillgrow.core.domain.model;

/** Individual (1 aprendiz) o grupal (hasta la capacidad del servicio). */
public enum SessionType implements WireEnum {
    INDIVIDUAL("individual"),
    GRUPAL("grupal");

    private final String value;

    SessionType(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
