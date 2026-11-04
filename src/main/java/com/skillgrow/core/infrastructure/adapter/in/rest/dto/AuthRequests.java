package com.skillgrow.core.infrastructure.adapter.in.rest.dto;

import java.util.List;

/** Cuerpos de las peticiones de login y perfil. Las reglas las valida el dominio. */
public final class AuthRequests {

    private AuthRequests() {}

    public record RegisterRequest(String fullName, String email, String password) {}

    public record LoginRequest(String email, String password) {}

    public record GoogleLoginRequest(String idToken) {}

    public record KnowledgeRequest(List<String> knowledge) {}

    public record TeachSkillsRequest(List<TeachSkillItem> teachSkills) {}

    public record TeachSkillItem(String skillId, String level) {}

    public record InterestsRequest(List<InterestItem> learningInterests) {}

    public record InterestItem(String skillId) {}
}
