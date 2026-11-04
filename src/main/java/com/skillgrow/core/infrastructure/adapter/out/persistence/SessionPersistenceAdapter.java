package com.skillgrow.core.infrastructure.adapter.out.persistence;

import static com.skillgrow.core.infrastructure.adapter.out.persistence.SessionSpecifications.ACTIVE;

import com.skillgrow.core.domain.exception.ConflictException;
import com.skillgrow.core.domain.exception.DayUnavailableException;
import com.skillgrow.core.domain.model.Modality;
import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionStatus;
import com.skillgrow.core.domain.model.UserStats;
import com.skillgrow.core.domain.model.WireEnum;
import com.skillgrow.core.domain.port.out.SessionRepositoryPort;
import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.SessionEntity;
import com.skillgrow.core.infrastructure.adapter.out.persistence.repository.SessionJpaRepository;
import com.skillgrow.core.infrastructure.adapter.out.persistence.repository.UserJpaRepository;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

/** Implementa SessionRepositoryPort sobre Azure SQL. */
@Repository
class SessionPersistenceAdapter implements SessionRepositoryPort {

    private static final String ACTIVE_DAY_INDEX = "ux_sessions_service_day_active";
    private static final String COMPLETED = "completada";

    private final SessionJpaRepository repo;
    private final UserJpaRepository users;

    SessionPersistenceAdapter(SessionJpaRepository repo, UserJpaRepository users) {
        this.repo = repo;
        this.users = users;
    }

    @Override
    public Optional<Session> findById(String id) {
        return repo.findById(id).map(SessionPersistenceMapper::toDomain);
    }

    @Override
    public Session save(Session session) {
        SessionEntity entity = repo.findById(session.id()).orElseGet(SessionEntity::new);
        if (entity.id != null && entity.version != session.version()) {
            throw conflict();
        }
        SessionPersistenceMapper.copyInto(session, entity);
        try {
            return SessionPersistenceMapper.toDomain(repo.saveAndFlush(entity));
        } catch (DataIntegrityViolationException ex) {
            if (mentions(ex, ACTIVE_DAY_INDEX)) {
                throw new DayUnavailableException();
            }
            throw ex;
        } catch (OptimisticLockingFailureException ex) {
            throw conflict();
        }
    }

    @Override
    public List<Session> findActiveByService(String serviceId, LocalDate from, LocalDate to) {
        return repo.findByServiceIdAndStatusInAndSessionDateBetween(serviceId, ACTIVE, from, to).stream()
                .map(SessionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsConfirmedOn(String serviceId, LocalDate date, String excludeSessionId) {
        return repo.existsOtherOn(serviceId, date, List.of("confirmada", "reprogramada"),
                excludeSessionId == null ? "" : excludeSessionId);
    }

    @Override
    public List<Session> findActiveByParticipant(String userId) {
        return repo.findByParticipantAndStatusIn(userId, ACTIVE).stream()
                .map(SessionPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public PageResult<Session> findForParticipant(SessionSearch s, PageQuery page) {
        Specification<SessionEntity> spec = switch (s.role() == null ? "all" : s.role()) {
            case "mentor" -> SessionSpecifications.mentor(s.userId());
            case "learner" -> SessionSpecifications.learner(s.userId());
            default -> SessionSpecifications.participant(s.userId());
        };
        if (s.scope() != null) {
            spec = spec.and(SessionSpecifications.scope(s.scope(), s.today()));
        }
        if (s.status() != null) {
            spec = spec.and(SessionSpecifications.status(s.status().value()));
        }
        spec = withSearch(spec, s.search());
        Sort.Direction dir = s.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(dir, "sessionDate").and(Sort.by(dir, "createdAtEpochMs"));
        return Pages.toResult(repo.findAll(spec, Pages.of(page, sort)), page, SessionPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<Session> findRequests(String userId, boolean sent, LocalDate today, PageQuery page) {
        Sort sort = Sort.by("sessionDate").and(Sort.by("createdAtEpochMs"));
        var result = sent
                ? repo.findRequestsSent(userId, today, Pages.of(page, sort))
                : repo.findRequestsReceived(userId, today, Pages.of(page, sort));
        return Pages.toResult(result, page, SessionPersistenceMapper::toDomain);
    }

    @Override
    public PageResult<Session> findForAdmin(SessionStatus status, Modality modality, LocalDate from, LocalDate to,
                                            String search, PageQuery page) {
        Specification<SessionEntity> spec = (root, q, cb) -> cb.conjunction();
        if (status != null) {
            spec = spec.and(SessionSpecifications.status(status.value()));
        }
        if (modality != null) {
            spec = spec.and(SessionSpecifications.modality(modality.value()));
        }
        if (from != null) {
            spec = spec.and(SessionSpecifications.dateFrom(from));
        }
        if (to != null) {
            spec = spec.and(SessionSpecifications.dateTo(to));
        }
        spec = withSearch(spec, search);
        Sort sort = Sort.by(Sort.Direction.DESC, "sessionDate").and(Sort.by(Sort.Direction.DESC, "createdAtEpochMs"));
        return Pages.toResult(repo.findAll(spec, Pages.of(page, sort)), page, SessionPersistenceMapper::toDomain);
    }

    @Override
    public UserStats statsFor(String userId) {
        return new UserStats(
                repo.countByMentorIdAndStatus(userId, COMPLETED),
                repo.countAsLearner(userId, COMPLETED),
                repo.countDistinctLearners(userId, COMPLETED));
    }

    @Override
    public Map<String, Long> completedAsMentorByUser(List<String> userIds) {
        Map<String, Long> out = new HashMap<>();
        if (!userIds.isEmpty()) {
            for (Object[] row : repo.countByMentorIn(userIds, COMPLETED)) {
                out.put((String) row[0], (Long) row[1]);
            }
        }
        return out;
    }

    @Override
    public Map<SessionStatus, Long> countByStatus() {
        Map<SessionStatus, Long> out = new EnumMap<>(SessionStatus.class);
        for (SessionStatus st : SessionStatus.values()) {
            out.put(st, 0L);
        }
        for (Object[] row : repo.countGroupedByStatus()) {
            WireEnum.find(SessionStatus.class, (String) row[0]).ifPresent(st -> out.put(st, (Long) row[1]));
        }
        return out;
    }

    @Override
    public Map<LocalDate, Long> countByDateBetween(LocalDate from, LocalDate to) {
        Map<LocalDate, Long> out = new HashMap<>();
        for (Object[] row : repo.countGroupedByDate(from, to)) {
            out.put((LocalDate) row[0], (Long) row[1]);
        }
        return out;
    }

    private Specification<SessionEntity> withSearch(Specification<SessionEntity> spec, String search) {
        if (search == null || search.isBlank()) {
            return spec;
        }
        String text = search.trim();
        return spec.and(SessionSpecifications.search(text, users.findIdsByNameContaining(text)));
    }

    private static boolean mentions(Throwable ex, String text) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(text)) {
                return true;
            }
        }
        return false;
    }

    private static ConflictException conflict() {
        return new ConflictException("La sesión cambió mientras la editabas. Vuelve a cargarla e inténtalo de nuevo.");
    }
}
