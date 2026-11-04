package com.skillgrow.core.domain.port.in;

import com.skillgrow.core.domain.model.Badge;
import com.skillgrow.core.domain.model.BadgeCriterion;
import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.UserBadge;
import java.util.List;

/** Insignias y logros. Se evalúan al completar una sesión. */
public interface GamificationUseCase {

    List<Badge> activeBadges();

    List<UserBadge> userBadges(String userId);

    List<AchievementProgress> achievements(String userId);

    PageResult<Badge> allBadges(PageQuery page);

    Badge createBadge(BadgeCommand command);

    Badge updateBadge(String badgeId, BadgeCommand command);

    Badge setBadgeActive(String badgeId, boolean active);

    /** Revisa los criterios y otorga las insignias nuevas; devuelve las ganadas ahora. */
    List<Badge> evaluate(String userId, long now);

    record AchievementProgress(BadgeCriterion criterion, double current, Double nextThreshold, String nextBadgeId) {}

    record BadgeCommand(String name, String description, String icon, String criterion, Double threshold,
                        Integer minReviews, Boolean active) {}
}
