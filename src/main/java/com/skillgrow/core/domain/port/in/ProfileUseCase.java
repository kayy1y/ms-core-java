package com.skillgrow.core.domain.port.in;

import com.skillgrow.core.domain.model.AcademicInfo;
import com.skillgrow.core.domain.model.ProfessionalInfo;
import com.skillgrow.core.domain.model.UserProfile;
import java.util.List;
import java.util.Map;

/** Perfil propio (/me) y perfil público de otros usuarios. */
public interface ProfileUseCase {

    UserProfile me(String userId);

    /** Perfil público: 404 si no existe o está suspendido. */
    UserProfile publicProfile(String userId);

    UserProfile updateProfile(String userId, UpdateProfileCommand command);

    UserProfile replaceKnowledge(String userId, List<String> knowledge);

    UserProfile replaceTeachSkills(String userId, List<TeachSkillInput> skills);

    UserProfile replaceInterests(String userId, List<String> skillIds);

    /** Claves = tipos de notificación; las desconocidas se ignoran. */
    UserProfile updateEmailPreferences(String userId, Map<String, Boolean> preferences);

    /** academicSent/professionalSent: si el cliente mandó la sección (puede venir null para borrarla). */
    record UpdateProfileCommand(String fullName, String bio, String avatarUrl,
                                AcademicInfo academic, boolean academicSent,
                                ProfessionalInfo professional, boolean professionalSent) {}

    record TeachSkillInput(String skillId, String level) {}
}
