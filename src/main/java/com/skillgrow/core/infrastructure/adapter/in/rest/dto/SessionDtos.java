package com.skillgrow.core.infrastructure.adapter.in.rest.dto;

import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.UserSummaryDto;
import java.util.List;

/** Sesión como la espera la web y el móvil (CONTRATO_API.md), con instantes en epoch ms. */
public final class SessionDtos {

    private SessionDtos() {}

    public record SessionResponse(String id, ServiceRef service, UserSummaryDto mentor, List<UserSummaryDto> learners,
                                  String date, String modality, String status, String platform,
                                  String otherPlatformName, String meetingLink, LocationDto location,
                                  String learnerNote, String proposedBy, String cancelReason, String cancelComment,
                                  List<HistoryDto> history, boolean canReview, long createdAtEpochMs) {}

    public record ServiceRef(String id, String title, int durationMinutes, String type) {}

    public record LocationDto(String name, String address, String notes) {}

    public record HistoryDto(String status, long atEpochMs, String by, String comment) {}

    public record CreateSessionRequest(String serviceId, String date, String modality, String learnerNote) {}

    public record AcceptRequest(String platform, String otherPlatformName, String meetingLink, LocationDto location) {}

    public record CommentRequest(String comment) {}

    public record RescheduleRequest(String date, String comment) {}

    public record MeetingRequest(String platform, String otherPlatformName, String meetingLink) {}
}
