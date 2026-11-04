package com.skillgrow.core.application.service;

import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.model.UserProfile;
import com.skillgrow.core.domain.port.out.BadgeRepositoryPort;
import com.skillgrow.core.domain.port.out.SessionRepositoryPort;

/** Arma el perfil completo (usuario + estadísticas + insignias). Lo comparten varios casos de uso. */
public class Profiles {

    private final SessionRepositoryPort sessions;
    private final BadgeRepositoryPort badges;

    public Profiles(SessionRepositoryPort sessions, BadgeRepositoryPort badges) {
        this.sessions = sessions;
        this.badges = badges;
    }

    public UserProfile of(User user) {
        return new UserProfile(user, sessions.statsFor(user.id()), badges.findByUser(user.id()));
    }
}
