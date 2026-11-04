package com.skillgrow.core.infrastructure.adapter.in.rest.mapper;

import com.skillgrow.core.domain.model.Badge;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.model.UserBadge;
import com.skillgrow.core.domain.model.UserProfile;
import com.skillgrow.core.domain.model.UserStats;
import com.skillgrow.core.domain.model.UserSummary;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.AcademicDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.BadgeDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.InterestDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.ProfessionalDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.PublicUserResponse;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.StatsDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.TeachSkillDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.UserBadgeDto;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.UserResponse;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.UserSummaryDto;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Dominio → DTOs de usuario. */
public final class UserRestMapper {

    private UserRestMapper() {}

    public static UserResponse toResponse(UserProfile p) {
        User u = p.user();
        Map<String, Boolean> prefs = new LinkedHashMap<>();
        u.emailPreferences().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> prefs.put(e.getKey().value(), e.getValue()));
        return new UserResponse(u.id(), u.fullName(), u.email(), u.avatarUrl(), u.bio(), u.role().value(),
                u.status().value(), academic(u), professional(u), u.knowledge(), teachSkills(u), interests(u),
                stats(p.stats()), badges(p.badges()), prefs, u.createdAtEpochMs());
    }

    public static PublicUserResponse toPublic(UserProfile p) {
        User u = p.user();
        return new PublicUserResponse(u.id(), u.fullName(), u.avatarUrl(), u.bio(), u.role().value(),
                u.status().value(), academic(u), professional(u), u.knowledge(), teachSkills(u), interests(u),
                stats(p.stats()), badges(p.badges()), u.createdAtEpochMs());
    }

    public static UserSummaryDto summary(UserSummary s) {
        return new UserSummaryDto(s.id(), s.fullName(), s.avatarUrl(), 0, 0);
    }

    public static BadgeDto badge(Badge b) {
        return new BadgeDto(b.id(), b.name(), b.description(), b.icon(), b.criterion().value(), b.threshold(),
                b.minReviews(), b.active());
    }

    public static List<UserBadgeDto> badges(List<UserBadge> badges) {
        return badges.stream().map(b -> new UserBadgeDto(badge(b.badge()), b.earnedAtEpochMs(), b.reason())).toList();
    }

    private static AcademicDto academic(User u) {
        return u.academic() == null ? null
                : new AcademicDto(u.academic().institution(), u.academic().career(), u.academic().level());
    }

    private static ProfessionalDto professional(User u) {
        return u.professional() == null ? null
                : new ProfessionalDto(u.professional().occupation(), u.professional().experience());
    }

    private static List<TeachSkillDto> teachSkills(User u) {
        return u.teachSkills().stream().map(t -> new TeachSkillDto(t.skillId(), t.name(), t.level().value())).toList();
    }

    private static List<InterestDto> interests(User u) {
        return u.learningInterests().stream().map(i -> new InterestDto(i.skillId(), i.name())).toList();
    }

    private static StatsDto stats(UserStats s) {
        return new StatsDto(0, 0, s.completedAsMentor(), s.completedAsLearner(), s.studentsHelped());
    }
}
