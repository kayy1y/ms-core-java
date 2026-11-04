package com.skillgrow.core.domain.port.out;

import com.skillgrow.core.domain.model.Modality;
import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionStatus;
import com.skillgrow.core.domain.model.UserStats;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Persistencia de sesiones. save() traduce los choques de concurrencia a errores de dominio. */
public interface SessionRepositoryPort {

    Optional<Session> findById(String id);

    /**
     * Guarda la sesión. Lanza DayUnavailableException si el índice único detecta otra sesión activa del
     * servicio ese día, y ConflictException si otra persona modificó la sesión al mismo tiempo.
     */
    Session save(Session session);

    /** Sesiones activas del servicio entre dos fechas (inclusive), para calcular cupos. */
    List<Session> findActiveByService(String serviceId, LocalDate from, LocalDate to);

    /** Hay otra sesión del servicio confirmada o reprogramada ese día. */
    boolean existsConfirmedOn(String serviceId, LocalDate date, String excludeSessionId);

    /** Sesiones activas en las que participa el usuario (como mentor o aprendiz). */
    List<Session> findActiveByParticipant(String userId);

    PageResult<Session> findForParticipant(SessionSearch search, PageQuery page);

    /** sent=false: las que esperan acción del usuario; sent=true: las que él envió. */
    PageResult<Session> findRequests(String userId, boolean sent, LocalDate today, PageQuery page);

    PageResult<Session> findForAdmin(SessionStatus status, Modality modality, LocalDate from, LocalDate to,
                                     String search, PageQuery page);

    UserStats statsFor(String userId);

    Map<String, Long> completedAsMentorByUser(List<String> userIds);

    Map<SessionStatus, Long> countByStatus();

    Map<LocalDate, Long> countByDateBetween(LocalDate from, LocalDate to);

    /**
     * Filtros de GET /sessions.
     *
     * @param role   learner | mentor | all
     * @param scope  upcoming | past | cancelled | null
     */
    record SessionSearch(String userId, String role, String scope, SessionStatus status, String search,
                         LocalDate today, boolean ascending) {}
}
