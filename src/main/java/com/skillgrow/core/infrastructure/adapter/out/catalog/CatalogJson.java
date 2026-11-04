package com.skillgrow.core.infrastructure.adapter.out.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.skillgrow.core.domain.model.OfferedModality;
import com.skillgrow.core.domain.model.ServiceSnapshot;
import com.skillgrow.core.domain.model.SessionType;
import com.skillgrow.core.domain.model.WireEnum;

/** Forma JSON de un servicio de MS2 (MentorshipService del contrato), solo con los campos que usa MS1. */
final class CatalogJson {

    private CatalogJson() {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ServiceJson(String id, String mentorId, MentorJson mentor, String title, Integer durationMinutes,
                       String type, Integer capacity, String modality, String status) {

        ServiceSnapshot toSnapshot() {
            String owner = mentorId != null ? mentorId : mentor == null ? null : mentor.id();
            SessionType sessionType = WireEnum.find(SessionType.class, type).orElse(SessionType.INDIVIDUAL);
            return new ServiceSnapshot(id, owner, title, durationMinutes == null ? 60 : durationMinutes, sessionType,
                    capacity == null ? 1 : capacity,
                    WireEnum.find(OfferedModality.class, modality).orElse(OfferedModality.AMBAS),
                    "activo".equals(status));
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record MentorJson(String id) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SkillJson(String id, String name) {}
}
