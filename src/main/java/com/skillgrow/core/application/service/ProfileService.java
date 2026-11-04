package com.skillgrow.core.application.service;

import com.skillgrow.core.domain.exception.NotFoundException;
import com.skillgrow.core.domain.exception.ValidationException;
import com.skillgrow.core.domain.model.LearningInterest;
import com.skillgrow.core.domain.model.NotificationType;
import com.skillgrow.core.domain.model.SkillLevel;
import com.skillgrow.core.domain.model.TeachSkill;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.model.UserProfile;
import com.skillgrow.core.domain.model.WireEnum;
import com.skillgrow.core.domain.port.in.ProfileUseCase;
import com.skillgrow.core.domain.port.out.ServiceCatalogPort;
import com.skillgrow.core.domain.port.out.UserRepositoryPort;
import java.time.Clock;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

/** Perfil propio y público. Los nombres de las habilidades vienen del catálogo de MS2. */
@Transactional
public class ProfileService implements ProfileUseCase {

    private final UserRepositoryPort users;
    private final ServiceCatalogPort catalog;
    private final Profiles profiles;
    private final Clock clock;

    public ProfileService(UserRepositoryPort users, ServiceCatalogPort catalog, Profiles profiles, Clock clock) {
        this.users = users;
        this.catalog = catalog;
        this.profiles = profiles;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile me(String userId) {
        return profiles.of(load(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfile publicProfile(String userId) {
        User user = load(userId);
        if (!user.isActive()) {
            throw new NotFoundException("El usuario");
        }
        return profiles.of(user);
    }

    @Override
    public UserProfile updateProfile(String userId, UpdateProfileCommand c) {
        User user = load(userId);
        user.updateProfile(c.fullName(), c.bio(), c.avatarUrl(), c.academic(), c.professional(),
                c.academicSent(), c.professionalSent(), clock.millis());
        return profiles.of(users.save(user));
    }

    @Override
    public UserProfile replaceKnowledge(String userId, List<String> knowledge) {
        User user = load(userId);
        user.replaceKnowledge(knowledge, clock.millis());
        return profiles.of(users.save(user));
    }

    @Override
    public UserProfile replaceTeachSkills(String userId, List<TeachSkillInput> skills) {
        User user = load(userId);
        Map<String, String> names = catalog.skillNames(skills.stream().map(TeachSkillInput::skillId).toList());
        List<TeachSkill> resolved = skills.stream().map(s -> {
            String name = names.get(s.skillId());
            if (name == null) {
                throw ValidationException.of("teachSkills", "Una de las habilidades no existe en el catálogo.");
            }
            SkillLevel level = WireEnum.find(SkillLevel.class, s.level()).orElse(SkillLevel.INTERMEDIO);
            return new TeachSkill(s.skillId(), name, level);
        }).toList();
        user.replaceTeachSkills(resolved, clock.millis());
        return profiles.of(users.save(user));
    }

    @Override
    public UserProfile replaceInterests(String userId, List<String> skillIds) {
        User user = load(userId);
        Map<String, String> names = catalog.skillNames(skillIds);
        List<LearningInterest> resolved = skillIds.stream().map(id -> {
            String name = names.get(id);
            if (name == null) {
                throw ValidationException.of("learningInterests", "Uno de los intereses no existe en el catálogo.");
            }
            return new LearningInterest(id, name);
        }).toList();
        user.replaceLearningInterests(resolved, clock.millis());
        return profiles.of(users.save(user));
    }

    @Override
    public UserProfile updateEmailPreferences(String userId, Map<String, Boolean> preferences) {
        User user = load(userId);
        Map<NotificationType, Boolean> changes = new EnumMap<>(NotificationType.class);
        preferences.forEach((key, value) -> {
            if (value != null) {
                WireEnum.find(NotificationType.class, key).ifPresent(t -> changes.put(t, value));
            }
        });
        user.updateEmailPreferences(changes, clock.millis());
        return profiles.of(users.save(user));
    }

    private User load(String userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("El usuario"));
    }
}
