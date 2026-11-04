package com.skillgrow.core.domain.model;

/** Estado de la cuenta. Una cuenta suspendida no puede iniciar sesión. */
public enum AccountStatus implements WireEnum {
    ACTIVE("active"),
    SUSPENDED("suspended");

    private final String value;

    AccountStatus(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }
}
