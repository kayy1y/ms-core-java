package com.skillgrow.core.domain.model;

/** Insignia del catálogo. icon es el nombre de un ícono de lucide (lo pinta el frontend). */
public record Badge(String id, String name, String description, String icon, BadgeCriterion criterion,
                    double threshold, Integer minReviews, boolean active) {}
