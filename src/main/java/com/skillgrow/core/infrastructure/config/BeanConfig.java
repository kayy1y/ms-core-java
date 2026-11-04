package com.skillgrow.core.infrastructure.config;

import com.skillgrow.core.application.service.AdminService;
import com.skillgrow.core.application.service.AuthService;
import com.skillgrow.core.application.service.AvailabilityService;
import com.skillgrow.core.application.service.GamificationService;
import com.skillgrow.core.application.service.ProfileService;
import com.skillgrow.core.application.service.Profiles;
import com.skillgrow.core.application.service.SessionAssembler;
import com.skillgrow.core.application.service.SessionQueryService;
import com.skillgrow.core.application.service.SessionService;
import com.skillgrow.core.domain.port.in.AdminUseCase;
import com.skillgrow.core.domain.port.in.AuthUseCase;
import com.skillgrow.core.domain.port.in.AvailabilityUseCase;
import com.skillgrow.core.domain.port.in.GamificationUseCase;
import com.skillgrow.core.domain.port.in.ProfileUseCase;
import com.skillgrow.core.domain.port.in.SessionCommandUseCase;
import com.skillgrow.core.domain.port.in.SessionQueryUseCase;
import com.skillgrow.core.domain.port.out.AvailabilityRepositoryPort;
import com.skillgrow.core.domain.port.out.BadgeRepositoryPort;
import com.skillgrow.core.domain.port.out.EventPublisherPort;
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

    @Bean
    SessionAssembler sessionAssembler(UserRepositoryPort users) {
        return new SessionAssembler(users);
    }

    @Bean
    GamificationUseCase gamificationUseCase(BadgeRepositoryPort badges, SessionRepositoryPort sessions,
                                            EventPublisherPort events) {
        return new GamificationService(badges, sessions, events);
    }

    @Bean
    SessionCommandUseCase sessionCommandUseCase(SessionRepositoryPort sessions, AvailabilityRepositoryPort availability,
                                                ServiceCatalogPort catalog, UserRepositoryPort users,
                                                EventPublisherPort events, SessionAssembler assembler,
                                                GamificationUseCase gamification, Clock clock) {
        return new SessionService(sessions, availability, catalog, users, events, assembler, gamification, clock);
    }

    @Bean
    SessionQueryUseCase sessionQueryUseCase(SessionRepositoryPort sessions, SessionAssembler assembler, Clock clock) {
        return new SessionQueryService(sessions, assembler, clock);
    }

    @Bean
    AvailabilityUseCase availabilityUseCase(AvailabilityRepositoryPort availability, SessionRepositoryPort sessions,
                                            ServiceCatalogPort catalog, Clock clock) {
        return new AvailabilityService(availability, sessions, catalog, clock);
    }

    @Bean
    AdminUseCase adminUseCase(UserRepositoryPort users, SessionRepositoryPort sessions, SessionAssembler assembler,
                              EventPublisherPort events, Clock clock) {
        return new AdminService(users, sessions, assembler, events, clock);
    }
}
