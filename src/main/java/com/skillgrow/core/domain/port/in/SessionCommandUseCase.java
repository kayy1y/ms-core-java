package com.skillgrow.core.domain.port.in;

import com.skillgrow.core.domain.model.AcceptInput;
import com.skillgrow.core.domain.model.SessionDetails;

/** Acciones que cambian una sesión. Cada una publica su evento hacia MS3 después del commit. */
public interface SessionCommandUseCase {

    /** Reserva (o se suma a la sesión grupal de ese día). date en YYYY-MM-DD. */
    SessionDetails create(String userId, CreateSessionCommand command);

    SessionDetails accept(String userId, String sessionId, AcceptInput input);

    SessionDetails reject(String userId, String sessionId, String comment);

    SessionDetails reschedule(String userId, String sessionId, String date, String comment);

    SessionDetails cancel(String userId, String sessionId, String comment);

    SessionDetails complete(String userId, String sessionId);

    SessionDetails updateMeeting(String userId, String sessionId, String platform, String otherPlatformName,
                                 String meetingLink);

    SessionDetails updateLocation(String userId, String sessionId, String name, String address, String notes);

    record CreateSessionCommand(String serviceId, String date, String modality, String learnerNote) {}
}
