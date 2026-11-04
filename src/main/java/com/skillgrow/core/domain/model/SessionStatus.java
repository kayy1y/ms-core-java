package com.skillgrow.core.domain.model;

/** Estados de la máquina de estados de la sesión (CONTRATO_API.md §3). */
public enum SessionStatus implements WireEnum {
    PENDIENTE("pendiente"),
    CONFIRMADA("confirmada"),
    COMPLETADA("completada"),
    CANCELADA("cancelada"),
    REPROGRAMADA("reprogramada");

    private final String value;

    SessionStatus(String value) {
        this.value = value;
    }

    @Override
    public String value() {
        return value;
    }

    /** Estados que ocupan el día del servicio: pendiente, confirmada y reprogramada. */
    public boolean isActive() {
        return this == PENDIENTE || this == CONFIRMADA || this == REPROGRAMADA;
    }
}
