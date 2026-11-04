package com.skillgrow.core.application.service;

import com.skillgrow.core.domain.exception.FieldIssue;
import com.skillgrow.core.domain.exception.NotFoundException;
import com.skillgrow.core.domain.exception.ValidationException;
import com.skillgrow.core.domain.model.AvailabilityRule;
import com.skillgrow.core.domain.model.AvailableDay;
import com.skillgrow.core.domain.model.BlockedDate;
import com.skillgrow.core.domain.model.ServiceSnapshot;
import com.skillgrow.core.domain.port.in.AvailabilityUseCase;
import com.skillgrow.core.domain.port.out.AvailabilityRepositoryPort;
import com.skillgrow.core.domain.port.out.ServiceCatalogPort;
import com.skillgrow.core.domain.port.out.SessionRepositoryPort;
import com.skillgrow.core.domain.service.AvailabilityPolicy;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

/** Días de atención y fechas bloqueadas del mentor, y cálculo de días reservables. */
@Transactional
public class AvailabilityService implements AvailabilityUseCase {

    private static final String[] DAY_NAMES = {"domingo", "lunes", "martes", "miércoles", "jueves", "viernes", "sábado"};

    private final AvailabilityRepositoryPort availability;
    private final SessionRepositoryPort sessions;
    private final ServiceCatalogPort catalog;
    private final Clock clock;

    public AvailabilityService(AvailabilityRepositoryPort availability, SessionRepositoryPort sessions,
                               ServiceCatalogPort catalog, Clock clock) {
        this.availability = availability;
        this.sessions = sessions;
        this.catalog = catalog;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public MentorAvailability get(String mentorId) {
        return new MentorAvailability(availability.findRules(mentorId), availability.findBlockedDates(mentorId));
    }

    @Override
    public AvailabilityRule addRule(String mentorId, Integer dayOfWeek, List<String> serviceIds) {
        List<String> ids = clean(serviceIds);
        validateRule(mentorId, dayOfWeek, ids, null);
        return availability.saveRule(new AvailabilityRule(UUID.randomUUID().toString(), mentorId, dayOfWeek, ids));
    }

    @Override
    public AvailabilityRule updateRule(String mentorId, String ruleId, List<String> serviceIds) {
        AvailabilityRule rule = ownRule(mentorId, ruleId);
        List<String> ids = clean(serviceIds);
        validateRule(mentorId, rule.dayOfWeek(), ids, ruleId);
        return availability.saveRule(rule.withServices(ids));
    }

    @Override
    public void deleteRule(String mentorId, String ruleId) {
        availability.deleteRule(ownRule(mentorId, ruleId).id());
    }

    @Override
    public BlockedDate addBlockedDate(String mentorId, String date, String reason) {
        LocalDate day = Dates.parseOrNull(date);
        if (day == null) {
            throw ValidationException.of("date", "Selecciona una fecha válida.");
        }
        if (day.isBefore(LocalDate.now(clock))) {
            throw ValidationException.of("date", "La fecha no puede estar en el pasado.");
        }
        if (availability.findBlockedDates(mentorId).stream().anyMatch(b -> b.date().equals(day))) {
            throw ValidationException.of("date", "Esa fecha ya está bloqueada.");
        }
        String cleanReason = reason == null || reason.isBlank() ? null : reason.trim();
        return availability.saveBlockedDate(new BlockedDate(UUID.randomUUID().toString(), mentorId, day, cleanReason));
    }

    @Override
    public void deleteBlockedDate(String mentorId, String blockedDateId) {
        BlockedDate b = availability.findBlockedDate(blockedDateId)
                .filter(x -> x.mentorId().equals(mentorId))
                .orElseThrow(() -> new NotFoundException("La fecha bloqueada"));
        availability.deleteBlockedDate(b.id());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableDay> availableDays(String serviceId, String from, String to, String excludeSessionId) {
        ServiceSnapshot service = catalog.findService(serviceId).orElseThrow(() -> new NotFoundException("El servicio"));
        LocalDate today = LocalDate.now(clock);
        LocalDate start = from == null ? today : Dates.parseOrNull(from);
        LocalDate end = to == null && start != null ? start.plusDays(30) : Dates.parseOrNull(to);
        if (start == null || end == null || end.isBefore(start)) {
            throw ValidationException.of("from", "Rango de fechas inválido (usa YYYY-MM-DD).");
        }
        return AvailabilityPolicy.availableDays(service,
                availability.findRules(service.mentorId()),
                availability.findBlockedDates(service.mentorId()),
                sessions.findActiveByService(service.id(), start, end),
                start, end, today, excludeSessionId);
    }

    /** Día 0–6, servicios propios y sin repetir el día (mismas reglas y mensajes que la Fase I). */
    private void validateRule(String mentorId, Integer day, List<String> serviceIds, String excludeRuleId) {
        List<FieldIssue> issues = new ArrayList<>();
        if (day == null || day < 0 || day > 6) {
            issues.add(new FieldIssue("dayOfWeek", "Selecciona un día."));
        }
        boolean foreign = serviceIds.stream().anyMatch(id -> catalog.findService(id)
                .map(s -> !s.mentorId().equals(mentorId)).orElse(true));
        if (foreign) {
            issues.add(new FieldIssue("serviceIds", "Solo puedes asignar tus propios servicios."));
        }
        if (issues.isEmpty() && availability.findRules(mentorId).stream()
                .anyMatch(r -> !r.id().equals(excludeRuleId) && r.dayOfWeek() == day)) {
            issues.add(new FieldIssue("dayOfWeek", "Ya marcaste el " + DAY_NAMES[day] + " como disponible."));
        }
        if (!issues.isEmpty()) {
            throw new ValidationException(issues, issues.getFirst().message());
        }
    }

    private AvailabilityRule ownRule(String mentorId, String ruleId) {
        return availability.findRule(ruleId)
                .filter(r -> r.mentorId().equals(mentorId))
                .orElseThrow(() -> new NotFoundException("El día disponible"));
    }

    private static List<String> clean(List<String> ids) {
        return ids == null ? List.of() : ids.stream().filter(s -> s != null && !s.isBlank()).distinct().toList();
    }
}
