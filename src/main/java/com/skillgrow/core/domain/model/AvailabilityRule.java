package com.skillgrow.core.domain.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Un día de la semana en que el mentor atiende (0 = domingo … 6 = sábado). Si serviceIds está vacío,
 * aplica a todos sus servicios. Las mentorías se agendan por día, sin hora.
 */
public record AvailabilityRule(String id, String mentorId, int dayOfWeek, List<String> serviceIds) {

    public AvailabilityRule {
        serviceIds = List.copyOf(serviceIds);
    }

    public boolean appliesTo(String serviceId) {
        return serviceIds.isEmpty() || serviceIds.contains(serviceId);
    }

    public AvailabilityRule withServices(List<String> newServiceIds) {
        return new AvailabilityRule(id, mentorId, dayOfWeek, newServiceIds);
    }

    /** Día de la semana con la misma numeración que JavaScript (0 = domingo). */
    public static int dayOfWeekOf(LocalDate date) {
        return date.getDayOfWeek().getValue() % 7;
    }
}
