package com.skillgrow.core.domain.model;

/** Rol del usuario. El rol admin habilita /admin/*. */
public enum Role implements WireEnum {
    USER("user"),
    ADMIN("admin");

    private final String value;

    Role(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
