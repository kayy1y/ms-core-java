package com.skillgrow.core.infrastructure.adapter.out.persistence.repository;

import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.BadgeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeJpaRepository extends JpaRepository<BadgeEntity, String> {

    List<BadgeEntity> findByActiveTrueOrderByThreshold();
}
