package com.skillgrow.core.infrastructure.adapter.out.persistence.repository;

import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.AvailabilityRuleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityRuleJpaRepository extends JpaRepository<AvailabilityRuleEntity, String> {

    List<AvailabilityRuleEntity> findByMentorIdOrderByDayOfWeek(String mentorId);
}
