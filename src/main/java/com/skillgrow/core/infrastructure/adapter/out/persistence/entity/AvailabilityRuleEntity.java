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
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "availability_rules")
public class AvailabilityRuleEntity {

    @Id
    public String id;
    @Column(name = "mentor_id")
    public String mentorId;
    @Column(name = "day_of_week")
    public int dayOfWeek;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "availability_rule_services", joinColumns = @JoinColumn(name = "rule_id"))
    @OrderColumn(name = "position")
    @Column(name = "service_id")
    public List<String> serviceIds = new ArrayList<>();
}
