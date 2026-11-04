package com.skillgrow.core.domain;

import static com.skillgrow.core.domain.Fixtures.LEARNER;
import static com.skillgrow.core.domain.Fixtures.MENTOR;
import static com.skillgrow.core.domain.Fixtures.NOW;
import static com.skillgrow.core.domain.Fixtures.OTHER;
import static com.skillgrow.core.domain.Fixtures.TODAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.skillgrow.core.domain.model.AvailabilityRule;
import com.skillgrow.core.domain.model.AvailableDay;
import com.skillgrow.core.domain.model.BlockedDate;
import com.skillgrow.core.domain.model.Modality;
import com.skillgrow.core.domain.model.ServiceSnapshot;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.service.AvailabilityPolicy;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Reglas de días reservables (port de days.ts). TODAY es miércoles 4 nov 2026. */
class AvailabilityPolicyTest {

    private static final int THURSDAY = 4;
    private static final int FRIDAY = 5;
    private final ServiceSnapshot service = Fixtures.individual();
    private final List<AvailabilityRule> rules = List.of(
            new AvailabilityRule("r1", MENTOR, THURSDAY, List.of()),
            new AvailabilityRule("r2", MENTOR, FRIDAY, List.of("otro_servicio")));

    @Test
    void soloDesdeMananaYSoloLosDiasDeAtencionQueAplicanAlServicio() {
        List<AvailableDay> days = AvailabilityPolicy.availableDays(service, rules, List.of(), List.of(),
                TODAY.minusDays(3), TODAY.plusDays(15), TODAY, null);

        assertThat(days).extracting(AvailableDay::date).containsExactly(
                LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 19));
        assertThat(days).allSatisfy(d -> assertThat(d.remainingSeats()).isEqualTo(1));
    }

    @Test
    void lasFechasBloqueadasNoAparecen() {
        List<BlockedDate> blocked = List.of(new BlockedDate("b1", MENTOR, LocalDate.of(2026, 11, 12), "Examen"));

        List<AvailableDay> days = AvailabilityPolicy.availableDays(service, rules, blocked, List.of(),
                TODAY, TODAY.plusDays(14), TODAY, null);

        assertThat(days).extracting(AvailableDay::date).doesNotContain(LocalDate.of(2026, 11, 12));
    }

    @Test
    void unDiaIndividualOcupadoDesaparece_salvoQueSeExcluyaEsaSesion() {
        LocalDate thursday = LocalDate.of(2026, 11, 5);
        Session taken = Session.request("ses_x", service, LEARNER, thursday, Modality.VIRTUAL, null, NOW);

        assertThat(AvailabilityPolicy.isDayAvailable(service, rules, List.of(), List.of(taken), thursday, TODAY, null))
                .isFalse();
        assertThat(AvailabilityPolicy.isDayAvailable(service, rules, List.of(), List.of(taken), thursday, TODAY, "ses_x"))
                .isTrue();
    }

    @Test
    void enGrupalesQuedanLosCuposRestantes() {
        ServiceSnapshot group = Fixtures.group(3);
        LocalDate thursday = LocalDate.of(2026, 11, 5);
        Session s = Session.request("ses_g", group, LEARNER, thursday, Modality.VIRTUAL, null, NOW);
        s.joinGroup(OTHER, "Otra", NOW);

        assertThat(AvailabilityPolicy.remainingSeats(group, thursday, List.of(s), null)).isEqualTo(1);
        s.joinGroup("usr_3", "Tercera", NOW);
        assertThat(AvailabilityPolicy.remainingSeats(group, thursday, List.of(s), null)).isZero();
    }

    @Test
    void unaSesionCanceladaLiberaElDia() {
        LocalDate thursday = LocalDate.of(2026, 11, 5);
        Session s = Session.request("ses_c", service, LEARNER, thursday, Modality.VIRTUAL, null, NOW);
        s.cancel(LEARNER, null, NOW);

        assertThat(AvailabilityPolicy.remainingSeats(service, thursday, List.of(s), null)).isEqualTo(1);
    }

    @Test
    void elDiaDeLaSemanaUsaLaNumeracionDeJavaScript() {
        assertThat(AvailabilityRule.dayOfWeekOf(LocalDate.of(2026, 11, 8))).isZero(); // domingo
        assertThat(AvailabilityRule.dayOfWeekOf(LocalDate.of(2026, 11, 7))).isEqualTo(6); // sábado
    }
}
