package com.skillgrow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class InterestEmbeddable {

    @Column(name = "skill_id")
    public String skillId;
    public String name;

    protected InterestEmbeddable() {}

    public InterestEmbeddable(String skillId, String name) {
        this.skillId = skillId;
        this.name = name;
    }
}
