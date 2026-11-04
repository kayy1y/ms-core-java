package com.skillgrow.core.domain.port.in;

import com.skillgrow.core.domain.model.AvailabilityRule;
import com.skillgrow.core.domain.model.AvailableDay;
import com.skillgrow.core.domain.model.BlockedDate;
import java.util.List;

/** Días de atención del mentor, fechas bloqueadas y días reservables de un servicio. */
public interface AvailabilityUseCase {

    MentorAvailability get(String mentorId);

    AvailabilityRule addRule(String mentorId, Integer dayOfWeek, List<String> serviceIds);

    AvailabilityRule updateRule(String mentorId, String ruleId, List<String> serviceIds);

    void deleteRule(String mentorId, String ruleId);

    BlockedDate addBlockedDate(String mentorId, String date, String reason);

    void deleteBlockedDate(String mentorId, String blockedDateId);

    /** from/to en YYYY-MM-DD; por defecto desde hoy y 30 días. */
    List<AvailableDay> availableDays(String serviceId, String from, String to, String excludeSessionId);

    record MentorAvailability(List<AvailabilityRule> rules, List<BlockedDate> blockedDates) {}
}
