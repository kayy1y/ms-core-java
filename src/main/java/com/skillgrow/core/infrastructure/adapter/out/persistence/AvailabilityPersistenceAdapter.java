package com.skillgrow.core.infrastructure.adapter.out.persistence;

import com.skillgrow.core.domain.model.AvailabilityRule;
import com.skillgrow.core.domain.model.BlockedDate;
import com.skillgrow.core.domain.port.out.AvailabilityRepositoryPort;
import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.AvailabilityRuleEntity;
import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.BlockedDateEntity;
import com.skillgrow.core.infrastructure.adapter.out.persistence.repository.AvailabilityRuleJpaRepository;
import com.skillgrow.core.infrastructure.adapter.out.persistence.repository.BlockedDateJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/** Implementa AvailabilityRepositoryPort sobre Azure SQL. */
@Repository
class AvailabilityPersistenceAdapter implements AvailabilityRepositoryPort {

    private final AvailabilityRuleJpaRepository rules;
    private final BlockedDateJpaRepository blocked;

    AvailabilityPersistenceAdapter(AvailabilityRuleJpaRepository rules, BlockedDateJpaRepository blocked) {
        this.rules = rules;
        this.blocked = blocked;
    }

    @Override
    public List<AvailabilityRule> findRules(String mentorId) {
        return rules.findByMentorIdOrderByDayOfWeek(mentorId).stream().map(AvailabilityPersistenceAdapter::toDomain).toList();
    }

    @Override
    public Optional<AvailabilityRule> findRule(String ruleId) {
        return rules.findById(ruleId).map(AvailabilityPersistenceAdapter::toDomain);
    }

    @Override
    public AvailabilityRule saveRule(AvailabilityRule rule) {
        AvailabilityRuleEntity e = rules.findById(rule.id()).orElseGet(AvailabilityRuleEntity::new);
        e.id = rule.id();
        e.mentorId = rule.mentorId();
        e.dayOfWeek = rule.dayOfWeek();
        e.serviceIds.clear();
        e.serviceIds.addAll(rule.serviceIds());
        return toDomain(rules.save(e));
    }

    @Override
    public void deleteRule(String ruleId) {
        rules.deleteById(ruleId);
    }

    @Override
    public List<BlockedDate> findBlockedDates(String mentorId) {
        return blocked.findByMentorIdOrderByBlockedOn(mentorId).stream().map(AvailabilityPersistenceAdapter::toDomain).toList();
    }

    @Override
    public Optional<BlockedDate> findBlockedDate(String id) {
        return blocked.findById(id).map(AvailabilityPersistenceAdapter::toDomain);
    }

    @Override
    public BlockedDate saveBlockedDate(BlockedDate b) {
        BlockedDateEntity e = new BlockedDateEntity();
        e.id = b.id();
        e.mentorId = b.mentorId();
        e.blockedOn = b.date();
        e.reason = b.reason();
        return toDomain(blocked.save(e));
    }

    @Override
    public void deleteBlockedDate(String id) {
        blocked.deleteById(id);
    }

    private static AvailabilityRule toDomain(AvailabilityRuleEntity e) {
        return new AvailabilityRule(e.id, e.mentorId, e.dayOfWeek, e.serviceIds);
    }

    private static BlockedDate toDomain(BlockedDateEntity e) {
        return new BlockedDate(e.id, e.mentorId, e.blockedOn, e.reason);
    }
}
