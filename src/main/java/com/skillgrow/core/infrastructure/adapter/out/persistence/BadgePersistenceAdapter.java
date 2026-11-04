package com.skillgrow.core.infrastructure.adapter.out.persistence;

import com.skillgrow.core.domain.model.Badge;
import com.skillgrow.core.domain.model.BadgeCriterion;
import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.UserBadge;
import com.skillgrow.core.domain.model.WireEnum;
import com.skillgrow.core.domain.port.out.BadgeRepositoryPort;
import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.BadgeEntity;
import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.UserBadgeEntity;
import com.skillgrow.core.infrastructure.adapter.out.persistence.repository.BadgeJpaRepository;
import com.skillgrow.core.infrastructure.adapter.out.persistence.repository.SessionJpaRepository;
import com.skillgrow.core.infrastructure.adapter.out.persistence.repository.UserBadgeJpaRepository;
import java.time.temporal.IsoFields;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/** Implementa BadgeRepositoryPort sobre Azure SQL. */
@Repository
class BadgePersistenceAdapter implements BadgeRepositoryPort {

    private final BadgeJpaRepository badges;
    private final UserBadgeJpaRepository userBadges;
    private final SessionJpaRepository sessions;

    BadgePersistenceAdapter(BadgeJpaRepository badges, UserBadgeJpaRepository userBadges, SessionJpaRepository sessions) {
        this.badges = badges;
        this.userBadges = userBadges;
        this.sessions = sessions;
    }

    @Override
    public List<Badge> findActive() {
        return badges.findByActiveTrueOrderByThreshold().stream().map(BadgePersistenceAdapter::toDomain).toList();
    }

    @Override
    public PageResult<Badge> findAll(PageQuery page) {
        return Pages.toResult(badges.findAll(Pages.of(page, Sort.by("name"))), page, BadgePersistenceAdapter::toDomain);
    }

    @Override
    public Optional<Badge> findById(String id) {
        return badges.findById(id).map(BadgePersistenceAdapter::toDomain);
    }

    @Override
    public Badge save(Badge b) {
        BadgeEntity e = badges.findById(b.id()).orElseGet(BadgeEntity::new);
        e.id = b.id();
        e.name = b.name();
        e.description = b.description();
        e.icon = b.icon();
        e.criterion = b.criterion().value();
        e.threshold = b.threshold();
        e.minReviews = b.minReviews();
        e.active = b.active();
        return toDomain(badges.save(e));
    }

    @Override
    public List<UserBadge> findByUser(String userId) {
        List<UserBadgeEntity> earned = userBadges.findByIdUserIdOrderByEarnedAtEpochMsDesc(userId);
        Map<String, Badge> byId = badges.findAllById(earned.stream().map(u -> u.id.badgeId()).toList()).stream()
                .map(BadgePersistenceAdapter::toDomain)
                .collect(Collectors.toMap(Badge::id, Function.identity()));
        return earned.stream()
                .filter(u -> byId.containsKey(u.id.badgeId()))
                .map(u -> new UserBadge(byId.get(u.id.badgeId()), u.earnedAtEpochMs, u.reason))
                .toList();
    }

    @Override
    public void award(String userId, String badgeId, long atEpochMs, String reason) {
        UserBadgeEntity e = new UserBadgeEntity();
        e.id = new UserBadgeEntity.Key(userId, badgeId);
        e.earnedAtEpochMs = atEpochMs;
        e.reason = reason;
        userBadges.save(e);
    }

    @Override
    public long activeWeeksAsMentor(String userId) {
        return sessions.completedDatesAsMentor(userId).stream()
                .map(d -> d.get(IsoFields.WEEK_BASED_YEAR) + "-" + d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR))
                .distinct()
                .count();
    }

    private static Badge toDomain(BadgeEntity e) {
        return new Badge(e.id, e.name, e.description, e.icon,
                WireEnum.find(BadgeCriterion.class, e.criterion).orElse(BadgeCriterion.COMPLETED_SESSIONS),
                e.threshold, e.minReviews, e.active);
    }
}
