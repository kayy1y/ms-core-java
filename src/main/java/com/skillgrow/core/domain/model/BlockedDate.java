package com.skillgrow.core.domain.model;

import java.time.LocalDate;

/** Fecha que el mentor bloqueó (vacaciones, exámenes…). */
public record BlockedDate(String id, String mentorId, LocalDate date, String reason) {}
