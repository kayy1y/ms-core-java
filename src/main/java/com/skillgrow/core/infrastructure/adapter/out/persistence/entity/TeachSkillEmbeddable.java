package com.skillgrow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class TeachSkillEmbeddable {

    @Column(name = "skill_id")
    public String skillId;
    public String name;
    public String level;

    protected TeachSkillEmbeddable() {}

    public TeachSkillEmbeddable(String skillId, String name, String level) {
        this.skillId = skillId;
        this.name = name;
        this.level = level;
    }
}
