package com.skillgrow.core.domain.model;

/** Resumen público de un usuario para listas y sesiones. El rating lo calcula MS2 (dueño de las reseñas). */
public record UserSummary(String id, String fullName, String avatarUrl) {

    public static UserSummary deleted(String id) {
        return new UserSummary(id, "Usuario eliminado", null);
    }

    /** Primer nombre, para los textos de las notificaciones. */
    public String firstName() {
        return fullName == null || fullName.isBlank() ? "Alguien" : fullName.trim().split("\\s+")[0];
    }
}
