package com.skillgrow.core.domain;

import static com.skillgrow.core.domain.Fixtures.LEARNER;
import static com.skillgrow.core.domain.Fixtures.MENTOR;
import static com.skillgrow.core.domain.Fixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;

import com.skillgrow.core.domain.event.NotificationEvent;
import com.skillgrow.core.domain.model.Modality;
import com.skillgrow.core.domain.model.NotificationType;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.UserSummary;
import com.skillgrow.core.domain.service.NotificationTexts;
import com.skillgrow.core.domain.service.SpanishDates;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** Los textos deben ser los mismos de la Fase I para que la web y el móvil no cambien. */
class NotificationTextsTest {

    private final Map<String, UserSummary> people = Map.of(
            MENTOR, new UserSummary(MENTOR, "Andrea Vargas", null),
            LEARNER, new UserSummary(LEARNER, "Carlos Ramírez", null));
    private final Function<String, UserSummary> lookup = people::get;

    @Test
    void solicitudNueva() {
        LocalDate day = LocalDate.of(2026, 11, 5);
        Session s = Session.request("ses_1", Fixtures.individual(), LEARNER, day, Modality.VIRTUAL, null, NOW);

        List<NotificationEvent> out = NotificationTexts.build(s, s.pullEvents().getFirst(), lookup, NOW);

        assertThat(out).singleElement().satisfies(n -> {
            assertThat(n.recipientId()).isEqualTo(MENTOR);
            assertThat(n.type()).isEqualTo(NotificationType.SESSION_REQUESTED);
            assertThat(n.title()).isEqualTo("Nueva solicitud de sesión");
            assertThat(n.message()).isEqualTo("Carlos Ramírez quiere una sesión de «Cálculo I» el jueves 5 de noviembre.");
            assertThat(n.link()).isEqualTo("/solicitudes");
            assertThat(n.entityId()).isEqualTo("ses_1");
            assertThat(n.eventId()).hasSize(36);
        });
    }

    @Test
    void completarGeneraDosNotificacionesEnOrden() {
        Session s = Fixtures.confirmedVirtual();
        s.complete(MENTOR, Fixtures.DAY, NOW);

        List<NotificationEvent> out = s.pullEvents().stream()
                .flatMap(e -> NotificationTexts.build(s, e, lookup, NOW).stream())
                .toList();

        assertThat(out).extracting(NotificationEvent::title)
                .containsExactly("Sesión completada", "¿Cómo te fue con Andrea?");
        assertThat(out.get(1).link()).isEqualTo("/sesiones/ses_1?evaluar=1");
        assertThat(out.get(1).occurredAtEpochMs()).isGreaterThan(out.get(0).occurredAtEpochMs());
    }

    @Test
    void cancelacionIncluyeElMotivo() {
        Session s = Fixtures.confirmedVirtual();
        s.cancel(MENTOR, "Me enfermé", NOW);

        NotificationEvent n = NotificationTexts.build(s, s.pullEvents().getFirst(), lookup, NOW).getFirst();

        assertThat(n.title()).isEqualTo("Andrea canceló la sesión");
        assertThat(n.message()).endsWith("fue cancelada. Motivo: Me enfermé");
    }

    @Test
    void fechasEnEspanol() {
        assertThat(SpanishDates.whenText(LocalDate.of(2026, 9, 7))).isEqualTo("lunes 7 de setiembre");
    }
}
