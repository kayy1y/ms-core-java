package com.skillgrow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "badges")
public class BadgeEntity {

    @Id
    public String id;
    public String name;
    public String description;
    public String icon;
    public String criterion;
    public double threshold;
    @Column(name = "min_reviews")
    public Integer minReviews;
    public boolean active;
}
