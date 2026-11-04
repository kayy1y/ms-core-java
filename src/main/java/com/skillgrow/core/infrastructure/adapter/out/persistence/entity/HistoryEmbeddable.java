package com.skillgrow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class HistoryEmbeddable {

    public String status;
    @Column(name = "at_epoch_ms")
    public long atEpochMs;
    @Column(name = "by_user_id")
    public String byUserId;
    public String comment;

    protected HistoryEmbeddable() {}

    public HistoryEmbeddable(String status, long atEpochMs, String byUserId, String comment) {
        this.status = status;
        this.atEpochMs = atEpochMs;
        this.byUserId = byUserId;
        this.comment = comment;
    }
}
