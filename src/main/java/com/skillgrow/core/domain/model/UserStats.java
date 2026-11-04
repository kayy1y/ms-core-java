package com.skillgrow.core.domain.model;

/** Estadísticas que MS1 puede calcular con sus sesiones. ratingAverage/ratingCount vienen de MS2. */
public record UserStats(long completedAsMentor, long completedAsLearner, long studentsHelped) {}
