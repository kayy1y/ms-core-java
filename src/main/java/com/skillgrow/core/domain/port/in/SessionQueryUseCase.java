package com.skillgrow.core.domain.port.in;

import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.SessionDetails;

/** Consultas de sesiones del usuario autenticado. */
public interface SessionQueryUseCase {

    PageResult<SessionDetails> list(String userId, SessionListQuery query, PageQuery page);

    /** 403 si el usuario no participa en la sesión. */
    SessionDetails get(String userId, String sessionId);

    /** kind = received (esperan mi acción, por defecto) | sent (las que yo envié). */
    PageResult<SessionDetails> requests(String userId, String kind, PageQuery page);

    /** role = learner | mentor | all · scope = upcoming | past | cancelled · sort = date:asc|desc. */
    record SessionListQuery(String role, String scope, String status, String search, String sort) {}
}
