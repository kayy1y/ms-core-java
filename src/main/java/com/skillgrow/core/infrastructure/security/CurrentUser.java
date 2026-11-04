package com.skillgrow.core.infrastructure.security;

import com.skillgrow.core.domain.model.Role;

/** Usuario autenticado, sacado del JWT. Los controllers lo reciben como parámetro. */
public record CurrentUser(String id, Role role, String name) {

    public static final String ATTRIBUTE = CurrentUser.class.getName();

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
