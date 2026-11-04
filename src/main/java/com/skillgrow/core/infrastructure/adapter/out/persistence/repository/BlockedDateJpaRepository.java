package com.skillgrow.core.infrastructure.adapter.out.persistence.repository;

import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.BlockedDateEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockedDateJpaRepository extends JpaRepository<BlockedDateEntity, String> {

    List<BlockedDateEntity> findByMentorIdOrderByBlockedOn(String mentorId);
}
