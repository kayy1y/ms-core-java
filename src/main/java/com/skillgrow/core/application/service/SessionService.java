package com.skillgrow.core.application.service;

import com.skillgrow.core.domain.exception.DayUnavailableException;
import com.skillgrow.core.domain.exception.FieldIssue;
import com.skillgrow.core.domain.exception.NotFoundException;
import com.skillgrow.core.domain.exception.ValidationException;
import com.skillgrow.core.domain.model.AcceptInput;
import com.skillgrow.core.domain.model.AvailabilityRule;
import com.skillgrow.core.domain.model.BlockedDate;
import com.skillgrow.core.domain.model.Modality;
import com.skillgrow.core.domain.model.Platform;
import com.skillgrow.core.domain.model.ServiceSnapshot;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionDetails;
import com.skillgrow.core.domain.model.SessionStatus;
import com.skillgrow.core.domain.model.SessionType;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.model.WireEnum;
import com.skillgrow.core.domain.port.in.GamificationUseCase;
import com.skillgrow.core.domain.port.in.SessionCommandUseCase;
import com.skillgrow.core.domain.port.out.AvailabilityRepositoryPort;
import com.skillgrow.core.domain.port.out.EventPublisherPort;
import com.skillgrow.core.domain.port.out.ServiceCatalogPort;
import com.skillgrow.core.domain.port.out.SessionRepositoryPort;
import com.skillgrow.core.domain.port.out.UserRepositoryPort;
import com.skillgrow.core.domain.service.AvailabilityPolicy;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso que cambian una sesión. Patrón de cada acción:
 * cargar → aplicar la regla en el dominio → guardar → publicar los eventos (después del commit).
 */
@Transactional
public class SessionService implements SessionCommandUseCase {

    private final SessionRepositoryPort sessions;
    private final AvailabilityRepositoryPort availability;
    private final ServiceCatalogPort catalog;
    private final UserRepositoryPort users;
    private final EventPublisherPort events;
    private final SessionAssembler assembler;
    private final GamificationUseCase gamification;
    private final Clock clock;

    public SessionService(SessionRepositoryPort sessions, AvailabilityRepositoryPort availability,
                          ServiceCatalogPort catalog, UserRepositoryPort users, EventPublisherPort events,
                          SessionAssembler assembler, GamificationUseCase gamification, Clock clock) {
        this.sessions = sessions;
        this.availability = availability;
        this.catalog = catalog;
        this.users = users;
        this.events = events;
        this.assembler = assembler;
        this.gamification = gamification;
        this.clock = clock;
    }

    // ---------- Reservar ----------

    @Override
    public SessionDetails create(String userId, CreateSessionCommand c) {
        LocalDate date = Dates.parseOrNull(c.date());
        Modality modality = WireEnum.find(Modality.class, c.modality()).orElse(null);
        List<FieldIssue> issues = new ArrayList<>();
        if (c.serviceId() == null || c.serviceId().isBlank()) {
            issues.add(new FieldIssue("serviceId", "Selecciona un servicio."));
        }
        if (date == null) {
            issues.add(new FieldIssue("date", "Selecciona un día disponible."));
        }
        if (modality == null) {
            issues.add(new FieldIssue("modality", "Selecciona la modalidad."));
        }
        if (c.learnerNote() != null && c.learnerNote().length() > 500) {
            issues.add(new FieldIssue("learnerNote", "La nota admite hasta 500 caracteres."));
        }
        ValidationException.throwIfAny(issues);

        ServiceSnapshot service = catalog.findService(c.serviceId())
                .filter(ServiceSnapshot::active)
                .orElseThrow(() -> new NotFoundException("El servicio"));
        Session.checkCanRequest(service, userId, modality);
        long now = clock.millis();
        LocalDate today = LocalDate.now(clock);
        List<AvailabilityRule> rules = availability.findRules(service.mentorId());
        List<BlockedDate> blocked = availability.findBlockedDates(service.mentorId());
        List<Session> sameDay = sessions.findActiveByService(service.id(), date, date);

        // Sumarse a la sesión grupal de ese día si todavía tiene cupo.
        if (service.type() == SessionType.GRUPAL) {
            Optional<Session> group = AvailabilityPolicy.activeSessionOn(service, date, sameDay, null);
            if (group.isPresent()) {
                Session s = group.get();
                if (!s.learnerIds().contains(userId)
                        && (AvailabilityPolicy.remainingSeats(service, date, sameDay, null) <= 0
                        || !AvailabilityPolicy.isDayAvailable(service, rules, blocked, sameDay, date, today, null))) {
                    throw new DayUnavailableException();
                }
                s.joinGroup(userId, fullName(userId), now);
                return saveAndPublish(s, now);
            }
        }
        if (!AvailabilityPolicy.isDayAvailable(service, rules, blocked, sameDay, date, today, null)) {
            throw new DayUnavailableException();
        }
        Session s = Session.request(UUID.randomUUID().toString(), service, userId, date, modality, c.learnerNote(), now);
        return saveAndPublish(s, now);
    }

    // ---------- Cambios de estado ----------

    @Override
    public SessionDetails accept(String userId, String sessionId, AcceptInput input) {
        return change(sessionId, (s, now) -> s.accept(userId, input,
                () -> sessions.existsConfirmedOn(s.service().id(), s.date(), s.id()), now));
    }

    @Override
    public SessionDetails reject(String userId, String sessionId, String comment) {
        return change(sessionId, (s, now) -> s.reject(userId, comment, now));
    }

    @Override
    public SessionDetails reschedule(String userId, String sessionId, String date, String comment) {
        LocalDate newDate = Dates.parseOrNull(date);
        LocalDate today = LocalDate.now(clock);
        return change(sessionId, (s, now) -> s.reschedule(userId, newDate, comment, day -> {
            List<AvailabilityRule> rules = availability.findRules(s.mentorId());
            List<BlockedDate> blocked = availability.findBlockedDates(s.mentorId());
            List<Session> sameDay = sessions.findActiveByService(s.service().id(), day, day);
            return AvailabilityPolicy.isDayAvailable(s.service(), rules, blocked, sameDay, day, today, s.id());
        }, now));
    }

    @Override
    public SessionDetails cancel(String userId, String sessionId, String comment) {
        return change(sessionId, (s, now) -> s.cancel(userId, comment, now));
    }

    @Override
    public SessionDetails complete(String userId, String sessionId) {
        SessionDetails done = change(sessionId, (s, now) -> s.complete(userId, LocalDate.now(clock), now));
        Session s = done.session();
        if (s.status() == SessionStatus.COMPLETADA) {
            long now = clock.millis();
            gamification.evaluate(s.mentorId(), now);
            s.learnerIds().forEach(id -> gamification.evaluate(id, now));
        }
        return done;
    }

    @Override
    public SessionDetails updateMeeting(String userId, String sessionId, String platform, String otherPlatformName,
                                        String meetingLink) {
        Platform p = WireEnum.find(Platform.class, platform).orElse(null);
        return change(sessionId, (s, now) -> s.updateMeeting(userId, p, otherPlatformName, meetingLink));
    }

    @Override
    public SessionDetails updateLocation(String userId, String sessionId, String name, String address, String notes) {
        return change(sessionId, (s, now) -> s.updateLocation(userId, name, address, notes));
    }

    // ---------- Internos ----------

    private interface Action {
        void apply(Session session, long now);
    }

    private SessionDetails change(String sessionId, Action action) {
        Session s = sessions.findById(sessionId).orElseThrow(() -> new NotFoundException("La sesión"));
        long now = clock.millis();
        action.apply(s, now);
        return saveAndPublish(s, now);
    }

    /** Los eventos se sacan del agregado antes de guardar; el publicador los manda tras el commit. */
    private SessionDetails saveAndPublish(Session s, long now) {
        var pending = s.pullEvents();
        Session saved = sessions.save(s);
        events.publish(assembler.notifications(saved, pending, now));
        return assembler.details(saved);
    }

    private String fullName(String userId) {
        return users.findById(userId).map(User::fullName).orElse("Alguien");
    }
}
