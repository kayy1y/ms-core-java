package com.skillgrow.core.infrastructure.adapter.out.persistence.repository;

import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.UserBadgeEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBadgeJpaRepository extends JpaRepository<UserBadgeEntity, UserBadgeEntity.Key> {

    List<UserBadgeEntity> findByIdUserIdOrderByEarnedAtEpochMsDesc(String userId);
}
