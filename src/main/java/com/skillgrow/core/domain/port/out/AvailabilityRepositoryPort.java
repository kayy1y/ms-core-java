package com.skillgrow.core.domain.port.out;

import com.skillgrow.core.domain.model.AvailabilityRule;
import com.skillgrow.core.domain.model.BlockedDate;
import java.util.List;
import java.util.Optional;

/** Días de atención y fechas bloqueadas de cada mentor. */
public interface AvailabilityRepositoryPort {

    List<AvailabilityRule> findRules(String mentorId);

    Optional<AvailabilityRule> findRule(String ruleId);

    AvailabilityRule saveRule(AvailabilityRule rule);

    void deleteRule(String ruleId);

    List<BlockedDate> findBlockedDates(String mentorId);

    Optional<BlockedDate> findBlockedDate(String id);

    BlockedDate saveBlockedDate(BlockedDate blockedDate);

    void deleteBlockedDate(String id);
}
