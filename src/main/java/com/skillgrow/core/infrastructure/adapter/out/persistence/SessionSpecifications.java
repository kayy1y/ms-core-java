package com.skillgrow.core.infrastructure.adapter.out.persistence;

import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.SessionEntity;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Filtros reutilizables para las listas de sesiones (GET /sessions y GET /admin/sessions). */
final class SessionSpecifications {

    static final List<String> ACTIVE = List.of("pendiente", "confirmada", "reprogramada");

    private SessionSpecifications() {}

    static Specification<SessionEntity> mentor(String userId) {
        return (root, q, cb) -> cb.equal(root.get("mentorId"), userId);
    }

    static Specification<SessionEntity> learner(String userId) {
        return (root, q, cb) -> cb.isMember(userId, root.<Collection<String>>get("learnerIds"));
    }

    static Specification<SessionEntity> participant(String userId) {
        return mentor(userId).or(learner(userId));
    }

    static Specification<SessionEntity> status(String status) {
        return (root, q, cb) -> cb.equal(root.get("status"), status);
    }

    static Specification<SessionEntity> modality(String modality) {
        return (root, q, cb) -> cb.equal(root.get("modality"), modality);
    }

    static Specification<SessionEntity> dateFrom(LocalDate from) {
        return (root, q, cb) -> cb.greaterThanOrEqualTo(root.get("sessionDate"), from);
    }

    static Specification<SessionEntity> dateTo(LocalDate to) {
        return (root, q, cb) -> cb.lessThanOrEqualTo(root.get("sessionDate"), to);
    }

    /** upcoming: activas desde hoy · past: completadas o activas que ya pasaron · cancelled. */
    static Specification<SessionEntity> scope(String scope, LocalDate today) {
        return (root, q, cb) -> switch (scope) {
            case "upcoming" -> cb.and(root.get("status").in(ACTIVE),
                    cb.greaterThanOrEqualTo(root.get("sessionDate"), today));
            case "past" -> cb.or(cb.equal(root.get("status"), "completada"),
                    cb.and(root.get("status").in(ACTIVE), cb.lessThan(root.get("sessionDate"), today)));
            case "cancelled" -> cb.equal(root.get("status"), "cancelada");
            default -> cb.conjunction();
        };
    }

    /**
     * Texto en el título del servicio o en el nombre de algún participante. Los ids de usuarios que
     * coinciden por nombre se buscan antes (la columna ignora tildes).
     */
    static Specification<SessionEntity> search(String text, List<String> matchingUserIds) {
        return (root, q, cb) -> {
            Predicate byTitle = cb.like(root.get("serviceTitle"), "%" + text + "%");
            if (matchingUserIds.isEmpty()) {
                return byTitle;
            }
            return cb.or(byTitle, root.get("mentorId").in(matchingUserIds),
                    learnerIn(root, q.subquery(String.class), cb, matchingUserIds));
        };
    }

    private static Predicate learnerIn(Root<SessionEntity> root, Subquery<String> sub, CriteriaBuilder cb,
                                       List<String> ids) {
        Root<SessionEntity> s2 = sub.from(SessionEntity.class);
        Expression<String> learner = s2.join("learnerIds");
        sub.select(s2.get("id")).where(cb.equal(s2.get("id"), root.get("id")), learner.in(ids));
        return cb.exists(sub);
    }
}
