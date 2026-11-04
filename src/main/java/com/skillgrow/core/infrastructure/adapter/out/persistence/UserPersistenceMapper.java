package com.skillgrow.core.infrastructure.adapter.out.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillgrow.core.domain.model.AcademicInfo;
import com.skillgrow.core.domain.model.AccountStatus;
import com.skillgrow.core.domain.model.LearningInterest;
import com.skillgrow.core.domain.model.NotificationType;
import com.skillgrow.core.domain.model.ProfessionalInfo;
import com.skillgrow.core.domain.model.Role;
import com.skillgrow.core.domain.model.SkillLevel;
import com.skillgrow.core.domain.model.TeachSkill;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.model.WireEnum;
import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.InterestEmbeddable;
import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.TeachSkillEmbeddable;
import com.skillgrow.core.infrastructure.adapter.out.persistence.entity.UserEntity;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** User (dominio) ↔ UserEntity (JPA). Las listas simples y las preferencias se guardan como JSON. */
@Component
class UserPersistenceMapper {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
    private static final TypeReference<Map<String, Boolean>> PREFS = new TypeReference<>() {};

    private final ObjectMapper json;

    UserPersistenceMapper(ObjectMapper json) {
        this.json = json;
    }

    User toDomain(UserEntity e) {
        AcademicInfo academic = allNull(e.academicInstitution, e.academicCareer, e.academicLevel)
                ? null : new AcademicInfo(e.academicInstitution, e.academicCareer, e.academicLevel);
        ProfessionalInfo professional = allNull(e.professionalOccupation, e.professionalExperience)
                ? null : new ProfessionalInfo(e.professionalOccupation, e.professionalExperience);
        List<TeachSkill> teach = e.teachSkills.stream()
                .map(t -> new TeachSkill(t.skillId, t.name,
                        WireEnum.find(SkillLevel.class, t.level).orElse(SkillLevel.INTERMEDIO)))
                .toList();
        List<LearningInterest> interests = e.learningInterests.stream()
                .map(i -> new LearningInterest(i.skillId, i.name))
                .toList();
        return new User(e.id, e.fullName, e.email, e.passwordHash, e.googleSub, e.avatarUrl, e.bio,
                WireEnum.find(Role.class, e.role).orElse(Role.USER),
                WireEnum.find(AccountStatus.class, e.status).orElse(AccountStatus.ACTIVE),
                academic, professional, read(e.knowledge, STRING_LIST, List.of()), teach, interests,
                preferences(e.emailPreferences), e.createdAtEpochMs, e.updatedAtEpochMs);
    }

    void copyInto(User u, UserEntity e) {
        e.id = u.id();
        e.fullName = u.fullName();
        e.email = u.email();
        e.passwordHash = u.passwordHash();
        e.googleSub = u.googleSub();
        e.avatarUrl = u.avatarUrl();
        e.bio = u.bio();
        e.role = u.role().value();
        e.status = u.status().value();
        e.academicInstitution = u.academic() == null ? null : u.academic().institution();
        e.academicCareer = u.academic() == null ? null : u.academic().career();
        e.academicLevel = u.academic() == null ? null : u.academic().level();
        e.professionalOccupation = u.professional() == null ? null : u.professional().occupation();
        e.professionalExperience = u.professional() == null ? null : u.professional().experience();
        e.knowledge = write(u.knowledge());
        Map<String, Boolean> prefs = new LinkedHashMap<>();
        u.emailPreferences().forEach((k, v) -> prefs.put(k.value(), v));
        e.emailPreferences = write(prefs);
        e.createdAtEpochMs = u.createdAtEpochMs();
        e.updatedAtEpochMs = u.updatedAtEpochMs();
        e.teachSkills.clear();
        u.teachSkills().forEach(t -> e.teachSkills.add(new TeachSkillEmbeddable(t.skillId(), t.name(), t.level().value())));
        e.learningInterests.clear();
        u.learningInterests().forEach(i -> e.learningInterests.add(new InterestEmbeddable(i.skillId(), i.name())));
    }

    private Map<NotificationType, Boolean> preferences(String raw) {
        Map<NotificationType, Boolean> out = new EnumMap<>(NotificationType.class);
        read(raw, PREFS, Map.<String, Boolean>of()).forEach((k, v) ->
                WireEnum.find(NotificationType.class, k).ifPresent(t -> out.put(t, Boolean.TRUE.equals(v))));
        return out;
    }

    private <T> T read(String raw, TypeReference<T> type, T fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return json.readValue(raw, type);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON inválido en la tabla users", ex);
        }
    }

    private String write(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo serializar a JSON", ex);
        }
    }

    private static boolean allNull(String... values) {
        for (String v : values) {
            if (v != null) {
                return false;
            }
        }
        return true;
    }
}
