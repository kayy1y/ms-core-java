package com.skillgrow.core.domain.port.out;

import com.skillgrow.core.domain.event.NotificationEvent;
import java.util.List;

/**
 * Publica las notificaciones hacia MS3. La implementación de Service Bus las envía después del commit,
 * para no anunciar algo que finalmente no se guardó.
 */
public interface EventPublisherPort {

    void publish(List<NotificationEvent> events);
}
