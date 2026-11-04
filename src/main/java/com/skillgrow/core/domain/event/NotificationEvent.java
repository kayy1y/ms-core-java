package com.skillgrow.core.domain.event;

import com.skillgrow.core.domain.model.NotificationType;

/**
 * Notificación lista para enviarse a la cola (un mensaje por destinatario). eventId es la clave de
 * idempotencia: MS3 descarta un eventId repetido.
 */
public record NotificationEvent(
        String eventId,
        NotificationType type,
        String recipientId,
        String title,
        String message,
        String link,
        String entityId,
        String entityType,
        long occurredAtEpochMs) {}
