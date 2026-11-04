package com.skillgrow.core.infrastructure.adapter.in.rest.dto;

import java.util.List;
import java.util.Map;

public final class AdminDtos {

    private AdminDtos() {}

    /** services, reviews y topAreas los completa la web con GET /dominio/v1/admin/stats (MS2). */
    public record AdminStatsResponse(UsersStats users, SessionsStats sessions, List<ActivityDto> activityLast30Days) {}

    public record UsersStats(long total, long active, long suspended, long mentors) {}

    public record SessionsStats(long total, Map<String, Long> byStatus) {}

    public record ActivityDto(String date, long sessions, long newUsers) {}

    public record StatusChangeRequest(String status, String reason) {}

    public record BadgeRequest(String name, String description, String icon, String criterion, Double threshold,
                               Integer minReviews, Boolean active) {}

    public record BadgeActiveRequest(Boolean active) {}

    public record AchievementDto(String criterion, double current, Double nextThreshold, String nextBadgeId) {}
}
