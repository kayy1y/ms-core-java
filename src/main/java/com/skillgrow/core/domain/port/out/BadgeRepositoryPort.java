package com.skillgrow.core.domain.port.out;

import com.skillgrow.core.domain.model.Badge;
import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.UserBadge;
import java.util.List;
import java.util.Optional;

/** Catálogo de insignias y las que ganó cada usuario. */
public interface BadgeRepositoryPort {

    List<Badge> findActive();

    PageResult<Badge> findAll(PageQuery page);

    Optional<Badge> findById(String id);

    Badge save(Badge badge);

    List<UserBadge> findByUser(String userId);

    void award(String userId, String badgeId, long atEpochMs, String reason);

    /** Semanas distintas (año-semana ISO) en que el usuario completó sesiones como mentor. */
    long activeWeeksAsMentor(String userId);
}
