package com.skillgrow.core.infrastructure.adapter.in.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.skillgrow.core.TestData;
import com.skillgrow.core.domain.Fixtures;
import com.skillgrow.core.domain.exception.DayUnavailableException;
import com.skillgrow.core.domain.model.AccountStatus;
import com.skillgrow.core.domain.model.PageResult;
import com.skillgrow.core.domain.model.Role;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionDetails;
import com.skillgrow.core.domain.model.UserSummary;
import com.skillgrow.core.domain.port.in.AdminUseCase;
import com.skillgrow.core.domain.port.in.SessionCommandUseCase;
import com.skillgrow.core.domain.port.in.SessionQueryUseCase;
import com.skillgrow.core.infrastructure.adapter.out.security.JwtTokenService;
import com.skillgrow.core.infrastructure.config.AppProperties;
import com.skillgrow.core.infrastructure.config.ClockConfig;
import com.skillgrow.core.infrastructure.security.ErrorWriter;
import java.util.List;
import java.util.Map;
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

/** Controllers de sesiones y admin: formato de respuesta, visibilidad del enlace y 403 en admin. */
@WebMvcTest(controllers = {SessionController.class, AdminController.class})
@Import({JwtTokenService.class, ErrorWriter.class, ClockConfig.class, SessionWebTest.Props.class})
@TestPropertySource(properties = {
        "app.zone=America/Costa_Rica", "app.jwt.secret=" + TestData.JWT_SECRET, "app.jwt.ttl-hours=8",
        "app.jwt.issuer=skillgrow-core", "app.apim.cert-check=false", "app.catalog.mode=stub"})
class SessionWebTest {

    @TestConfiguration
    @EnableConfigurationProperties(AppProperties.class)
    static class Props {}

    @Autowired MockMvc mvc;
    @Autowired JwtTokenService jwt;
    @MockitoBean SessionCommandUseCase commands;
    @MockitoBean SessionQueryUseCase queries;
    @MockitoBean AdminUseCase admin;

    private final Map<String, UserSummary> people = Map.of(
            Fixtures.MENTOR, new UserSummary(Fixtures.MENTOR, "Andrea Vargas", null),
            Fixtures.LEARNER, new UserSummary(Fixtures.LEARNER, "Valeria Rojas", null));

    private String token(String id, Role role) {
        return "Bearer " + jwt.issue(TestData.user(id, id + "@skillgrow.test", role, AccountStatus.ACTIVE));
    }

    @Test
    void laSesionSaleConElFormatoDelContrato() throws Exception {
        Session s = Fixtures.confirmedVirtual();
        when(queries.get(Fixtures.LEARNER, "ses_1")).thenReturn(new SessionDetails(s, people));

        mvc.perform(get("/v1/sessions/ses_1").header("Authorization", token(Fixtures.LEARNER, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service.title").value("Cálculo I"))
                .andExpect(jsonPath("$.mentor.fullName").value("Andrea Vargas"))
                .andExpect(jsonPath("$.date").value("2026-11-06"))
                .andExpect(jsonPath("$.status").value("confirmada"))
                .andExpect(jsonPath("$.platform").value("google_meet"))
                .andExpect(jsonPath("$.meetingLink").value("https://meet.google.com/abc-defg-hij"))
                .andExpect(jsonPath("$.history[0].status").value("pendiente"))
                .andExpect(jsonPath("$.history[0].atEpochMs").isNumber())
                .andExpect(jsonPath("$.createdAtEpochMs").value(Fixtures.NOW));
    }

    @Test
    void elEnlaceNoSeMuestraSiLaSesionSigueEnPendiente() throws Exception {
        when(queries.get(Fixtures.LEARNER, "ses_1")).thenReturn(new SessionDetails(Fixtures.pendingVirtual(), people));

        mvc.perform(get("/v1/sessions/ses_1").header("Authorization", token(Fixtures.LEARNER, Role.USER)))
                .andExpect(jsonPath("$.meetingLink").isEmpty());
    }

    @Test
    void listaPaginada() throws Exception {
        when(queries.list(eq(Fixtures.LEARNER), any(), any())).thenReturn(
                new PageResult<>(List.of(new SessionDetails(Fixtures.pendingVirtual(), people)), 11, 2, 5));

        mvc.perform(get("/v1/sessions?page=2&pageSize=5").header("Authorization", token(Fixtures.LEARNER, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(11))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.pageSize").value(5))
                .andExpect(jsonPath("$.items[0].status").value("pendiente"));
    }

    @Test
    void reservarDevuelve201ODayUnavailable() throws Exception {
        when(commands.create(eq(Fixtures.LEARNER), any()))
                .thenReturn(new SessionDetails(Fixtures.pendingVirtual(), people))
                .thenThrow(new DayUnavailableException());
        String body = "{\"serviceId\":\"srv_1\",\"date\":\"2026-11-06\",\"modality\":\"virtual\"}";

        mvc.perform(post("/v1/sessions").header("Authorization", token(Fixtures.LEARNER, Role.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/v1/sessions").header("Authorization", token(Fixtures.LEARNER, Role.USER))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DAY_UNAVAILABLE"))
                .andExpect(jsonPath("$.details[0].field").value("date"));
    }

    @Test
    void cancelarSinCuerpoFunciona() throws Exception {
        Session s = Fixtures.pendingVirtual();
        s.cancel(Fixtures.LEARNER, null, Fixtures.NOW);
        when(commands.cancel(Fixtures.LEARNER, "ses_1", null)).thenReturn(new SessionDetails(s, people));

        mvc.perform(patch("/v1/sessions/ses_1/cancel").header("Authorization", token(Fixtures.LEARNER, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cancelReason").value("CANCELLED_BY_LEARNER"));
    }

    @Test
    void adminRequiereRolAdmin() throws Exception {
        when(admin.users(any(), any(), any(), any(), any())).thenReturn(new PageResult<>(List.of(), 0, 1, 10));

        mvc.perform(get("/v1/admin/users").header("Authorization", token(Fixtures.LEARNER, Role.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mvc.perform(get("/v1/admin/users").header("Authorization", token("usr_003", Role.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecords").value(0));
    }
}
