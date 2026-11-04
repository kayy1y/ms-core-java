package com.skillgrow.core.infrastructure.adapter.in.rest.dto;

import java.util.List;
import java.util.Map;

/** Respuestas de usuario. Fechas en epoch ms; enums con el valor del contrato ("user", "intermedio"…). */
public final class UserDtos {

    private UserDtos() {}

    public record UserResponse(String id, String fullName, String email, String avatarUrl, String bio, String role,
                               String status, AcademicDto academic, ProfessionalDto professional,
                               List<String> knowledge, List<TeachSkillDto> teachSkills,
                               List<InterestDto> learningInterests, StatsDto stats, List<UserBadgeDto> badges,
                               Map<String, Boolean> emailPreferences, long createdAtEpochMs) {}

    /** Perfil público: sin correo ni preferencias. */
    public record PublicUserResponse(String id, String fullName, String avatarUrl, String bio, String role,
                                     String status, AcademicDto academic, ProfessionalDto professional,
                                     List<String> knowledge, List<TeachSkillDto> teachSkills,
                                     List<InterestDto> learningInterests, StatsDto stats,
                                     List<UserBadgeDto> badges, long createdAtEpochMs) {}

    public record AcademicDto(String institution, String career, String level) {}

    public record ProfessionalDto(String occupation, String experience) {}

    public record TeachSkillDto(String skillId, String name, String level) {}

    public record InterestDto(String skillId, String name) {}

    /** ratingAverage/ratingCount: MS2 es el dueño de las reseñas (GET /dominio/v1/users/{id}/rating-summary). */
    public record StatsDto(double ratingAverage, long ratingCount, long completedAsMentor, long completedAsLearner,
                           long studentsHelped) {}

    public record BadgeDto(String id, String name, String description, String icon, String criterion,
                           double threshold, Integer minReviews, boolean active) {}

    public record UserBadgeDto(BadgeDto badge, long earnedAtEpochMs, String reason) {}

    public record UserSummaryDto(String id, String fullName, String avatarUrl, double ratingAverage,
                                 long ratingCount) {}

    public record AuthResponse(String token, UserResponse user) {}

    public record AdminUserRowDto(String id, String fullName, String email, String avatarUrl, String role,
                                  String status, String institution, long activeServices, long completedSessions,
                                  long createdAtEpochMs) {}

    public record UserStatusDto(String id, String status) {}
}
