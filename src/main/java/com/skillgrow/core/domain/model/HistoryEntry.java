package com.skillgrow.core.domain.model;

/** Cada cambio de estado de una sesión: quién, cuándo y por qué. */
public record HistoryEntry(SessionStatus status, long atEpochMs, String byUserId, String comment) {}
