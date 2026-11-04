package com.skillgrow.core.infrastructure.adapter.out.persistence.repository;

import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.SessionEntity;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionJpaRepository extends JpaRepository<SessionEntity, String>, JpaSpecificationExecutor<SessionEntity> {

    List<SessionEntity> findByServiceIdAndStatusInAndSessionDateBetween(
            String serviceId, Collection<String> statuses, LocalDate from, LocalDate to);

    @Query("""
            select count(s) > 0 from SessionEntity s
            where s.serviceId = :serviceId and s.sessionDate = :date and s.status in :statuses and s.id <> :excludeId
            """)
    boolean existsOtherOn(@Param("serviceId") String serviceId, @Param("date") LocalDate date,
                          @Param("statuses") Collection<String> statuses, @Param("excludeId") String excludeId);

    @Query("""
            select s from SessionEntity s
            where s.status in :statuses and (s.mentorId = :userId or :userId member of s.learnerIds)
            """)
    List<SessionEntity> findByParticipantAndStatusIn(@Param("userId") String userId,
                                                     @Param("statuses") Collection<String> statuses);

    /** Solicitudes que esperan una respuesta del usuario. */
    @Query(value = """
            select s from SessionEntity s
            where s.sessionDate >= :today
              and ((s.status = 'pendiente' and s.mentorId = :userId)
                or (s.status = 'reprogramada' and s.proposedBy <> :userId
                    and (s.mentorId = :userId or :userId member of s.learnerIds)))
            """)
    Page<SessionEntity> findRequestsReceived(@Param("userId") String userId, @Param("today") LocalDate today,
                                             Pageable pageable);

    /** Solicitudes que el usuario envió y siguen sin respuesta. */
    @Query(value = """
            select s from SessionEntity s
            where s.sessionDate >= :today
              and ((s.status = 'pendiente' and :userId member of s.learnerIds)
                or (s.status = 'reprogramada' and s.proposedBy = :userId))
            """)
    Page<SessionEntity> findRequestsSent(@Param("userId") String userId, @Param("today") LocalDate today,
                                         Pageable pageable);

    long countByMentorIdAndStatus(String mentorId, String status);

    @Query("select count(s) from SessionEntity s where s.status = :status and :userId member of s.learnerIds")
    long countAsLearner(@Param("userId") String userId, @Param("status") String status);

    @Query("""
            select count(distinct l) from SessionEntity s join s.learnerIds l
            where s.mentorId = :userId and s.status = :status
            """)
    long countDistinctLearners(@Param("userId") String userId, @Param("status") String status);

    @Query("""
            select s.mentorId, count(s) from SessionEntity s
            where s.status = :status and s.mentorId in :ids group by s.mentorId
            """)
    List<Object[]> countByMentorIn(@Param("ids") Collection<String> ids, @Param("status") String status);

    @Query("select s.status, count(s) from SessionEntity s group by s.status")
    List<Object[]> countGroupedByStatus();

    @Query("""
            select s.sessionDate, count(s) from SessionEntity s
            where s.sessionDate between :from and :to group by s.sessionDate
            """)
    List<Object[]> countGroupedByDate(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("""
            select s.sessionDate from SessionEntity s where s.mentorId = :userId and s.status = 'completada'
            """)
    List<LocalDate> completedDatesAsMentor(@Param("userId") String userId);
}
