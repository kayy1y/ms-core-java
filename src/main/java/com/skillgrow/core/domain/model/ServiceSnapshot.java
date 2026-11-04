package com.skillgrow.core.domain.model;

/**
 * Copia de los datos del servicio de mentoría (que vive en MS2) que MS1 necesita para reservar.
 * Se guarda dentro de la sesión al crearla, así la sesión no depende de que MS2 responda después.
 */
public record ServiceSnapshot(
        String id,
        String mentorId,
        String title,
        int durationMinutes,
        SessionType type,
        int capacity,
        OfferedModality modality,
        boolean active) {

    /** Cupos totales de un día: 1 si es individual, la capacidad si es grupal. */
    public int seatsPerDay() {
        return type == SessionType.GRUPAL ? capacity : 1;
    }
}
