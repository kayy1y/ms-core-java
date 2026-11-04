package com.skillgrow.core.application.service;

import com.skillgrow.core.domain.event.NotificationEvent;
import com.skillgrow.core.domain.exception.FieldIssue;
import com.skillgrow.core.domain.exception.NotFoundException;
import com.skillgrow.core.domain.exception.ValidationException;
import com.skillgrow.core.domain.model.Badge;
import com.skillgrow.core.domain.model.BadgeCriterion;
import com.skillgrow.core.domain.model.NotificationType;
import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.UserBadge;
import com.skillgrow.core.domain.model.UserStats;
import com.skillgrow.core.domain.model.WireEnum;
import com.skillgrow.core.domain.port.in.GamificationUseCase;
import com.skillgrow.core.domain.port.out.BadgeRepositoryPort;
import com.skillgrow.core.domain.port.out.EventPublisherPort;
import com.skillgrow.core.domain.port.out.SessionRepositoryPort;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insignias y logros (port de logic/badges.ts). El criterio rating_average depende de las reseñas, que
 * viven en MS2: aquí su valor es 0 y esas insignias no se otorgan desde MS1.
 */
@Transactional
public class GamificationService implements GamificationUseCase {

    private final BadgeRepositoryPort badges;
    private final SessionRepositoryPort sessions;
    private final EventPublisherPort events;

    public GamificationService(BadgeRepositoryPort badges, SessionRepositoryPort sessions, EventPublisherPort events) {
        this.badges = badges;
        this.sessions = sessions;
        this.events = events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Badge> activeBadges() {
        return badges.findActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserBadge> userBadges(String userId) {
        return badges.findByUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchievementProgress> achievements(String userId) {
        Set<String> earned = earnedIds(userId);
        UserStats stats = sessions.statsFor(userId);
        List<Badge> active = badges.findActive();
        List<AchievementProgress> out = new ArrayList<>();
        for (BadgeCriterion criterion : BadgeCriterion.values()) {
            Badge next = active.stream()
                    .filter(b -> b.criterion() == criterion && !earned.contains(b.id()))
                    .min(Comparator.comparingDouble(Badge::threshold))
                    .orElse(null);
            out.add(new AchievementProgress(criterion, value(userId, criterion, stats),
                    next == null ? null : next.threshold(), next == null ? null : next.id()));
        }
        return out;
    }

    @Override
    public List<Badge> evaluate(String userId, long now) {
        Set<String> earned = earnedIds(userId);
        UserStats stats = sessions.statsFor(userId);
        List<Badge> fresh = new ArrayList<>();
        List<NotificationEvent> notifications = new ArrayList<>();
        for (Badge badge : badges.findActive()) {
            if (earned.contains(badge.id()) || badge.criterion() == BadgeCriterion.RATING_AVERAGE) {
                continue;
            }
            if (value(userId, badge.criterion(), stats) >= badge.threshold()) {
                String reason = reasonFor(badge);
                badges.award(userId, badge.id(), now, reason);
                fresh.add(badge);
                notifications.add(new NotificationEvent(UUID.randomUUID().toString(), NotificationType.BADGE_EARNED,
                        userId, "¡Nueva insignia: " + badge.name() + "!", reason, "/logros", badge.id(), "badge", now));
            }
        }
        events.publish(notifications);
        return fresh;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Badge> allBadges(PageQuery page) {
        return badges.findAll(page);
    }

    @Override
    public Badge createBadge(BadgeCommand c) {
        return badges.save(validated("bdg_" + UUID.randomUUID().toString().substring(0, 8), c));
    }

    @Override
    public Badge updateBadge(String badgeId, BadgeCommand c) {
        badges.findById(badgeId).orElseThrow(() -> new NotFoundException("La insignia"));
        return badges.save(validated(badgeId, c));
    }

    @Override
    public Badge setBadgeActive(String badgeId, boolean active) {
        Badge b = badges.findById(badgeId).orElseThrow(() -> new NotFoundException("La insignia"));
        return badges.save(new Badge(b.id(), b.name(), b.description(), b.icon(), b.criterion(), b.threshold(),
                b.minReviews(), active));
    }

    // ---------- Internos ----------

    private double value(String userId, BadgeCriterion criterion, UserStats stats) {
        return switch (criterion) {
            case FIRST_SESSION -> stats.completedAsMentor() + stats.completedAsLearner();
            case COMPLETED_SESSIONS -> stats.completedAsMentor();
            case STUDENTS_HELPED -> stats.studentsHelped();
            case RATING_AVERAGE -> 0;
            case ACTIVE_WEEKS -> badges.activeWeeksAsMentor(userId);
        };
    }

    private Set<String> earnedIds(String userId) {
        return badges.findByUser(userId).stream().map(ub -> ub.badge().id()).collect(Collectors.toSet());
    }

    private static String reasonFor(Badge b) {
        String t = number(b.threshold());
        return switch (b.criterion()) {
            case FIRST_SESSION -> "Completaste tu primera sesión.";
            case COMPLETED_SESSIONS -> "Llegaste a " + t + " sesiones completadas como mentor.";
            case STUDENTS_HELPED -> "Apoyaste a " + t + " estudiantes distintos.";
            case RATING_AVERAGE -> "Tu promedio alcanzó " + t.replace('.', ',') + " estrellas con al menos "
                    + (b.minReviews() == null ? 1 : b.minReviews()) + " reseñas.";
            case ACTIVE_WEEKS -> "Impartiste sesiones en " + t + " semanas distintas.";
        };
    }

    private static String number(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    /** Mismas reglas y mensajes que el admin de insignias de la Fase I. */
    private static Badge validated(String id, BadgeCommand c) {
        String name = c.name() == null ? "" : c.name().trim();
        String description = c.description() == null ? "" : c.description().trim();
        String icon = c.icon() == null || c.icon().isBlank() ? "Award" : c.icon().trim();
        BadgeCriterion criterion = WireEnum.find(BadgeCriterion.class, c.criterion()).orElse(null);
        List<FieldIssue> issues = new ArrayList<>();
        if (name.length() < 3 || name.length() > 40) {
            issues.add(new FieldIssue("name", "El nombre debe tener entre 3 y 40 caracteres."));
        }
        if (description.length() < 10 || description.length() > 200) {
            issues.add(new FieldIssue("description", "La descripción debe tener entre 10 y 200 caracteres."));
        }
        if (criterion == null) {
            issues.add(new FieldIssue("criterion", "Selecciona un criterio."));
        }
        boolean rating = criterion == BadgeCriterion.RATING_AVERAGE;
        double max = rating ? 5 : 1000;
        if (c.threshold() == null || c.threshold() <= 0 || c.threshold() > max) {
            issues.add(new FieldIssue("threshold", rating
                    ? "El umbral debe estar entre 0,1 y 5." : "El umbral debe ser un número positivo."));
        }
        if (c.minReviews() != null && c.minReviews() < 1) {
            issues.add(new FieldIssue("minReviews", "El mínimo de reseñas debe ser un entero positivo."));
        }
        ValidationException.throwIfAny(issues);
        Integer minReviews = rating ? (c.minReviews() == null ? 5 : c.minReviews()) : null;
        return new Badge(id, name, description, icon, criterion, c.threshold(), minReviews,
                c.active() == null || c.active());
    }
}
