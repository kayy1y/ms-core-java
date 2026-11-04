package com.skillgrow.core.infrastructure.adapter.out.messaging;

import com.skillgrow.core.domain.event.NotificationEvent;
import java.util.Map;

/**
 * Cuerpo del mensaje de la cola "notifications" (contrato MS1 → MS3, doc 03 §6).
 * data solo lleva strings porque FCM solo acepta strings en su campo data.
 */
public record NotificationMessage(
        String eventId,
        String eventType,
        int version,
        String source,
        long occurredAtEpochMs,
        String userId,
        String title,
        String body,
        Map<String, String> data,
        String correlationId) {

    public static final int VERSION = 1;
    public static final String SOURCE = "ms-core-java";

    public static NotificationMessage from(NotificationEvent e, String correlationId) {
        return new NotificationMessage(e.eventId(), e.type().value(), VERSION, SOURCE, e.occurredAtEpochMs(),
                e.recipientId(), e.title(), e.message(),
                Map.of("entityId", e.entityId(), "entityType", e.entityType(), "link", e.link()),
                correlationId);
    }
}
