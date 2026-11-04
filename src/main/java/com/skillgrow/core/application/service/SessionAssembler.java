package com.skillgrow.core.application.service;

import com.skillgrow.core.domain.event.NotificationEvent;
import com.skillgrow.core.domain.event.SessionEvent;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionDetails;
import com.skillgrow.core.domain.model.UserSummary;
import com.skillgrow.core.domain.port.out.UserRepositoryPort;
import com.skillgrow.core.domain.service.NotificationTexts;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Junta las sesiones con los nombres de sus participantes (una sola consulta por página). */
public class SessionAssembler {

    private final UserRepositoryPort users;

    public SessionAssembler(UserRepositoryPort users) {
        this.users = users;
    }

    public SessionDetails details(Session s) {
        return new SessionDetails(s, users.findSummaries(participants(List.of(s))));
    }

    public PageResult<SessionDetails> details(PageResult<Session> page) {
        Map<String, UserSummary> people = users.findSummaries(participants(page.items()));
        return page.map(s -> new SessionDetails(s, people));
    }

    /** Convierte los eventos de la sesión en notificaciones con nombres reales. */
    public List<NotificationEvent> notifications(Session s, List<SessionEvent> events, long now) {
        if (events.isEmpty()) {
            return List.of();
        }
        Set<String> ids = participants(List.of(s));
        events.forEach(e -> ids.add(e.actorId()));
        Map<String, UserSummary> people = users.findSummaries(ids);
        return events.stream()
                .flatMap(e -> NotificationTexts.build(s, e,
                        id -> people.getOrDefault(id, UserSummary.deleted(id)), now).stream())
                .toList();
    }

    private static Set<String> participants(List<Session> sessions) {
        Set<String> ids = new HashSet<>();
        for (Session s : sessions) {
            ids.add(s.mentorId());
            ids.addAll(s.learnerIds());
        }
        return ids;
    }
}
