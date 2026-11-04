package com.skillgrow.core.domain.model;

/** Modalidad que ofrece un servicio: puede ser ambas. */
public enum OfferedModality implements WireEnum {
    VIRTUAL("virtual"),
    PRESENCIAL("presencial"),
    AMBAS("ambas");

    private final String value;

    OfferedModality(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    public boolean allows(Modality modality) {
        return this == AMBAS || value.equals(modality.value());
    }
}
