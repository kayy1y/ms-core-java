package com.skillgrow.core.domain;

import static com.skillgrow.core.domain.Fixtures.DAY;
import static com.skillgrow.core.domain.Fixtures.LEARNER;
import static com.skillgrow.core.domain.Fixtures.MENTOR;
import static com.skillgrow.core.domain.Fixtures.NOW;
import static com.skillgrow.core.domain.Fixtures.OTHER;
import static com.skillgrow.core.domain.Fixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skillgrow.core.domain.event.SessionEvent;
import com.skillgrow.core.domain.event.SessionEvent.Variant;
import com.skillgrow.core.domain.exception.DayUnavailableException;
import com.skillgrow.core.domain.exception.ForbiddenException;
import com.skillgrow.core.domain.exception.InvalidStatusTransitionException;
import com.skillgrow.core.domain.exception.ValidationException;
import com.skillgrow.core.domain.model.AcceptInput;
import com.skillgrow.core.domain.model.CancelReason;
import com.skillgrow.core.domain.model.Modality;
import com.skillgrow.core.domain.model.NotificationType;
import com.skillgrow.core.domain.model.Platform;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionStatus;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Máquina de estados de la sesión (CONTRATO_API.md §3), sin Spring ni base de datos. */
class SessionTest {

    @Nested
    class Reservar {

        @Test
        void quedaPendienteYAvisaAlMentor() {
            Session s = Session.request("ses_1", Fixtures.individual(), LEARNER, DAY, Modality.VIRTUAL, null, NOW);

            assertThat(s.status()).isEqualTo(SessionStatus.PENDIENTE);
            assertThat(s.history()).hasSize(1);
            List<SessionEvent> events = s.pullEvents();
            assertThat(events).singleElement().satisfies(e -> {
                assertThat(e.type()).isEqualTo(NotificationType.SESSION_REQUESTED);
                assertThat(e.recipientIds()).containsExactly(MENTOR);
            });
            assertThat(s.pullEvents()).isEmpty();
        }

        @Test
        void noSePuedeReservarElServicioPropio() {
            assertThatThrownBy(() ->
                    Session.request("ses_1", Fixtures.individual(), MENTOR, DAY, Modality.VIRTUAL, null, NOW))
                    .isInstanceOf(ValidationException.class)
                    .hasMessage("No puedes solicitar tu propio servicio.");
        }

        @Test
        void laModalidadDebeEstarOfrecida() {
            assertThatThrownBy(() ->
                    Session.request("ses_1", Fixtures.group(5), LEARNER, DAY, Modality.PRESENCIAL, null, NOW))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        void sumarseAGrupalAgregaAprendizSinCambiarEstado() {
            Session s = Session.request("ses_g", Fixtures.group(3), LEARNER, DAY, Modality.VIRTUAL, null, NOW);
            s.pullEvents();

            s.joinGroup(OTHER, "Otra Persona", NOW + 5);

            assertThat(s.learnerIds()).containsExactly(LEARNER, OTHER);
            assertThat(s.status()).isEqualTo(SessionStatus.PENDIENTE);
            assertThat(s.pullEvents()).singleElement()
                    .extracting(SessionEvent::variant).isEqualTo(Variant.JOINED_GROUP);
            assertThatThrownBy(() -> s.joinGroup(OTHER, "Otra Persona", NOW))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    class Aceptar {

        @Test
        void elMentorAceptaConEnlace() {
            Session s = Fixtures.pendingVirtual();

            s.accept(MENTOR, Fixtures.meeting(), () -> false, NOW);

            assertThat(s.status()).isEqualTo(SessionStatus.CONFIRMADA);
            assertThat(s.platform()).isEqualTo(Platform.GOOGLE_MEET);
            assertThat(s.pullEvents()).singleElement().satisfies(e -> {
                assertThat(e.type()).isEqualTo(NotificationType.SESSION_ACCEPTED);
                assertThat(e.recipientIds()).containsExactly(LEARNER);
            });
        }

        @Test
        void presencialExigeLugar() {
            Session s = Session.request("ses_p", Fixtures.individual(), LEARNER, DAY, Modality.PRESENCIAL, null, NOW);

            assertThatThrownBy(() -> s.accept(MENTOR, AcceptInput.empty(), () -> false, NOW))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(e -> assertThat(((ValidationException) e).details())
                            .extracting("field").containsExactly("location.name", "location.address"));

            s.accept(MENTOR, Fixtures.place(), () -> false, NOW);
            assertThat(s.location().name()).isEqualTo("Biblioteca");
        }

        @Test
        void virtualExigeEnlaceValido() {
            Session s = Fixtures.pendingVirtual();
            AcceptInput bad = new AcceptInput(Platform.OTRA, " ", "no-es-url", null, null, null);

            assertThatThrownBy(() -> s.accept(MENTOR, bad, () -> false, NOW))
                    .isInstanceOf(ValidationException.class)
                    .satisfies(e -> assertThat(((ValidationException) e).details())
                            .extracting("field").containsExactly("otherPlatformName", "meetingLink"));
        }

        @Test
        void elAprendizNoPuedeAceptar() {
            Session s = Fixtures.pendingVirtual();
            assertThatThrownBy(() -> s.accept(LEARNER, Fixtures.meeting(), () -> false, NOW))
                    .isInstanceOf(ForbiddenException.class);
        }

        @Test
        void siOtraSesionYaTieneElDiaFalla() {
            Session s = Fixtures.pendingVirtual();
            assertThatThrownBy(() -> s.accept(MENTOR, Fixtures.meeting(), () -> true, NOW))
                    .isInstanceOf(DayUnavailableException.class);
        }

        @Test
        void unaConfirmadaNoSeVuelveAAceptar() {
            Session s = Fixtures.confirmedVirtual();
            assertThatThrownBy(() -> s.accept(MENTOR, Fixtures.meeting(), () -> false, NOW))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        void unNoParticipanteRecibe403() {
            Session s = Fixtures.pendingVirtual();
            assertThatThrownBy(() -> s.accept(OTHER, Fixtures.meeting(), () -> false, NOW))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Test
    void rechazarSoloDesdePendienteYPorElMentor() {
        Session s = Fixtures.pendingVirtual();
        assertThatThrownBy(() -> s.reject(LEARNER, null, NOW)).isInstanceOf(ForbiddenException.class);

        s.reject(MENTOR, "  No tengo tiempo  ", NOW);

        assertThat(s.status()).isEqualTo(SessionStatus.CANCELADA);
        assertThat(s.cancelReason()).isEqualTo(CancelReason.REJECTED_BY_MENTOR);
        assertThat(s.cancelComment()).isEqualTo("No tengo tiempo");
        assertThat(s.pullEvents()).singleElement()
                .extracting(SessionEvent::type).isEqualTo(NotificationType.SESSION_REJECTED);
        assertThatThrownBy(() -> s.reject(MENTOR, null, NOW)).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Nested
    class Reprogramar {

        @Test
        void proponeDiaYSoloLaOtraParteConfirma() {
            Session s = Fixtures.confirmedVirtual();

            s.reschedule(LEARNER, DAY.plusDays(7), null, d -> true, NOW);

            assertThat(s.status()).isEqualTo(SessionStatus.REPROGRAMADA);
            assertThat(s.proposedBy()).isEqualTo(LEARNER);
            assertThat(s.date()).isEqualTo(DAY.plusDays(7));
            assertThat(s.history().getLast().comment()).startsWith("Nuevo día propuesto (antes: ");
            assertThat(s.pullEvents()).singleElement().satisfies(e -> {
                assertThat(e.type()).isEqualTo(NotificationType.SESSION_RESCHEDULED);
                assertThat(e.recipientIds()).containsExactly(MENTOR);
            });

            assertThatThrownBy(() -> s.accept(LEARNER, AcceptInput.empty(), () -> false, NOW))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("Debes esperar");

            s.accept(MENTOR, AcceptInput.empty(), () -> false, NOW);
            assertThat(s.status()).isEqualTo(SessionStatus.CONFIRMADA);
            assertThat(s.proposedBy()).isNull();
            assertThat(s.pullEvents()).singleElement()
                    .extracting(SessionEvent::variant).isEqualTo(Variant.AFTER_RESCHEDULE);
        }

        @Test
        void laOtraParteHaceContrapropuesta() {
            Session s = Fixtures.confirmedVirtual();
            s.reschedule(LEARNER, DAY.plusDays(7), null, d -> true, NOW);

            assertThatThrownBy(() -> s.reschedule(LEARNER, DAY.plusDays(8), null, d -> true, NOW))
                    .isInstanceOf(InvalidStatusTransitionException.class);
            s.reschedule(MENTOR, DAY.plusDays(9), "Mejor el jueves", d -> true, NOW);
            assertThat(s.proposedBy()).isEqualTo(MENTOR);
            assertThat(s.history().getLast().comment()).isEqualTo("Mejor el jueves");
        }

        @Test
        void diaNoDisponibleFalla() {
            Session s = Fixtures.pendingVirtual();
            assertThatThrownBy(() -> s.reschedule(LEARNER, DAY.plusDays(1), null, d -> false, NOW))
                    .isInstanceOf(DayUnavailableException.class);
            assertThat(s.status()).isEqualTo(SessionStatus.PENDIENTE);
        }

        @Test
        void sinFechaEsValidacion() {
            Session s = Fixtures.pendingVirtual();
            assertThatThrownBy(() -> s.reschedule(LEARNER, null, null, d -> true, NOW))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Test
    void cancelarDesdeCualquierEstadoActivo() {
        Session s = Fixtures.confirmedVirtual();

        s.cancel(LEARNER, null, NOW);

        assertThat(s.status()).isEqualTo(SessionStatus.CANCELADA);
        assertThat(s.cancelReason()).isEqualTo(CancelReason.CANCELLED_BY_LEARNER);
        assertThat(s.pullEvents()).singleElement().satisfies(e ->
                assertThat(e.recipientIds()).containsExactly(MENTOR));
        assertThatThrownBy(() -> s.cancel(MENTOR, null, NOW)).isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Nested
    class Completar {

        @Test
        void soloDesdeElDiaDeLaSesion() {
            Session s = Fixtures.confirmedVirtual();
            assertThatThrownBy(() -> s.complete(MENTOR, TODAY, NOW))
                    .isInstanceOf(InvalidStatusTransitionException.class)
                    .hasMessageContaining("a partir del día");

            s.complete(MENTOR, DAY, NOW);

            assertThat(s.status()).isEqualTo(SessionStatus.COMPLETADA);
            assertThat(s.pullEvents()).extracting(SessionEvent::type)
                    .containsExactly(NotificationType.SESSION_COMPLETED, NotificationType.REVIEW_REQUESTED);
            assertThat(s.canReview(LEARNER)).isTrue();
            assertThat(s.canReview(MENTOR)).isFalse();
        }

        @Test
        void unaPendienteNoSeCompleta() {
            Session s = Fixtures.pendingVirtual();
            assertThatThrownBy(() -> s.complete(MENTOR, DAY, NOW)).isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        void elAprendizNoCompleta() {
            Session s = Fixtures.confirmedVirtual();
            assertThatThrownBy(() -> s.complete(LEARNER, DAY, NOW)).isInstanceOf(ForbiddenException.class);
        }
    }

    @Test
    void enlaceYLugarSoloLosEditaElMentorSegunLaModalidad() {
        Session s = Fixtures.confirmedVirtual();
        assertThatThrownBy(() -> s.updateMeeting(LEARNER, Platform.ZOOM, null, "https://zoom.us/j/1"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> s.updateLocation(MENTOR, "Aula", "Edificio A", null))
                .isInstanceOf(InvalidStatusTransitionException.class)
                .hasMessage("Esta sesión es virtual.");

        s.updateMeeting(MENTOR, Platform.ZOOM, null, "https://zoom.us/j/1");

        assertThat(s.meetingLink()).isEqualTo("https://zoom.us/j/1");
    }

    @Test
    void elEnlaceSoloLoVenLosParticipantesConfirmados() {
        Session pending = Fixtures.pendingVirtual();
        Session confirmed = Fixtures.confirmedVirtual();

        assertThat(pending.canSeeMeetingLink(LEARNER)).isFalse();
        assertThat(confirmed.canSeeMeetingLink(LEARNER)).isTrue();
        assertThat(confirmed.canSeeMeetingLink(OTHER)).isFalse();
    }
}
