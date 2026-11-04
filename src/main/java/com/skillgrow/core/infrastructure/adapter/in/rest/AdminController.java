package com.skillgrow.core.infrastructure.adapter.in.rest;

import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.port.in.AdminUseCase;
import com.skillgrow.core.domain.port.in.AdminUseCase.AdminUserRow;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AdminDtos.ActivityDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AdminDtos.AdminStatsResponse;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AdminDtos.SessionsStats;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AdminDtos.StatusChangeRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AdminDtos.UsersStats;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.PageResponse;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.SessionDtos.SessionResponse;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.AdminUserRowDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.UserStatusDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.mapper.SessionRestMapper;
import com.skillgrow.core.infrastructure.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** /v1/admin/**: el AdminInterceptor exige rol admin (403 si no). */
@RestController
@RequestMapping("/v1/admin")
@Tag(name = "Administración", description = "Usuarios, sesiones y estadísticas (rol admin)")
public class AdminController {

    private final AdminUseCase admin;

    public AdminController(AdminUseCase admin) {
        this.admin = admin;
    }

    @GetMapping("/users")
    @Operation(summary = "Usuarios con búsqueda y filtros")
    public PageResponse<AdminUserRowDto> users(@RequestParam(required = false) String search,
                                               @RequestParam(required = false) String role,
                                               @RequestParam(required = false) String status,
                                               @RequestParam(required = false) String sort,
                                               @RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int pageSize) {
        return PageResponse.of(admin.users(search, role, status, sort, new PageQuery(page, pageSize)),
                AdminController::row);
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Suspender o reactivar (suspender cancela sus sesiones activas)")
    public UserStatusDto changeStatus(CurrentUser user, @PathVariable String id, @RequestBody StatusChangeRequest body) {
        User u = admin.changeUserStatus(user.id(), id, body.status(), body.reason());
        return new UserStatusDto(u.id(), u.status().value());
    }

    @GetMapping("/sessions")
    @Operation(summary = "Todas las sesiones (sin enlaces ni lugares)")
    public PageResponse<SessionResponse> sessions(@RequestParam(required = false) String status,
                                                  @RequestParam(required = false) String modality,
                                                  @RequestParam(required = false) String from,
                                                  @RequestParam(required = false) String to,
                                                  @RequestParam(required = false) String search,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int pageSize) {
        return PageResponse.of(admin.sessions(status, modality, from, to, search, new PageQuery(page, pageSize)),
                d -> SessionRestMapper.toResponse(d, null));
    }

    @GetMapping("/stats")
    @Operation(summary = "Usuarios, sesiones por estado y actividad de 30 días")
    public AdminStatsResponse stats() {
        var s = admin.stats();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        s.sessionsByStatus().forEach((k, v) -> byStatus.put(k.value(), v));
        return new AdminStatsResponse(
                new UsersStats(s.totalUsers(), s.activeUsers(), s.suspendedUsers(), s.mentors()),
                new SessionsStats(s.totalSessions(), byStatus),
                s.activityLast30Days().stream()
                        .map(a -> new ActivityDto(a.date().toString(), a.sessions(), a.newUsers()))
                        .toList());
    }

    /** activeServices lo conoce MS2; aquí sale en 0. */
    private static AdminUserRowDto row(AdminUserRow r) {
        User u = r.user();
        return new AdminUserRowDto(u.id(), u.fullName(), u.email(), u.avatarUrl(), u.role().value(),
                u.status().value(), u.academic() == null ? null : u.academic().institution(), 0,
                r.completedSessions(), u.createdAtEpochMs());
    }
}
