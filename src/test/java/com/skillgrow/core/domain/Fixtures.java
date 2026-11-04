package com.skillgrow.core.domain;

import com.skillgrow.core.domain.model.AcceptInput;
import com.skillgrow.core.domain.model.Modality;
import com.skillgrow.core.domain.model.OfferedModality;
import com.skillgrow.core.domain.model.Platform;
import com.skillgrow.core.domain.model.ServiceSnapshot;
import com.skillgrow.core.domain.model.Session;
import com.skillgrow.core.domain.model.SessionType;
import java.time.LocalDate;

/** Datos de prueba compartidos por los tests de dominio y de casos de uso. */
public final class Fixtures {

    public static final String MENTOR = "usr_mentor";
    public static final String LEARNER = "usr_learner";
    public static final String OTHER = "usr_other";
    public static final long NOW = 1_762_300_000_000L;
    /** Miércoles 4 de noviembre de 2026. */
    public static final LocalDate TODAY = LocalDate.of(2026, 11, 4);
    public static final LocalDate DAY = TODAY.plusDays(2);

    private Fixtures() {}

    public static ServiceSnapshot individual() {
        return new ServiceSnapshot("srv_1", MENTOR, "Cálculo I", 60, SessionType.INDIVIDUAL, 1,
                OfferedModality.AMBAS, true);
    }

    public static ServiceSnapshot group(int capacity) {
        return new ServiceSnapshot("srv_g", MENTOR, "Taller de Java", 90, SessionType.GRUPAL, capacity,
                OfferedModality.VIRTUAL, true);
    }

    public static Session pendingVirtual() {
        Session s = Session.request("ses_1", individual(), LEARNER, DAY, Modality.VIRTUAL, "Hola", NOW);
        s.pullEvents();
        return s;
    }

    public static Session confirmedVirtual() {
        Session s = pendingVirtual();
        s.accept(MENTOR, meeting(), () -> false, NOW + 1);
        s.pullEvents();
        return s;
    }

    public static AcceptInput meeting() {
        return new AcceptInput(Platform.GOOGLE_MEET, null, "https://meet.google.com/abc-defg-hij", null, null, null);
    }

    public static AcceptInput place() {
        return new AcceptInput(null, null, null, "Biblioteca", "Edificio B, segundo piso", null);
    }
}
