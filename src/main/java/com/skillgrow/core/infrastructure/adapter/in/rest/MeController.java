package com.skillgrow.core.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.skillgrow.core.domain.model.AcademicInfo;
import com.skillgrow.core.domain.model.ProfessionalInfo;
import com.skillgrow.core.domain.port.in.ProfileUseCase;
import com.skillgrow.core.domain.port.in.ProfileUseCase.TeachSkillInput;
import com.skillgrow.core.domain.port.in.ProfileUseCase.UpdateProfileCommand;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AuthRequests.InterestItem;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AuthRequests.InterestsRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AuthRequests.KnowledgeRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.AuthRequests.TeachSkillsRequest;
import com.skillgrow.core.infrastructure.adapter.in.rest.dto.UserDtos.UserResponse;
import com.skillgrow.core.infrastructure.adapter.in.rest.mapper.UserRestMapper;
import com.skillgrow.core.infrastructure.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/me")
@Tag(name = "Perfil", description = "Perfil del usuario autenticado")
public class MeController {

    private final ProfileUseCase profiles;

    public MeController(ProfileUseCase profiles) {
        this.profiles = profiles;
    }

    @GetMapping
    @Operation(summary = "Mi perfil")
    public UserResponse me(CurrentUser user) {
        return UserRestMapper.toResponse(profiles.me(user.id()));
    }

    /** academic y professional son opcionales: si no vienen no se tocan; si vienen null se borran. */
    @PutMapping
    @Operation(summary = "Editar nombre, bio, foto, datos académicos y profesionales")
    public UserResponse update(CurrentUser user, @RequestBody JsonNode body) {
        JsonNode academic = body.get("academic");
        JsonNode professional = body.get("professional");
        UpdateProfileCommand cmd = new UpdateProfileCommand(
                text(body, "fullName"), text(body, "bio"), text(body, "avatarUrl"),
                academic == null || academic.isNull() ? null
                        : new AcademicInfo(text(academic, "institution"), text(academic, "career"), text(academic, "level")),
                body.has("academic"),
                professional == null || professional.isNull() ? null
                        : new ProfessionalInfo(text(professional, "occupation"), text(professional, "experience")),
                body.has("professional"));
        return UserRestMapper.toResponse(profiles.updateProfile(user.id(), cmd));
    }

    @PutMapping("/knowledge")
    @Operation(summary = "Reemplazar los conocimientos libres")
    public UserResponse knowledge(CurrentUser user, @RequestBody KnowledgeRequest body) {
        return UserRestMapper.toResponse(profiles.replaceKnowledge(user.id(), orEmpty(body.knowledge())));
    }

    @PutMapping("/teach-skills")
    @Operation(summary = "Reemplazar las habilidades que enseño")
    public UserResponse teachSkills(CurrentUser user, @RequestBody TeachSkillsRequest body) {
        List<TeachSkillInput> skills = orEmpty(body.teachSkills()).stream()
                .map(s -> new TeachSkillInput(s.skillId(), s.level()))
                .toList();
        return UserRestMapper.toResponse(profiles.replaceTeachSkills(user.id(), skills));
    }

    @PutMapping("/interests")
    @Operation(summary = "Reemplazar los intereses de aprendizaje")
    public UserResponse interests(CurrentUser user, @RequestBody InterestsRequest body) {
        List<String> ids = orEmpty(body.learningInterests()).stream().map(InterestItem::skillId).toList();
        return UserRestMapper.toResponse(profiles.replaceInterests(user.id(), ids));
    }

    @PutMapping("/email-preferences")
    @Operation(summary = "Activar o desactivar correos por tipo de notificación")
    public UserResponse emailPreferences(CurrentUser user, @RequestBody Map<String, Boolean> body) {
        return UserRestMapper.toResponse(profiles.updateEmailPreferences(user.id(), body));
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
