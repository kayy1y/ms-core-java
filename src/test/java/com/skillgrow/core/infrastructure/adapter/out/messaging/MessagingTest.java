package com.skillgrow.core.infrastructure.adapter.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillgrow.core.domain.event.NotificationEvent;
import com.skillgrow.core.domain.model.NotificationType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Contrato del mensaje MS1 → MS3 y la regla "solo se publica si la transacción hizo commit". */
class MessagingTest {

    private final NotificationEvent event = new NotificationEvent("8c1d6b1e-0b8f-4a7e-9a55-2f0c7c7e1a10",
            NotificationType.SESSION_ACCEPTED, "usr_001", "Andrea aceptó tu solicitud",
            "«Cálculo I» quedó confirmada para el jueves 5 de noviembre.", "/sesiones/ses_1", "ses_1", "session",
            1_762_300_000_000L);

    @AfterEach
    void clear() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void elMensajeCumpleElContratoDelDoc03() throws Exception {
        JsonNode json = new ObjectMapper().readTree(
                new ObjectMapper().writeValueAsString(NotificationMessage.from(event, "abc-123")));

        assertThat(json.get("eventId").asText()).isEqualTo(event.eventId());
        assertThat(json.get("eventType").asText()).isEqualTo("SESSION_ACCEPTED");
        assertThat(json.get("version").asInt()).isEqualTo(1);
        assertThat(json.get("source").asText()).isEqualTo("ms-core-java");
        assertThat(json.get("occurredAtEpochMs").asLong()).isEqualTo(1_762_300_000_000L);
        assertThat(json.get("userId").asText()).isEqualTo("usr_001");
        assertThat(json.get("title").asText()).isEqualTo("Andrea aceptó tu solicitud");
        assertThat(json.get("body").asText()).startsWith("«Cálculo I»");
        assertThat(json.get("data").get("link").asText()).isEqualTo("/sesiones/ses_1");
        assertThat(json.get("data").get("entityType").asText()).isEqualTo("session");
        assertThat(json.get("correlationId").asText()).isEqualTo("abc-123");
    }

    @Test
    void dentroDeUnaTransaccionEsperaAlCommit() {
        List<String> sent = new ArrayList<>();
        TransactionSynchronizationManager.initSynchronization();

        AfterCommit.run(() -> sent.add("enviado"));
        assertThat(sent).isEmpty();

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertThat(sent).containsExactly("enviado");
    }

    @Test
    void siLaTransaccionSeRevierteNoSePublica() {
        List<String> sent = new ArrayList<>();
        TransactionSynchronizationManager.initSynchronization();

        AfterCommit.run(() -> sent.add("enviado"));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(sent).isEmpty();
    }
}
