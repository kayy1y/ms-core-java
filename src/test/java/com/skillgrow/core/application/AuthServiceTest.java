package com.skillgrow.core.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skillgrow.core.TestData;
import com.skillgrow.core.application.service.AuthService;
import com.skillgrow.core.application.service.Profiles;
import com.skillgrow.core.domain.exception.EmailAlreadyExistsException;
import com.skillgrow.core.domain.exception.ForbiddenException;
import com.skillgrow.core.domain.exception.InvalidCredentialsException;
import com.skillgrow.core.domain.model.AccountStatus;
import com.skillgrow.core.domain.model.Role;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.model.UserStats;
import com.skillgrow.core.domain.port.out.BadgeRepositoryPort;
import com.skillgrow.core.domain.port.out.GoogleTokenVerifierPort;
import com.skillgrow.core.domain.port.out.GoogleTokenVerifierPort.GoogleIdentity;
import com.skillgrow.core.domain.port.out.PasswordHasherPort;
import com.skillgrow.core.domain.port.out.SessionRepositoryPort;
import com.skillgrow.core.domain.port.out.TokenIssuerPort;
import com.skillgrow.core.domain.port.out.UserRepositoryPort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Caso de uso de login con todos los ports simulados: sin base de datos ni Spring. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepositoryPort users;
    @Mock PasswordHasherPort hasher;
    @Mock TokenIssuerPort tokens;
    @Mock GoogleTokenVerifierPort google;
    @Mock SessionRepositoryPort sessions;
    @Mock BadgeRepositoryPort badges;

    AuthService auth;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.ofEpochMilli(1_762_300_000_000L), ZoneOffset.UTC);
        auth = new AuthService(users, hasher, tokens, google, new Profiles(sessions, badges), clock);
    }

    private void profilesWork() {
        when(sessions.statsFor(any())).thenReturn(new UserStats(0, 0, 0));
        when(tokens.issue(any())).thenReturn("jwt");
    }

    @Test
    void registroNormalizaElCorreoYGuardaElHash() {
        profilesWork();
        when(users.existsByEmail("nueva@skillgrow.test")).thenReturn(false);
        when(hasher.hash("Secreta123")).thenReturn("$hash");
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = auth.register("  Nueva Persona ", "  NUEVA@skillgrow.test ", "Secreta123");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());
        assertThat(saved.getValue().email()).isEqualTo("nueva@skillgrow.test");
        assertThat(saved.getValue().fullName()).isEqualTo("Nueva Persona");
        assertThat(saved.getValue().passwordHash()).isEqualTo("$hash");
        assertThat(saved.getValue().role()).isEqualTo(Role.USER);
        assertThat(result.token()).isEqualTo("jwt");
    }

    @Test
    void registroConCorreoExistenteFalla() {
        when(users.existsByEmail("existe@skillgrow.test")).thenReturn(true);

        assertThatThrownBy(() -> auth.register("Alguien Más", "existe@skillgrow.test", "Secreta123"))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(users, never()).save(any());
    }

    @Test
    void loginConContrasenaIncorrectaNoDiceCualDatoFallo() {
        User u = TestData.user("usr_001", "aprendiz@skillgrow.test", Role.USER, AccountStatus.ACTIVE);
        when(users.findByEmail("aprendiz@skillgrow.test")).thenReturn(Optional.of(u));
        when(hasher.matches("mala", u.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> auth.login("aprendiz@skillgrow.test", "mala"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> auth.login("nadie@skillgrow.test", "x"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void unaCuentaSuspendidaNoEntra() {
        User u = TestData.user("usr_009", "susp@skillgrow.test", Role.USER, AccountStatus.SUSPENDED);
        when(users.findByEmail("susp@skillgrow.test")).thenReturn(Optional.of(u));
        when(hasher.matches("Skill123!", u.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> auth.login("susp@skillgrow.test", "Skill123!"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("suspendida");
    }

    @Test
    void googleVinculaLaCuentaExistentePorCorreo() {
        profilesWork();
        User u = TestData.user("usr_001", "aprendiz@skillgrow.test", Role.USER, AccountStatus.ACTIVE);
        when(google.verify("token-google")).thenReturn(
                Optional.of(new GoogleIdentity("sub-123", "Aprendiz@SkillGrow.test", "Valeria", null)));
        when(users.findByGoogleSub("sub-123")).thenReturn(Optional.empty());
        when(users.findByEmail("aprendiz@skillgrow.test")).thenReturn(Optional.of(u));
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = auth.loginWithGoogle("token-google");

        assertThat(result.profile().user().id()).isEqualTo("usr_001");
        assertThat(result.profile().user().googleSub()).isEqualTo("sub-123");
    }

    @Test
    void googleConTokenInvalido() {
        when(google.verify("falso")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auth.loginWithGoogle("falso")).isInstanceOf(InvalidCredentialsException.class);
    }
}
