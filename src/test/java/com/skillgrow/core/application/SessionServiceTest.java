package com.skillgrow.core.application;

import static com.skillgrow.core.domain.Fixtures.LEARNER;
import static com.skillgrow.core.domain.Fixtures.MENTOR;
import static com.skillgrow.core.domain.Fixtures.OTHER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skillgrow.core.application.service.SessionAssembler;
import com.skillgrow.core.application.service.SessionService;
import com.skillgrow.core.domain.Fixtures;
import com.skillgrow.core.domain.event.NotificationEvent;
import com.skillgrow.core.domain.exception.DayUnavailableException;
import com.skillgrow.core.domain.exception.NotFoundException;
import com.skillgrow.core.domain.exception.ValidationException;
import com.skillgrow.core.domain.model.AvailabilityRule;
import com.skillgrow.core.domain.model.Modality;
import com.skillgrow.core.domain.model.NotificationType;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionStatus;
import com.skillgrow.core.domain.model.UserSummary;
import com.skillgrow.core.domain.port.in.GamificationUseCase;
import com.skillgrow.core.domain.port.in.SessionCommandUseCase.CreateSessionCommand;
import com.skillgrow.core.domain.port.out.AvailabilityRepositoryPort;
import com.skillgrow.core.domain.port.out.EventPublisherPort;
import com.skillgrow.core.domain.port.out.ServiceCatalogPort;
import com.skillgrow.core.domain.port.out.SessionRepositoryPort;
import com.skillgrow.core.domain.port.out.UserRepositoryPort;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Casos de uso de sesiones con los ports simulados (Mockito): sin base de datos ni Service Bus. */
@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    /** Jueves 5 nov 2026: el mentor atiende los jueves. */
    private static final LocalDate THURSDAY = LocalDate.of(2026, 11, 5);

    @Mock SessionRepositoryPort sessions;
    @Mock AvailabilityRepositoryPort availability;
    @Mock ServiceCatalogPort catalog;
    @Mock UserRepositoryPort users;
    @Mock EventPublisherPort events;
    @Mock GamificationUseCase gamification;

    SessionService service;

    @BeforeEach
    void setUp() {
        // "Hoy" es miércoles 4 nov 2026 en Costa Rica.
        Clock clock = Clock.fixed(Fixtures.TODAY.atTime(10, 0).atZone(ZoneId.of("America/Costa_Rica")).toInstant(),
                ZoneId.of("America/Costa_Rica"));
        service = new SessionService(sessions, availability, catalog, users, events, new SessionAssembler(users),
                gamification, clock);
        lenient().when(users.findSummaries(anyCollection())).thenReturn(Map.of(
                MENTOR, new UserSummary(MENTOR, "Andrea Vargas", null),
                LEARNER, new UserSummary(LEARNER, "Valeria Rojas", null)));
        lenient().when(sessions.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(availability.findRules(MENTOR)).thenReturn(List.of(new AvailabilityRule("r1", MENTOR, 4, List.of())));
        lenient().when(availability.findBlockedDates(MENTOR)).thenReturn(List.of());
    }

    @SuppressWarnings("unchecked")
    private List<NotificationEvent> published() {
        ArgumentCaptor<List<NotificationEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(events).publish(captor.capture());
        return captor.getValue();
    }

    @Test
    void reservarGuardaYPublicaSessionRequestedAlMentor() {
        when(catalog.findService("srv_1")).thenReturn(Optional.of(Fixtures.individual()));

        var result = service.create(LEARNER, new CreateSessionCommand("srv_1", "2026-11-05", "virtual", null));

        assertThat(result.session().status()).isEqualTo(SessionStatus.PENDIENTE);
        verify(sessions).save(any());
        assertThat(published()).singleElement().satisfies(n -> {
            assertThat(n.type()).isEqualTo(NotificationType.SESSION_REQUESTED);
            assertThat(n.recipientId()).isEqualTo(MENTOR);
            assertThat(n.message()).startsWith("Valeria Rojas quiere una sesión de «Cálculo I»");
        });
    }

    @Test
    void unDiaQueNoEsDeAtencionEsDayUnavailable() {
        when(catalog.findService("srv_1")).thenReturn(Optional.of(Fixtures.individual()));

        assertThatThrownBy(() -> service.create(LEARNER, new CreateSessionCommand("srv_1", "2026-11-06", "virtual", null)))
                .isInstanceOf(DayUnavailableException.class);
        verify(sessions, never()).save(any());
        verify(events, never()).publish(any());
    }

    @Test
    void camposInvalidosSeReportanJuntos() {
        assertThatThrownBy(() -> service.create(LEARNER, new CreateSessionCommand("", "5/11/2026", "telepatia", null)))
                .isInstanceOf(ValidationException.class)
                .satisfies(e -> assertThat(((ValidationException) e).details())
                        .extracting("field").containsExactly("serviceId", "date", "modality"));
    }

    @Test
    void unServicioInexistenteEs404() {
        when(catalog.findService("srv_x")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(LEARNER, new CreateSessionCommand("srv_x", "2026-11-05", "virtual", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void enGrupalSeSumaALaSesionDelDia() {
        var group = Fixtures.group(3);
        Session existing = Session.request("ses_g", group, LEARNER, THURSDAY, Modality.VIRTUAL, null, Fixtures.NOW);
        existing.pullEvents();
        when(catalog.findService("srv_g")).thenReturn(Optional.of(group));
        when(sessions.findActiveByService("srv_g", THURSDAY, THURSDAY)).thenReturn(List.of(existing));
        when(users.findById(OTHER)).thenReturn(Optional.empty());

        var result = service.create(OTHER, new CreateSessionCommand("srv_g", "2026-11-05", "virtual", null));

        assertThat(result.session().id()).isEqualTo("ses_g");
        assertThat(result.session().learnerIds()).containsExactly(LEARNER, OTHER);
        assertThat(published()).singleElement().extracting(NotificationEvent::title)
                .isEqualTo("Nuevo participante en tu sesión grupal");
    }

    @Test
    void aceptarRevisaQueElDiaSigaLibre() {
        Session pending = Fixtures.pendingVirtual();
        when(sessions.findById("ses_1")).thenReturn(Optional.of(pending));
        when(sessions.existsConfirmedOn(eq("srv_1"), any(), eq("ses_1"))).thenReturn(true);

        assertThatThrownBy(() -> service.accept(MENTOR, "ses_1", Fixtures.meeting()))
                .isInstanceOf(DayUnavailableException.class);
        verify(events, never()).publish(any());
    }

    @Test
    void completarEvaluaInsigniasDeMentorYAprendiz() {
        Session confirmed = Fixtures.confirmedVirtual();
        when(sessions.findById("ses_1")).thenReturn(Optional.of(confirmed));
        // La sesión es el viernes 6; se completa ese día.
        SessionService onSessionDay = new SessionService(sessions, availability, catalog, users, events,
                new SessionAssembler(users), gamification,
                Clock.fixed(Fixtures.DAY.atTime(18, 0).atZone(ZoneId.of("America/Costa_Rica")).toInstant(),
                        ZoneId.of("America/Costa_Rica")));

        onSessionDay.complete(MENTOR, "ses_1");

        assertThat(published()).extracting(NotificationEvent::type)
                .containsExactly(NotificationType.SESSION_COMPLETED, NotificationType.REVIEW_REQUESTED);
        verify(gamification).evaluate(eq(MENTOR), anyLong());
        verify(gamification).evaluate(eq(LEARNER), anyLong());
    }

    @Test
    void sesionInexistenteEs404() {
        when(sessions.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(LEARNER, "ses_404", null)).isInstanceOf(NotFoundException.class);
    }
}
