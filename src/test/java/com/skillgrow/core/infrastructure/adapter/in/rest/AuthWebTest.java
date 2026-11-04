package com.skillgrow.core.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.skillgrow.core.TestData;
import com.skillgrow.core.domain.exception.InvalidCredentialsException;
import com.skillgrow.core.domain.exception.ValidationException;
import com.skillgrow.core.domain.model.AccountStatus;
import com.skillgrow.core.domain.model.Role;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.model.UserProfile;
import com.skillgrow.core.domain.model.UserStats;
import com.skillgrow.core.domain.port.in.AuthUseCase;
import com.skillgrow.core.domain.port.in.ProfileUseCase;
import com.skillgrow.core.infrastructure.adapter.out.security.JwtTokenService;
import com.skillgrow.core.infrastructure.config.AppProperties;
import com.skillgrow.core.infrastructure.config.ClockConfig;
import com.skillgrow.core.infrastructure.security.ErrorWriter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Capa web: filtros reales (JWT y correlation id), controllers y formato de error; casos de uso simulados. */
@WebMvcTest(controllers = {AuthController.class, MeController.class, HealthController.class})
@Import({JwtTokenService.class, ErrorWriter.class, ClockConfig.class, AuthWebTest.Props.class})
@TestPropertySource(properties = {
        "app.zone=America/Costa_Rica", "app.jwt.secret=" + TestData.JWT_SECRET, "app.jwt.ttl-hours=8",
        "app.jwt.issuer=skillgrow-core", "app.apim.cert-check=false", "app.catalog.mode=stub"})
class AuthWebTest {

    @TestConfiguration
    @EnableConfigurationProperties(AppProperties.class)
    static class Props {}

    @Autowired MockMvc mvc;
    @Autowired JwtTokenService jwt;
    @MockitoBean AuthUseCase auth;
    @MockitoBean ProfileUseCase profiles;

    private final User valeria = TestData.user("usr_001", "aprendiz@skillgrow.test", Role.USER, AccountStatus.ACTIVE);
    private final UserProfile profile = new UserProfile(valeria, new UserStats(1, 2, 1), List.of());

    @Test
    void healthEsPublico() throws Exception {
        mvc.perform(get("/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.timestampEpochMs").isNumber());
    }

    @Test
    void sinTokenDa401ConElFormatoEstandar() throws Exception {
        mvc.perform(get("/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.path").value("/v1/me"))
                .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void tokenInvalidoDa401() throws Exception {
        mvc.perform(get("/v1/me").header("Authorization", "Bearer abc"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenDevuelveElPerfilEnEpoch() throws Exception {
        when(profiles.me("usr_001")).thenReturn(profile);

        mvc.perform(get("/v1/me").header("Authorization", "Bearer " + jwt.issue(valeria))
                        .header("X-Correlation-Id", "abc-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "abc-123"))
                .andExpect(jsonPath("$.id").value("usr_001"))
                .andExpect(jsonPath("$.role").value("user"))
                .andExpect(jsonPath("$.createdAtEpochMs").value(1000))
                .andExpect(jsonPath("$.stats.completedAsLearner").value(2));
    }

    @Test
    void loginDevuelveTokenYUsuario() throws Exception {
        when(auth.login("aprendiz@skillgrow.test", "Skill123!")).thenReturn(new AuthUseCase.AuthResult("jwt", profile));

        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"aprendiz@skillgrow.test\",\"password\":\"Skill123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt"))
                .andExpect(jsonPath("$.user.email").value("aprendiz@skillgrow.test"));
    }

    @Test
    void erroresDeNegocioSalenConCodigoYDetalles() throws Exception {
        when(auth.login(any(), any())).thenThrow(new InvalidCredentialsException());
        when(auth.register(any(), any(), any())).thenThrow(ValidationException.of("email", "Formato de correo inválido."));

        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        mvc.perform(post("/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("email"))
                .andExpect(jsonPath("$.details[0].message").value("Formato de correo inválido."));
    }

    @Test
    void jsonMalFormadoEs400() throws Exception {
        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
