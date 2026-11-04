package com.skillgrow.core.infrastructure.adapter.out.persistence.repository;

import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.UserEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserEntity, String>, JpaSpecificationExecutor<UserEntity> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByGoogleSub(String googleSub);

    boolean existsByEmail(String email);

    long countByStatus(String status);

    @Query("select count(u) from UserEntity u where size(u.teachSkills) > 0")
    long countMentors();

    @Query("select u.createdAtEpochMs from UserEntity u where u.createdAtEpochMs >= :from")
    List<Long> createdSince(@Param("from") long fromEpochMs);

    @Query("select u.id as id, u.fullName as fullName, u.avatarUrl as avatarUrl from UserEntity u where u.id in :ids")
    List<SummaryView> findSummaries(@Param("ids") Collection<String> ids);

    /** Ids de usuarios cuyo nombre contiene el texto (la columna ignora tildes y mayúsculas). */
    @Query("select u.id from UserEntity u where u.fullName like concat('%', :text, '%')")
    List<String> findIdsByNameContaining(@Param("text") String text);

    interface SummaryView {
        String getId();

        String getFullName();

        String getAvatarUrl();
    }
}
