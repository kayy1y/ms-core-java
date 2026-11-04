package com.skillgrow.core.domain.event;

import com.skillgrow.core.domain.model.NotificationType;
import java.time.LocalDate;
import java.util.List;

/**
 * Algo que pasó en una sesión y que hay que notificar. Lo genera el agregado Session; los textos los
 * arma {@link com.skillgrow.core.domain.service.NotificationTexts} porque necesitan los nombres.
 *
 * @param actorId      quién hizo la acción
 * @param recipientIds a quién se le avisa
 * @param comment      motivo que escribió el actor (rechazo o cancelación), puede ser null
 * @param variant      matiz del texto: NEW, JOINED_GROUP o AFTER_RESCHEDULE
 */
public record SessionEvent(
        NotificationType type,
        String actorId,
        List<String> recipientIds,
        String comment,
        LocalDate date,
        Variant variant) {

    public enum Variant { NEW, JOINED_GROUP, AFTER_RESCHEDULE }

    public SessionEvent {
        recipientIds = List.copyOf(recipientIds);
    }
}
