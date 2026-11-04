package com.skillgrow.core.infrastructure.config;

import com.skillgrow.core.application.service.AuthService;
import com.skillgrow.core.application.service.ProfileService;
import com.skillgrow.core.application.service.Profiles;
import com.skillgrow.core.domain.port.in.AuthUseCase;
import com.skillgrow.core.domain.port.in.ProfileUseCase;
import com.skillgrow.core.domain.port.out.BadgeRepositoryPort;
import com.skillgrow.core.domain.port.out.GoogleTokenVerifierPort;
import com.skillgrow.core.domain.port.out.PasswordHasherPort;
import com.skillgrow.core.domain.port.out.ServiceCatalogPort;
import com.skillgrow.core.domain.port.out.SessionRepositoryPort;
import com.skillgrow.core.domain.port.out.TokenIssuerPort;
import com.skillgrow.core.domain.port.out.UserRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wiring de la arquitectura hexagonal: los casos de uso (application) son clases normales sin
 * anotaciones de Spring; aquí se crean y se les pasan los adaptadores que implementan sus ports.
 */
@Configuration
public class BeanConfig {

    @Bean
    Profiles profiles(SessionRepositoryPort sessions, BadgeRepositoryPort badges) {
        return new Profiles(sessions, badges);
    }

    @Bean
    AuthUseCase authUseCase(UserRepositoryPort users, PasswordHasherPort hasher, TokenIssuerPort tokens,
                            GoogleTokenVerifierPort google, Profiles profiles, Clock clock) {
        return new AuthService(users, hasher, tokens, google, profiles, clock);
    }

    @Bean
    ProfileUseCase profileUseCase(UserRepositoryPort users, ServiceCatalogPort catalog, Profiles profiles,
                                  Clock clock) {
        return new ProfileService(users, catalog, profiles, clock);
    }
}
