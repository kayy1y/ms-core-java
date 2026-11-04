package com.skillgrow.core.application.service;

import com.skillgrow.core.domain.exception.ForbiddenException;
import com.skillgrow.core.domain.exception.NotFoundException;
import com.skillgrow.core.domain.exception.ValidationException;
import com.skillgrow.core.domain.model.AccountStatus;
import com.skillgrow.core.domain.model.Modality;
import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.Role;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionDetails;
import com.skillgrow.core.domain.model.SessionStatus;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.model.WireEnum;
import com.skillgrow.core.domain.port.in.AdminUseCase;
import com.skillgrow.core.domain.port.out.EventPublisherPort;
import com.skillgrow.core.domain.port.out.SessionRepositoryPort;
import com.skillgrow.core.domain.port.out.UserRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/** Administración de usuarios y sesiones. Las estadísticas de servicios y reseñas las da MS2. */
@Transactional
public class AdminService implements AdminUseCase {

    private static final int ACTIVITY_DAYS = 30;

    private final UserRepositoryPort users;
    private final SessionRepositoryPort sessions;
    private final SessionAssembler assembler;
    private final EventPublisherPort events;
    private final Clock clock;

    public AdminService(UserRepositoryPort users, SessionRepositoryPort sessions, SessionAssembler assembler,
                        EventPublisherPort events, Clock clock) {
        this.users = users;
        this.sessions = sessions;
        this.assembler = assembler;
        this.events = events;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminUserRow> users(String search, String role, String status, String sort, PageQuery page) {
        String[] parts = (sort == null || sort.isBlank() ? "createdAt:desc" : sort).split(":");
        PageResult<User> found = users.search(search,
                WireEnum.find(Role.class, role).orElse(null),
                WireEnum.find(AccountStatus.class, status).orElse(null),
                parts[0], parts.length > 1 && "asc".equals(parts[1]), page);
        Map<String, Long> completed = sessions.completedAsMentorByUser(found.items().stream().map(User::id).toList());
        return found.map(u -> new AdminUserRow(u, completed.getOrDefault(u.id(), 0L)));
    }

    /** Suspender cancela las sesiones activas del usuario y avisa a la otra parte de cada una. */
    @Override
    public User changeUserStatus(String adminId, String userId, String status, String reason) {
        AccountStatus newStatus = WireEnum.find(AccountStatus.class, status)
                .orElseThrow(() -> ValidationException.of("status", "Estado inválido."));
        String why = reason == null ? "" : reason.trim();
        if (why.length() < 5) {
            throw ValidationException.of("reason", "Explica el motivo (mínimo 5 caracteres).");
        }
        User user = users.findById(userId).orElseThrow(() -> new NotFoundException("El usuario"));
        if (user.isAdmin()) {
            throw new ForbiddenException("No es posible suspender a un administrador.");
        }
        if (user.id().equals(adminId)) {
            throw new ForbiddenException("No puedes cambiar el estado de tu propia cuenta.");
        }
        long now = clock.millis();
        user.changeStatus(newStatus, now);
        User saved = users.save(user);
        if (newStatus == AccountStatus.SUSPENDED) {
            for (Session s : sessions.findActiveByParticipant(userId)) {
                s.cancel(userId, "Cuenta suspendida por administración: " + why, now);
                var pending = s.pullEvents();
                Session stored = sessions.save(s);
                events.publish(assembler.notifications(stored, pending, now));
            }
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<SessionDetails> sessions(String status, String modality, String from, String to, String search,
                                               PageQuery page) {
        return assembler.details(sessions.findForAdmin(
                WireEnum.find(SessionStatus.class, status).orElse(null),
                WireEnum.find(Modality.class, modality).orElse(null),
                Dates.parseOrNull(from), Dates.parseOrNull(to), search, page));
    }

    @Override
    @Transactional(readOnly = true)
    public AdminStats stats() {
        ZoneId zone = clock.getZone();
        LocalDate today = LocalDate.now(clock);
        LocalDate first = today.minusDays(ACTIVITY_DAYS - 1L);
        Map<LocalDate, Long> sessionsPerDay = sessions.countByDateBetween(first, today);
        long fromMs = first.atStartOfDay(zone).toInstant().toEpochMilli();
        Map<LocalDate, Long> usersPerDay = users.createdSince(fromMs).stream()
                .map(ms -> LocalDate.ofInstant(Instant.ofEpochMilli(ms), zone))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        List<DailyActivity> activity = new ArrayList<>();
        for (LocalDate d = first; !d.isAfter(today); d = d.plusDays(1)) {
            activity.add(new DailyActivity(d, sessionsPerDay.getOrDefault(d, 0L), usersPerDay.getOrDefault(d, 0L)));
        }
        Map<SessionStatus, Long> byStatus = sessions.countByStatus();
        return new AdminStats(users.count(), users.countByStatus(AccountStatus.ACTIVE),
                users.countByStatus(AccountStatus.SUSPENDED), users.countMentors(),
                byStatus.values().stream().mapToLong(Long::longValue).sum(), byStatus, activity);
    }
}
