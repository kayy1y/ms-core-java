package com.skillgrow.core.infrastructure.adapter.in.rest.mapper;

import com.skillgrow.core.domain.model.HistoryEntry;
import com.skillgrow.core.domain.model.Location;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionDetails;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.SessionDtos.HistoryDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.SessionDtos.LocationDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.SessionDtos.ServiceRef;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.SessionDtos.SessionResponse;

/**
 * Dominio → SessionResponse. Aplica las reglas de visibilidad de la Fase I: el enlace solo lo ven los
 * participantes de una sesión confirmada o reprogramada, y el lugar solo los participantes.
 */
public final class SessionRestMapper {

    private SessionRestMapper() {}

    /** viewerId null = vista de administración (sin enlace ni lugar). */
    public static SessionResponse toResponse(SessionDetails d, String viewerId) {
        Session s = d.session();
        boolean participant = viewerId != null && s.isParticipant(viewerId);
        Location loc = participant ? s.location() : null;
        return new SessionResponse(
                s.id(),
                new ServiceRef(s.service().id(), s.service().title(), s.service().durationMinutes(),
                        s.service().type().value()),
                UserRestMapper.summary(d.person(s.mentorId())),
                s.learnerIds().stream().map(id -> UserRestMapper.summary(d.person(id))).toList(),
                s.date().toString(),
                s.modality().value(),
                s.status().value(),
                s.platform() == null ? null : s.platform().value(),
                s.otherPlatformName(),
                viewerId != null && s.canSeeMeetingLink(viewerId) ? s.meetingLink() : null,
                loc == null ? null : new LocationDto(loc.name(), loc.address(), loc.notes()),
                s.learnerNote(),
                s.proposedBy(),
                s.cancelReason() == null ? null : s.cancelReason().value(),
                s.cancelComment(),
                s.history().stream().map(SessionRestMapper::history).toList(),
                viewerId != null && s.canReview(viewerId),
                s.createdAtEpochMs());
    }

    private static HistoryDto history(HistoryEntry h) {
        return new HistoryDto(h.status().value(), h.atEpochMs(), h.byUserId(), h.comment());
    }
}
