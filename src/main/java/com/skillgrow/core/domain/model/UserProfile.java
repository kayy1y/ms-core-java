package com.skillgrow.core.domain.model;

import java.util.List;

/** Usuario con sus estadísticas e insignias: lo que devuelve GET /me y GET /users/{id}. */
public record UserProfile(User user, UserStats stats, List<UserBadge> badges) {}
