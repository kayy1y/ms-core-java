package com.skillgrow.core.domain.model;

import com.skillgrow.core.domain.event.SessionEvent;
import com.skillgrow.core.domain.event.SessionEvent.Variant;
import com.skillgrow.core.domain.exception.DayUnavailableException;
import com.skillgrow.core.domain.exception.FieldIssue;
import com.skillgrow.core.domain.exception.ForbiddenException;
import com.skillgrow.core.domain.exception.InvalidStatusTransitionException;
import com.skillgrow.core.domain.exception.ValidationException;
import com.skillgrow.core.domain.service.SpanishDates;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/**
 * Sesión de mentoría: el agregado principal de MS1. La máquina de estados (CONTRATO_API.md §3) vive
 * aquí, en Java puro. Cada acción válida deja un {@link SessionEvent} para notificar a la otra parte.
 *
 * <pre>
 *  (reservar) → pendiente ─accept→ confirmada ─complete→ completada
 *                  │  └reject→ cancelada        │
 *                  └──────reschedule──→ reprogramada ─accept(la otra parte)→ confirmada
 *  pendiente / confirmada / reprogramada ─cancel→ cancelada
 * </pre>
 */
public class Session {

    private static final int MAX_NOTE = 500;

    private final String id;
    private final ServiceSnapshot service;
    private final String mentorId;
    private final List<String> learnerIds;
    private LocalDate date;
    private final Modality modality;
    private SessionStatus status;
    private Platform platform;
    private String otherPlatformName;
    private String meetingLink;
    private Location location;
    private final String learnerNote;
    private String proposedBy;
    private CancelReason cancelReason;
    private String cancelComment;
    private final List<HistoryEntry> history;
    private final long createdAtEpochMs;
    /** Token de concurrencia optimista; lo maneja el adaptador de persistencia. */
    private final long version;

    private final List<SessionEvent> pendingEvents = new ArrayList<>();

    /** Reconstruye una sesión guardada (lo usa el adaptador de persistencia). */
    public Session(String id, ServiceSnapshot service, String mentorId, List<String> learnerIds,
                   LocalDate date, Modality modality, SessionStatus status, Platform platform,
                   String otherPlatformName, String meetingLink, Location location, String learnerNote,
                   String proposedBy, CancelReason cancelReason, String cancelComment,
                   List<HistoryEntry> history, long createdAtEpochMs, long version) {
        this.id = id;
        this.service = service;
        this.mentorId = mentorId;
        this.learnerIds = new ArrayList<>(learnerIds);
        this.date = date;
        this.modality = modality;
        this.status = status;
        this.platform = platform;
        this.otherPlatformName = otherPlatformName;
        this.meetingLink = meetingLink;
        this.location = location;
        this.learnerNote = learnerNote;
        this.proposedBy = proposedBy;
        this.cancelReason = cancelReason;
        this.cancelComment = cancelComment;
        this.history = new ArrayList<>(history);
        this.createdAtEpochMs = createdAtEpochMs;
        this.version = version;
    }

    // ---------- Reservar ----------

    /**
     * El aprendiz reserva un día. La disponibilidad del día la verifica antes el caso de uso con
     * {@link com.skillgrow.core.domain.service.AvailabilityPolicy}, porque necesita otras sesiones.
     */
    public static Session request(String id, ServiceSnapshot service, String learnerId, LocalDate date,
                                  Modality modality, String learnerNote, long now) {
        checkCanRequest(service, learnerId, modality);
        String note = learnerNote == null || learnerNote.isBlank() ? null : learnerNote.trim();
        if (note != null && note.length() > MAX_NOTE) {
            throw ValidationException.of("learnerNote", "La nota admite hasta 500 caracteres.");
        }
        Session s = new Session(id, service, service.mentorId(), List.of(learnerId), date, modality,
                SessionStatus.PENDIENTE, null, null, null, null, note, null, null, null,
                List.of(new HistoryEntry(SessionStatus.PENDIENTE, now, learnerId, null)), now, 0);
        s.record(NotificationType.SESSION_REQUESTED, learnerId, List.of(service.mentorId()), null, Variant.NEW);
        return s;
    }

    /** Reglas que se revisan antes que la disponibilidad del día (mismo orden que la Fase I). */
    public static void checkCanRequest(ServiceSnapshot service, String learnerId, Modality modality) {
        if (service.mentorId().equals(learnerId)) {
            throw new ValidationException(
                    List.of(new FieldIssue("serviceId", "No puedes solicitar tu propio servicio.")),
                    "No puedes solicitar tu propio servicio.");
        }
        if (!service.modality().allows(modality)) {
            throw ValidationException.of("modality", "Este servicio no ofrece esa modalidad.");
        }
    }

    /** Sumarse a la sesión grupal que ya existe ese día. Los cupos los verifica el caso de uso. */
    public void joinGroup(String learnerId, String learnerFullName, long now) {
        if (learnerIds.contains(learnerId)) {
            throw new ValidationException(
                    List.of(new FieldIssue("date", "Ya estás inscrito en esta sesión.")),
                    "Ya estás inscrito en esta sesión.");
        }
        if (service.mentorId().equals(learnerId)) {
            throw ValidationException.of("serviceId", "No puedes solicitar tu propio servicio.");
        }
        learnerIds.add(learnerId);
        history.add(new HistoryEntry(status, now, learnerId, learnerFullName + " se unió a la sesión grupal."));
        record(NotificationType.SESSION_REQUESTED, learnerId, List.of(mentorId), null, Variant.JOINED_GROUP);
    }

    // ---------- Aceptar ----------

    /**
     * @param dayTaken true si otra sesión del mismo servicio ya está confirmada o reprogramada ese día
     */
    public void accept(String userId, AcceptInput input, BooleanSupplier dayTaken, long now) {
        requireParticipant(userId);
        if (status == SessionStatus.PENDIENTE) {
            if (!mentorId.equals(userId)) {
                throw new ForbiddenException("Solo el mentor puede aceptar la solicitud.");
            }
            if (modality == Modality.VIRTUAL) {
                applyMeeting(MeetingDetails.validated(input.platform(), input.otherPlatformName(), input.meetingLink()));
            } else {
                this.location = Location.validated(input.locationName(), input.locationAddress(), input.locationNotes());
            }
        } else if (status == SessionStatus.REPROGRAMADA) {
            if (userId.equals(proposedBy)) {
                throw new InvalidStatusTransitionException("Debes esperar a que la otra persona confirme el nuevo día.");
            }
        } else {
            throw new InvalidStatusTransitionException();
        }
        if (dayTaken.getAsBoolean()) {
            throw new DayUnavailableException();
        }
        boolean wasRescheduled = status == SessionStatus.REPROGRAMADA;
        move(SessionStatus.CONFIRMADA, userId, null, now);
        proposedBy = null;
        record(NotificationType.SESSION_ACCEPTED, userId, others(userId), null,
                wasRescheduled ? Variant.AFTER_RESCHEDULE : Variant.NEW);
    }

    // ---------- Rechazar ----------

    public void reject(String userId, String comment, long now) {
        requireParticipant(userId);
        if (!mentorId.equals(userId)) {
            throw new ForbiddenException("Solo el mentor puede rechazar la solicitud.");
        }
        if (status != SessionStatus.PENDIENTE) {
            throw new InvalidStatusTransitionException();
        }
        String clean = clean(comment);
        cancelReason = CancelReason.REJECTED_BY_MENTOR;
        cancelComment = clean;
        move(SessionStatus.CANCELADA, userId, clean, now);
        record(NotificationType.SESSION_REJECTED, userId, List.copyOf(learnerIds), clean, Variant.NEW);
    }

    // ---------- Reprogramar ----------

    /** @param isAvailable dice si el nuevo día está libre (sin contar esta misma sesión) */
    public void reschedule(String userId, LocalDate newDate, String comment,
                           Predicate<LocalDate> isAvailable, long now) {
        requireParticipant(userId);
        if (newDate == null) {
            throw ValidationException.of("date", "Selecciona el nuevo día.");
        }
        boolean allowed = status == SessionStatus.PENDIENTE
                || status == SessionStatus.CONFIRMADA
                || (status == SessionStatus.REPROGRAMADA && !userId.equals(proposedBy));
        if (!allowed) {
            throw new InvalidStatusTransitionException();
        }
        if (!isAvailable.test(newDate)) {
            throw new DayUnavailableException();
        }
        LocalDate previous = date;
        date = newDate;
        proposedBy = userId;
        String clean = clean(comment);
        String historyComment = clean != null
                ? clean
                : "Nuevo día propuesto (antes: " + SpanishDates.whenText(previous) + ").";
        move(SessionStatus.REPROGRAMADA, userId, historyComment, now);
        record(NotificationType.SESSION_RESCHEDULED, userId, others(userId), clean, Variant.NEW);
    }

    // ---------- Cancelar ----------

    public void cancel(String userId, String comment, long now) {
        requireParticipant(userId);
        if (!status.isActive()) {
            throw new InvalidStatusTransitionException();
        }
        String clean = clean(comment);
        cancelReason = mentorId.equals(userId) ? CancelReason.CANCELLED_BY_MENTOR : CancelReason.CANCELLED_BY_LEARNER;
        cancelComment = clean;
        move(SessionStatus.CANCELADA, userId, clean, now);
        record(NotificationType.SESSION_CANCELLED, userId, others(userId), clean, Variant.NEW);
    }

    // ---------- Completar ----------

    public void complete(String userId, LocalDate today, long now) {
        requireParticipant(userId);
        if (!mentorId.equals(userId)) {
            throw new ForbiddenException("Solo el mentor puede marcar la sesión como completada.");
        }
        if (status != SessionStatus.CONFIRMADA) {
            throw new InvalidStatusTransitionException();
        }
        if (date.isAfter(today)) {
            throw new InvalidStatusTransitionException("Podrás marcarla como completada a partir del día de la sesión.");
        }
        move(SessionStatus.COMPLETADA, userId, null, now);
        record(NotificationType.SESSION_COMPLETED, userId, List.copyOf(learnerIds), null, Variant.NEW);
        record(NotificationType.REVIEW_REQUESTED, userId, List.copyOf(learnerIds), null, Variant.NEW);
    }

    // ---------- Enlace y lugar ----------

    public void updateMeeting(String userId, Platform newPlatform, String newOtherName, String newLink) {
        requireParticipant(userId);
        if (!mentorId.equals(userId)) {
            throw new ForbiddenException("Solo el mentor puede editar el enlace.");
        }
        if (modality != Modality.VIRTUAL) {
            throw new InvalidStatusTransitionException("Esta sesión es presencial.");
        }
        if (!status.isActive()) {
            throw new InvalidStatusTransitionException();
        }
        applyMeeting(MeetingDetails.validated(newPlatform, newOtherName, newLink));
    }

    public void updateLocation(String userId, String name, String address, String notes) {
        requireParticipant(userId);
        if (!mentorId.equals(userId)) {
            throw new ForbiddenException("Solo el mentor puede editar el lugar.");
        }
        if (modality != Modality.PRESENCIAL) {
            throw new InvalidStatusTransitionException("Esta sesión es virtual.");
        }
        if (!status.isActive()) {
            throw new InvalidStatusTransitionException();
        }
        this.location = Location.validated(name, address, notes);
    }

    // ---------- Reglas de visibilidad ----------

    public boolean isParticipant(String userId) {
        return mentorId.equals(userId) || learnerIds.contains(userId);
    }

    public void requireParticipant(String userId) {
        if (!isParticipant(userId)) {
            throw new ForbiddenException("Solo los participantes pueden ver esta sesión.");
        }
    }

    /** El enlace solo lo ven los participantes y solo cuando la sesión está confirmada o reprogramada. */
    public boolean canSeeMeetingLink(String viewerId) {
        return isParticipant(viewerId)
                && (status == SessionStatus.CONFIRMADA || status == SessionStatus.REPROGRAMADA);
    }

    /** MS2 rechaza la reseña duplicada (ALREADY_REVIEWED); aquí solo se revisa estado y rol. */
    public boolean canReview(String viewerId) {
        return status == SessionStatus.COMPLETADA && learnerIds.contains(viewerId);
    }

    /** Eventos generados desde la última vez; se vacían al leerlos. */
    public List<SessionEvent> pullEvents() {
        List<SessionEvent> out = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return out;
    }

    // ---------- Internos ----------

    private void move(SessionStatus newStatus, String by, String comment, long now) {
        status = newStatus;
        history.add(new HistoryEntry(newStatus, now, by, comment));
    }

    private void applyMeeting(MeetingDetails m) {
        platform = m.platform();
        otherPlatformName = m.otherPlatformName();
        meetingLink = m.meetingLink();
    }

    private List<String> others(String userId) {
        List<String> all = new ArrayList<>();
        all.add(mentorId);
        all.addAll(learnerIds);
        return all.stream().filter(id -> !id.equals(userId)).toList();
    }

    private void record(NotificationType type, String actor, List<String> recipients, String comment, Variant variant) {
        if (!recipients.isEmpty()) {
            pendingEvents.add(new SessionEvent(type, actor, recipients, comment, date, variant));
        }
    }

    private static String clean(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ---------- Getters ----------

    public String id() { return id; }
    public ServiceSnapshot service() { return service; }
    public String mentorId() { return mentorId; }
    public List<String> learnerIds() { return List.copyOf(learnerIds); }
    public LocalDate date() { return date; }
    public Modality modality() { return modality; }
    public SessionStatus status() { return status; }
    public Platform platform() { return platform; }
    public String otherPlatformName() { return otherPlatformName; }
    public String meetingLink() { return meetingLink; }
    public Location location() { return location; }
    public String learnerNote() { return learnerNote; }
    public String proposedBy() { return proposedBy; }
    public CancelReason cancelReason() { return cancelReason; }
    public String cancelComment() { return cancelComment; }
    public List<HistoryEntry> history() { return List.copyOf(history); }
    public long createdAtEpochMs() { return createdAtEpochMs; }
    public long version() { return version; }
}
