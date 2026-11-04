package com.skillgrow.core.infrastructure.adapter.out.messaging;

import com.azure.core.amqp.AmqpRetryMode;
import com.azure.core.amqp.AmqpRetryOptions;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillgrow.core.domain.event.NotificationEvent;
import com.skillgrow.core.domain.port.out.EventPublisherPort;
import com.skillgrow.core.infrastructure.config.AppProperties;
import com.skillgrow.core.infrastructure.security.CorrelationIdFilter;
import jakarta.annotation.PreDestroy;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Publica cada notificación en la cola de Azure Service Bus, después del commit.
 * messageId = eventId: MS3 lo usa para descartar duplicados. El SDK reintenta 3 veces con backoff
 * exponencial; si aun así falla, se registra el error y NO se revierte la sesión (ya está guardada).
 * Mejora documentada: patrón outbox para no perder el evento.
 */
@Component
@ConditionalOnProperty(name = "app.events.enabled", havingValue = "true")
class ServiceBusEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(ServiceBusEventPublisher.class);

    private final ServiceBusSenderClient sender;
    private final ObjectMapper json;

    ServiceBusEventPublisher(AppProperties props, ObjectMapper json) {
        AppProperties.Events cfg = props.events();
        if (cfg.connection() == null || cfg.connection().isBlank()) {
            throw new IllegalStateException("EVENTS_ENABLED=true necesita SERVICEBUS_CONNECTION");
        }
        this.sender = new ServiceBusClientBuilder()
                .connectionString(cfg.connection())
                .retryOptions(new AmqpRetryOptions()
                        .setMode(AmqpRetryMode.EXPONENTIAL)
                        .setMaxRetries(3)
                        .setDelay(Duration.ofMillis(500))
                        .setTryTimeout(Duration.ofSeconds(10)))
                .sender()
                .queueName(cfg.queue())
                .buildClient();
        this.json = json;
    }

    @Override
    public void publish(List<NotificationEvent> events) {
        if (events.isEmpty()) {
            return;
        }
        String correlationId = CorrelationIdFilter.current();
        AfterCommit.run(() -> events.forEach(e -> send(e, correlationId)));
    }

    private void send(NotificationEvent e, String correlationId) {
        try {
            ServiceBusMessage message = new ServiceBusMessage(json.writeValueAsBytes(NotificationMessage.from(e, correlationId)))
                    .setMessageId(e.eventId())
                    .setContentType("application/json")
                    .setSubject(e.type().value());
            if (correlationId != null) {
                message.setCorrelationId(correlationId);
            }
            sender.sendMessage(message);
            log.info("Evento {} publicado (eventId={}, userId={})", e.type().value(), e.eventId(), e.recipientId());
        } catch (Exception ex) {
            log.error("No se pudo publicar el evento {} (eventId={}, userId={}) tras los reintentos",
                    e.type().value(), e.eventId(), e.recipientId(), ex);
        }
    }

    @PreDestroy
    void close() {
        sender.close();
    }
}
