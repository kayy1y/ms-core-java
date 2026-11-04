package com.skillgrow.core.domain.model;

/** Habilidad que el mentor enseña, con su nivel. */
public record TeachSkill(String skillId, String name, SkillLevel level) {}
