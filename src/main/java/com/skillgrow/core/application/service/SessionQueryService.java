package com.skillgrow.core.application.service;

import com.skillgrow.core.domain.exception.NotFoundException;
import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionDetails;
import com.skillgrow.core.domain.model.SessionStatus;
import com.skillgrow.core.domain.model.WireEnum;
import com.skillgrow.core.domain.port.in.SessionQueryUseCase;
import com.skillgrow.core.domain.port.out.SessionRepositoryPort;
import com.skillgrow.core.domain.port.out.SessionRepositoryPort.SessionSearch;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.transaction.annotation.Transactional;

/** Consultas de sesiones del usuario autenticado. */
@Transactional(readOnly = true)
public class SessionQueryService implements SessionQueryUseCase {

    private final SessionRepositoryPort sessions;
    private final SessionAssembler assembler;
    private final Clock clock;

    public SessionQueryService(SessionRepositoryPort sessions, SessionAssembler assembler, Clock clock) {
        this.sessions = sessions;
        this.assembler = assembler;
        this.clock = clock;
    }

    @Override
    public PageResult<SessionDetails> list(String userId, SessionListQuery q, PageQuery page) {
        SessionStatus status = WireEnum.find(SessionStatus.class, q.status()).orElse(null);
        // Las próximas se ordenan de la más cercana a la más lejana; el resto, de la más reciente.
        boolean ascending = "upcoming".equals(q.scope()) || "date:asc".equals(q.sort());
        SessionSearch search = new SessionSearch(userId, q.role(), q.scope(), status, q.search(),
                LocalDate.now(clock), ascending);
        return assembler.details(sessions.findForParticipant(search, page));
    }

    @Override
    public SessionDetails get(String userId, String sessionId) {
        Session s = sessions.findById(sessionId).orElseThrow(() -> new NotFoundException("La sesión"));
        s.requireParticipant(userId);
        return assembler.details(s);
    }

    @Override
    public PageResult<SessionDetails> requests(String userId, String kind, PageQuery page) {
        return assembler.details(sessions.findRequests(userId, "sent".equals(kind), LocalDate.now(clock), page));
    }
}
