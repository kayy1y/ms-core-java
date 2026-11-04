package com.skillgrow.core.infrastructure.adapter.in.rest.dto;

import java.util.List;

public final class AvailabilityDtos {

    private AvailabilityDtos() {}

    public record AvailabilityResponse(List<RuleDto> rules, List<BlockedDateDto> blockedDates) {}

    public record RuleDto(String id, int dayOfWeek, List<String> serviceIds) {}

    public record BlockedDateDto(String id, String date, String reason) {}

    public record AvailableDayDto(String date, int remainingSeats) {}

    public record RuleRequest(Integer dayOfWeek, List<String> serviceIds) {}

    public record BlockedDateRequest(String date, String reason) {}
}
