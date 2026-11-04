package com.skillgrow.core.infrastructure.adapter.out.persistence;

import com.skillgrow.core.domain.model.CancelReason;
import com.skillgrow.core.domain.model.HistoryEntry;
import com.skillgrow.core.domain.model.Location;
import com.skillgrow.core.domain.model.Modality;
import com.skillgrow.core.domain.model.OfferedModality;
import com.skillgrow.core.domain.model.Platform;
import com.skillgrow.core.domain.model.ServiceSnapshot;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionStatus;
import com.skillgrow.core.domain.model.SessionType;
import com.skillgrow.core.domain.model.WireEnum;
import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.HistoryEmbeddable;
import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.SessionEntity;
import java.util.List;

/** Session (dominio) ↔ SessionEntity (JPA). */
final class SessionPersistenceMapper {

    private SessionPersistenceMapper() {}

    static Session toDomain(SessionEntity e) {
        ServiceSnapshot service = new ServiceSnapshot(e.serviceId, e.mentorId, e.serviceTitle,
                e.serviceDurationMinutes, enumOf(SessionType.class, e.serviceType),
                e.serviceCapacity, enumOf(OfferedModality.class, e.serviceModality), true);
        Location location = e.locationName == null ? null
                : new Location(e.locationName, e.locationAddress, e.locationNotes);
        List<HistoryEntry> history = e.history.stream()
                .map(h -> new HistoryEntry(enumOf(SessionStatus.class, h.status), h.atEpochMs, h.byUserId, h.comment))
                .toList();
        return new Session(e.id, service, e.mentorId, e.learnerIds, e.sessionDate,
                enumOf(Modality.class, e.modality), enumOf(SessionStatus.class, e.status),
                WireEnum.find(Platform.class, e.platform).orElse(null), e.otherPlatformName, e.meetingLink,
                location, e.learnerNote, e.proposedBy,
                WireEnum.find(CancelReason.class, e.cancelReason).orElse(null), e.cancelComment,
                history, e.createdAtEpochMs, e.version);
    }

    /**
     * Copia el estado del dominio a la entidad. Aprendices e historial solo crecen, así que se agregan
     * las filas nuevas en lugar de reescribir toda la colección.
     */
    static void copyInto(Session s, SessionEntity e) {
        e.id = s.id();
        e.serviceId = s.service().id();
        e.serviceTitle = s.service().title();
        e.serviceDurationMinutes = s.service().durationMinutes();
        e.serviceType = s.service().type().value();
        e.serviceCapacity = s.service().capacity();
        e.serviceModality = s.service().modality().value();
        e.mentorId = s.mentorId();
        e.sessionDate = s.date();
        e.modality = s.modality().value();
        e.status = s.status().value();
        e.platform = s.platform() == null ? null : s.platform().value();
        e.otherPlatformName = s.otherPlatformName();
        e.meetingLink = s.meetingLink();
        e.locationName = s.location() == null ? null : s.location().name();
        e.locationAddress = s.location() == null ? null : s.location().address();
        e.locationNotes = s.location() == null ? null : s.location().notes();
        e.learnerNote = s.learnerNote();
        e.proposedBy = s.proposedBy();
        e.cancelReason = s.cancelReason() == null ? null : s.cancelReason().value();
        e.cancelComment = s.cancelComment();
        e.createdAtEpochMs = s.createdAtEpochMs();

        List<String> learners = s.learnerIds();
        for (int i = e.learnerIds.size(); i < learners.size(); i++) {
            e.learnerIds.add(learners.get(i));
        }
        List<HistoryEntry> history = s.history();
        for (int i = e.history.size(); i < history.size(); i++) {
            HistoryEntry h = history.get(i);
            e.history.add(new HistoryEmbeddable(h.status().value(), h.atEpochMs(), h.byUserId(), h.comment()));
        }
    }

    private static <E extends Enum<E> & WireEnum> E enumOf(Class<E> type, String value) {
        return WireEnum.find(type, value)
                .orElseThrow(() -> new IllegalStateException("Valor desconocido en la BD para " + type.getSimpleName() + ": " + value));
    }
}
