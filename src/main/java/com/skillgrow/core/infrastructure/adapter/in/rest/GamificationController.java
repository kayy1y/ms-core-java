package com.skillgrow.core.infrastructure.adapter.in.rest;

import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.port.in.GamificationUseCase;
import com.skillgrow.core.domain.port.in.GamificationUseCase.BadgeCommand;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AdminDtos.AchievementDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AdminDtos.BadgeActiveRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AdminDtos.BadgeRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.PageResponse;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.BadgeDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.UserBadgeDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import com.skillgrow.core.infrastructure.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
@Tag(name = "Gamificación", description = "Insignias y logros")
public class GamificationController {

    private final GamificationUseCase gamification;

    public GamificationController(GamificationUseCase gamification) {
        this.gamification = gamification;
    }

    @GetMapping("/badges")
    @Operation(summary = "Catálogo de insignias activas")
    public List<BadgeDto> badges() {
        return gamification.activeBadges().stream().map(UserRestMapper::badge).toList();
    }

    @GetMapping("/users/{id}/badges")
    @Operation(summary = "Insignias de un usuario")
    public List<UserBadgeDto> userBadges(@PathVariable String id) {
        return UserRestMapper.badges(gamification.userBadges(id));
    }

    @GetMapping("/me/achievements")
    @Operation(summary = "Mi progreso hacia la próxima insignia de cada criterio")
    public List<AchievementDto> achievements(CurrentUser user) {
        return gamification.achievements(user.id()).stream()
                .map(a -> new AchievementDto(a.criterion().value(), a.current(), a.nextThreshold(), a.nextBadgeId()))
                .toList();
    }

    @GetMapping("/admin/badges")
    @Operation(summary = "Todas las insignias, incluidas las inactivas (admin)")
    public PageResponse<BadgeDto> all(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int pageSize) {
        return PageResponse.of(gamification.allBadges(new PageQuery(page, pageSize)), UserRestMapper::badge);
    }

    @PostMapping("/admin/badges")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear insignia (admin)")
    public BadgeDto create(@RequestBody BadgeRequest body) {
        return UserRestMapper.badge(gamification.createBadge(command(body)));
    }

    @PutMapping("/admin/badges/{id}")
    @Operation(summary = "Editar insignia (admin)")
    public BadgeDto update(@PathVariable String id, @RequestBody BadgeRequest body) {
        return UserRestMapper.badge(gamification.updateBadge(id, command(body)));
    }

    @PatchMapping("/admin/badges/{id}/status")
    @Operation(summary = "Activar o desactivar insignia (admin)")
    public BadgeDto status(@PathVariable String id, @RequestBody BadgeActiveRequest body) {
        return UserRestMapper.badge(gamification.setBadgeActive(id, Boolean.TRUE.equals(body.active())));
    }

    private static BadgeCommand command(BadgeRequest b) {
        return new BadgeCommand(b.name(), b.description(), b.icon(), b.criterion(), b.threshold(), b.minReviews(),
                b.active());
    }
}
