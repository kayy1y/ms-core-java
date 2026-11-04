package com.skillgrow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Fila de sessions + sus aprendices e historial. @Version da la concurrencia optimista. */
@Entity
@Table(name = "sessions")
public class SessionEntity {

    @Id
    public String id;
    @Column(name = "service_id")
    public String serviceId;
    @Column(name = "service_title")
    public String serviceTitle;
    @Column(name = "service_duration_minutes")
    public int serviceDurationMinutes;
    @Column(name = "service_type")
    public String serviceType;
    @Column(name = "service_capacity")
    public int serviceCapacity;
    @Column(name = "service_modality")
    public String serviceModality;
    @Column(name = "mentor_id")
    public String mentorId;
    @Column(name = "session_date")
    public LocalDate sessionDate;
    public String modality;
    public String status;
    public String platform;
    @Column(name = "other_platform_name")
    public String otherPlatformName;
    @Column(name = "meeting_link")
    public String meetingLink;
    @Column(name = "location_name")
    public String locationName;
    @Column(name = "location_address")
    public String locationAddress;
    @Column(name = "location_notes")
    public String locationNotes;
    @Column(name = "learner_note")
    public String learnerNote;
    @Column(name = "proposed_by")
    public String proposedBy;
    @Column(name = "cancel_reason")
    public String cancelReason;
    @Column(name = "cancel_comment")
    public String cancelComment;
    @Column(name = "created_at_epoch_ms")
    public long createdAtEpochMs;
    @Version
    public long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_learners", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    @Column(name = "learner_id")
    public List<String> learnerIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "session_history", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    public List<HistoryEmbeddable> history = new ArrayList<>();
}
