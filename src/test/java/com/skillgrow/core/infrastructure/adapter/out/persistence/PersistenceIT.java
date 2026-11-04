package com.skillgrow.core.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skillgrow.core.domain.exception.ConflictException;
import com.skillgrow.core.domain.exception.DayUnavailableException;
import com.skillgrow.core.domain.model.AcceptInput;
import com.skillgrow.core.domain.model.Modality;
import com.skillgrow.core.domain.model.OfferedModality;
import com.skillgrow.core.domain.model.PageQuery;
import com.skillgrow.core.domain.model.Platform;
import com.skillgrow.core.domain.model.ServiceSnapshot;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionStatus;
import com.skillgrow.core.domain.model.SessionType;
import com.skillgrow.core.domain.model.User;
import com.skillgrow.core.domain.port.out.SessionRepositoryPort.SessionSearch;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Adaptadores de persistencia contra un SQL Server real (el mismo motor de Azure SQL). Flyway crea el
 * esquema y carga el seed de la Fase I. Se salta si no hay Docker.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration(JacksonAutoConfiguration.class)
@Import({SessionPersistenceAdapter.class, UserPersistenceAdapter.class, UserPersistenceMapper.class})
@Testcontainers(disabledWithoutDocker = true)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PersistenceIT {

    @Container
    @ServiceConnection
    static final MSSQLServerContainer<?> SQL =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    private static final long NOW = 1_762_300_000_000L;
    private static final String LEARNER = "usr_001";
    private static final String MENTOR = "usr_002";

    @Autowired
    SessionPersistenceAdapter sessions;
    @Autowired
    UserPersistenceAdapter users;

    private static ServiceSnapshot service(String id) {
        return new ServiceSnapshot(id, MENTOR, "Prueba de integración", 60, SessionType.INDIVIDUAL, 1,
                OfferedModality.AMBAS, true);
    }

    @Test
    void flywayCargaElSeedDeLaFaseI() {
        User valeria = users.findByEmail("aprendiz@skillgrow.test").orElseThrow();

        assertThat(valeria.id()).isEqualTo(LEARNER);
        assertThat(valeria.knowledge()).isNotEmpty();
        assertThat(users.findById("usr_003").orElseThrow().isAdmin()).isTrue();
        assertThat(users.count()).isGreaterThan(30);
    }

    @Test
    void elIndiceUnicoImpideDosSesionesActivasElMismoDia() {
        LocalDate day = LocalDate.of(2027, 3, 1);
        sessions.save(Session.request("ses_it_1", service("srv_it_a"), LEARNER, day, Modality.VIRTUAL, null, NOW));

        Session second = Session.request("ses_it_2", service("srv_it_a"), "usr_005", day, Modality.VIRTUAL, null, NOW);

        assertThatThrownBy(() -> sessions.save(second)).isInstanceOf(DayUnavailableException.class);
    }

    @Test
    void unaCanceladaLiberaElDiaEnLaBase() {
        LocalDate day = LocalDate.of(2027, 3, 2);
        Session first = sessions.save(
                Session.request("ses_it_3", service("srv_it_b"), LEARNER, day, Modality.VIRTUAL, null, NOW));
        first.cancel(LEARNER, "Ya no puedo", NOW + 1);
        sessions.save(first);

        Session again = sessions.save(
                Session.request("ses_it_4", service("srv_it_b"), "usr_005", day, Modality.VIRTUAL, null, NOW));

        assertThat(again.status()).isEqualTo(SessionStatus.PENDIENTE);
    }

    @Test
    void guardaHistorialEnOrdenYDetectaCambiosSimultaneos() {
        LocalDate day = LocalDate.of(2027, 3, 3);
        sessions.save(Session.request("ses_it_5", service("srv_it_c"), LEARNER, day, Modality.VIRTUAL, null, NOW));

        Session copyA = sessions.findById("ses_it_5").orElseThrow();
        Session copyB = sessions.findById("ses_it_5").orElseThrow();
        copyA.accept(MENTOR, new AcceptInput(Platform.ZOOM, null, "https://zoom.us/j/1", null, null, null),
                () -> false, NOW + 10);
        sessions.save(copyA);
        copyB.cancel(LEARNER, null, NOW + 20);

        assertThatThrownBy(() -> sessions.save(copyB)).isInstanceOf(ConflictException.class);
        Session stored = sessions.findById("ses_it_5").orElseThrow();
        assertThat(stored.status()).isEqualTo(SessionStatus.CONFIRMADA);
        assertThat(stored.history()).extracting("status")
                .containsExactly(SessionStatus.PENDIENTE, SessionStatus.CONFIRMADA);
        assertThat(stored.meetingLink()).isEqualTo("https://zoom.us/j/1");
    }

    @Test
    void laBusquedaIgnoraTildesYMayusculas() {
        var page = sessions.findForParticipant(
                new SessionSearch(MENTOR, "mentor", null, null, "VALERIA", LocalDate.of(2026, 11, 3), false),
                new PageQuery(1, 50));

        assertThat(page.totalRecords()).isPositive();
        assertThat(page.items()).allSatisfy(s -> assertThat(s.mentorId()).isEqualTo(MENTOR));
        assertThat(users.findSummaries(java.util.List.of(LEARNER, MENTOR))).hasSize(2);
    }
}
