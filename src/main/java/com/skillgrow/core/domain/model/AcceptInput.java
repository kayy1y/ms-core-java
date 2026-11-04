package com.skillgrow.core.domain.model;

/**
 * Lo que manda el mentor al aceptar: enlace y plataforma si es virtual, lugar si es presencial.
 * Llega sin validar; la sesión valida solo lo que corresponde a su modalidad.
 */
public record AcceptInput(
        Platform platform,
        String otherPlatformName,
        String meetingLink,
        String locationName,
        String locationAddress,
        String locationNotes) {

    public static AcceptInput empty() {
        return new AcceptInput(null, null, null, null, null, null);
    }
}
