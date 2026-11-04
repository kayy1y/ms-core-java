package com.skillgrow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "blocked_dates")
public class BlockedDateEntity {

    @Id
    public String id;
    @Column(name = "mentor_id")
    public String mentorId;
    @Column(name = "blocked_on")
    public LocalDate blockedOn;
    public String reason;
}
