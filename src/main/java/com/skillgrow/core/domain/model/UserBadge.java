package com.skillgrow.core.domain.model;

/** Insignia que ganó un usuario, con el momento y el motivo. */
public record UserBadge(Badge badge, long earnedAtEpochMs, String reason) {}
