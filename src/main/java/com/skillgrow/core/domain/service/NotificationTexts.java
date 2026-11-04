package com.skillgrow.core.domain.service;

import static com.skillgrow.core.domain.service.SpanishDates.whenText;

import com.skillgrow.core.domain.event.NotificationEvent;
import com.skillgrow.core.domain.event.SessionEvent;
import com.skillgrow.core.domain.event.SessionEvent.Variant;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.UserSummary;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

/**
 * Convierte un {@link SessionEvent} en las notificaciones que viajan a la cola (una por destinatario).
 * Los textos son los mismos de la Fase I (logic/sessions.ts) para que la web y el móvil no cambien.
 */
public final class NotificationTexts {

    private NotificationTexts() {}

    public static List<NotificationEvent> build(Session s, SessionEvent e,
                                                Function<String, UserSummary> people, long now) {
        UserSummary actor = people.apply(e.actorId());
        String title = s.service().title();
        String when = whenText(e.date());
        String link = "/sesiones/" + s.id();
        String reason = e.comment() == null ? "" : " Motivo: " + e.comment();

        return switch (e.type()) {
            case SESSION_REQUESTED -> e.variant() == Variant.JOINED_GROUP
                    ? to(e, s, "Nuevo participante en tu sesión grupal",
                            actor.fullName() + " se unió a «" + title + "» (" + when + ").", link, now)
                    : to(e, s, "Nueva solicitud de sesión",
                            actor.fullName() + " quiere una sesión de «" + title + "» el " + when + ".",
                            "/solicitudes", now);
            case SESSION_ACCEPTED -> to(e, s,
                    e.variant() == Variant.AFTER_RESCHEDULE
                            ? actor.firstName() + " confirmó el nuevo día"
                            : actor.firstName() + " aceptó tu solicitud",
                    "«" + title + "» quedó confirmada para el " + when + ".", link, now);
            case SESSION_REJECTED -> to(e, s, actor.firstName() + " no puede atender tu solicitud",
                    "Tu solicitud de «" + title + "» fue rechazada." + reason + " El día quedó libre para elegir otro.",
                    link, now);
            case SESSION_RESCHEDULED -> to(e, s, actor.firstName() + " propuso un nuevo día",
                    "«" + title + "» pasaría al " + when + ". Confírmalo o propón otro.", link, now);
            case SESSION_CANCELLED -> to(e, s, actor.firstName() + " canceló la sesión",
                    "«" + title + "» del " + when + " fue cancelada." + reason, link, now);
            case SESSION_COMPLETED -> to(e, s, "Sesión completada",
                    actor.firstName() + " marcó «" + title + "» como completada.", link, now);
            // +1 ms para que en el historial aparezca justo después de "Sesión completada".
            case REVIEW_REQUESTED -> to(e, s, "¿Cómo te fue con " + actor.firstName() + "?",
                    "Evalúa tu sesión «" + title + "». Tu reseña ayuda a otros estudiantes.",
                    link + "?evaluar=1", now + 1);
            default -> List.of();
        };
    }

    private static List<NotificationEvent> to(SessionEvent e, Session s, String title, String message,
                                              String link, long at) {
        return e.recipientIds().stream()
                .map(recipient -> new NotificationEvent(UUID.randomUUID().toString(), e.type(), recipient,
                        title, message, link, s.id(), "session", at))
                .toList();
    }
}
