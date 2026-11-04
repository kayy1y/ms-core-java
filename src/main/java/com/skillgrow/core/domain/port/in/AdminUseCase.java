package com.skillgrow.core.domain.port.in;

import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.SessionDetails;
import com.skillgrow.core.domain.model.SessionStatus;
import com.skillgrow.core.domain.model.User;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Administración de usuarios y sesiones (solo rol admin). Servicios y reseñas los administra MS2. */
public interface AdminUseCase {

    PageResult<AdminUserRow> users(String search, String role, String status, String sort, PageQuery page);

    /** Suspender cancela las sesiones activas del usuario y avisa a la otra parte. */
    User changeUserStatus(String adminId, String userId, String status, String reason);

    PageResult<SessionDetails> sessions(String status, String modality, String from, String to, String search,
                                        PageQuery page);

    AdminStats stats();

    record AdminUserRow(User user, long completedSessions) {}

    record AdminStats(long totalUsers, long activeUsers, long suspendedUsers, long mentors,
                      long totalSessions, Map<SessionStatus, Long> sessionsByStatus,
                      List<DailyActivity> activityLast30Days) {}

    record DailyActivity(LocalDate date, long sessions, long newUsers) {}
}
