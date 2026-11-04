package com.skillgrow.core.infrastructure.adapter.out.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillgrow.core.domain.event.NotificationEvent;
import com.skillgrow.core.domain.port.out.EventPublisherPort;
import com.skillgrow.core.infrastructure.security.CorrelationIdFilter;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Con EVENTS_ENABLED=false (local): escribe en el log el JSON exacto que iría a Service Bus. */
@Component
@ConditionalOnProperty(name = "app.events.enabled", havingValue = "false", matchIfMissing = true)
class LoggingEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    private final ObjectMapper json;

    LoggingEventPublisher(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public void publish(List<NotificationEvent> events) {
        String correlationId = CorrelationIdFilter.current();
        AfterCommit.run(() -> events.forEach(e -> {
            try {
                log.info("[evento → cola notifications] {}", json.writeValueAsString(NotificationMessage.from(e, correlationId)));
            } catch (JsonProcessingException ex) {
                log.error("No se pudo serializar el evento {}", e.eventId(), ex);
            }
        }));
    }
}
