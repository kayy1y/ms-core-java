package com.skillgrow.core.domain.model;

import com.skillgrow.core.domain.exception.FieldIssue;
import com.skillgrow.core.domain.exception.ValidationException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** Enlace de una sesión virtual. Se valida al aceptar o al editar el enlace. */
public record MeetingDetails(Platform platform, String otherPlatformName, String meetingLink) {

    /** Valida los datos que llegan del cliente; platform puede venir null si el valor no existe. */
    public static MeetingDetails validated(Platform platform, String otherPlatformName, String meetingLink) {
        List<FieldIssue> issues = new ArrayList<>();
        if (platform == null) {
            issues.add(new FieldIssue("platform", "Elige la plataforma de la reunión."));
        }
        if (platform == Platform.OTRA && isBlank(otherPlatformName)) {
            issues.add(new FieldIssue("otherPlatformName", "Escribe el nombre de la plataforma."));
        }
        if (!isHttpUrl(meetingLink)) {
            issues.add(new FieldIssue("meetingLink", "Ingresa un enlace válido que empiece con https://."));
        }
        ValidationException.throwIfAny(issues);
        String other = platform == Platform.OTRA ? otherPlatformName.trim() : null;
        return new MeetingDetails(platform, other, meetingLink.trim());
    }

    private static boolean isHttpUrl(String value) {
        if (isBlank(value)) {
            return false;
        }
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();
            return ("https".equals(scheme) || "http".equals(scheme)) && uri.getHost() != null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
