package com.skillgrow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;

@Entity
@Table(name = "user_badges")
public class UserBadgeEntity {

    @EmbeddedId
    public Key id;
    @Column(name = "earned_at_epoch_ms")
    public long earnedAtEpochMs;
    public String reason;

    @Embeddable
    public record Key(@Column(name = "user_id") String userId,
                      @Column(name = "badge_id") String badgeId) implements Serializable {}
}
