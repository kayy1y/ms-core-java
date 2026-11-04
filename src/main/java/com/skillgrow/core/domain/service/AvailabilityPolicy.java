package com.skillgrow.core.domain.service;

import com.skillgrow.core.domain.model.AvailabilityRule;
import com.skillgrow.core.domain.model.AvailableDay;
import com.skillgrow.core.domain.model.BlockedDate;
import com.skillgrow.core.domain.model.ServiceSnapshot;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reglas de reserva (port de days.ts de la Fase I). Un día es reservable si:
 * el mentor atiende ese día de la semana para el servicio, no está bloqueado, es a partir de mañana
 * y quedan cupos. Cada servicio admite una sola sesión activa por día; en las grupales se suman
 * aprendices a esa sesión hasta llenar la capacidad.
 */
public final class AvailabilityPolicy {

    public static final int MAX_RANGE_DAYS = 62;

    private AvailabilityPolicy() {}

    /** La sesión activa (pendiente, confirmada o reprogramada) del servicio en ese día, si hay. */
    public static Optional<Session> activeSessionOn(ServiceSnapshot service, LocalDate date,
                                                    List<Session> sessionsOfService, String excludeSessionId) {
        return sessionsOfService.stream()
                .filter(s -> !s.id().equals(excludeSessionId))
                .filter(s -> s.service().id().equals(service.id()))
                .filter(s -> s.date().equals(date))
                .filter(s -> s.status().isActive())
                .findFirst();
    }

    /** Individual: 1 si nadie lo reservó. Grupal: capacidad menos los aprendices ya inscritos. */
    public static int remainingSeats(ServiceSnapshot service, LocalDate date,
                                     List<Session> sessionsOfService, String excludeSessionId) {
        Optional<Session> taken = activeSessionOn(service, date, sessionsOfService, excludeSessionId);
        if (taken.isEmpty()) {
            return service.seatsPerDay();
        }
        if (service.type() == SessionType.GRUPAL) {
            return Math.max(service.capacity() - taken.get().learnerIds().size(), 0);
        }
        return 0;
    }

    public static List<AvailableDay> availableDays(ServiceSnapshot service, List<AvailabilityRule> rules,
                                                   List<BlockedDate> blocked, List<Session> sessionsOfService,
                                                   LocalDate from, LocalDate to, LocalDate today,
                                                   String excludeSessionId) {
        Set<LocalDate> blockedDays = blocked.stream().map(BlockedDate::date).collect(Collectors.toSet());
        Set<Integer> weekDays = rules.stream()
                .filter(r -> r.appliesTo(service.id()))
                .map(AvailabilityRule::dayOfWeek)
                .collect(Collectors.toSet());
        LocalDate firstBookable = today.plusDays(1);
        LocalDate date = from.isBefore(firstBookable) ? firstBookable : from;
        List<AvailableDay> result = new ArrayList<>();
        for (int i = 0; i <= MAX_RANGE_DAYS && !date.isAfter(to); i++, date = date.plusDays(1)) {
            if (blockedDays.contains(date) || !weekDays.contains(AvailabilityRule.dayOfWeekOf(date))) {
                continue;
            }
            int seats = remainingSeats(service, date, sessionsOfService, excludeSessionId);
            if (seats > 0) {
                result.add(new AvailableDay(date, seats));
            }
        }
        return result;
    }

    public static boolean isDayAvailable(ServiceSnapshot service, List<AvailabilityRule> rules,
                                         List<BlockedDate> blocked, List<Session> sessionsOfService,
                                         LocalDate date, LocalDate today, String excludeSessionId) {
        return !availableDays(service, rules, blocked, sessionsOfService, date, date, today, excludeSessionId).isEmpty();
    }
}
