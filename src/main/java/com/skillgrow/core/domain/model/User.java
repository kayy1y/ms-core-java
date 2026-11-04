package com.skillgrow.core.domain.model;

import com.skillgrow.core.domain.exception.FieldIssue;
import com.skillgrow.core.domain.exception.ValidationException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Usuario de SkillGrow. Puede ser mentor y aprendiz a la vez; admin es un rol aparte. */
public class User {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    public static final int MIN_PASSWORD = 8;
    private static final int MAX_BIO = 600;
    private static final int MAX_AVATAR_CHARS = 2_800_000;
    private static final int MAX_KNOWLEDGE_ITEMS = 20;
    private static final int MAX_KNOWLEDGE_CHARS = 60;
    private static final int MAX_SKILLS = 15;

    private final String id;
    private String fullName;
    private final String email;
    private String passwordHash;
    private String googleSub;
    private String avatarUrl;
    private String bio;
    private final Role role;
    private AccountStatus status;
    private AcademicInfo academic;
    private ProfessionalInfo professional;
    private List<String> knowledge;
    private List<TeachSkill> teachSkills;
    private List<LearningInterest> learningInterests;
    private final Map<NotificationType, Boolean> emailPreferences;
    private final long createdAtEpochMs;
    private long updatedAtEpochMs;

    public User(String id, String fullName, String email, String passwordHash, String googleSub,
                String avatarUrl, String bio, Role role, AccountStatus status,
                AcademicInfo academic, ProfessionalInfo professional, List<String> knowledge,
                List<TeachSkill> teachSkills, List<LearningInterest> learningInterests,
                Map<NotificationType, Boolean> emailPreferences, long createdAtEpochMs, long updatedAtEpochMs) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.googleSub = googleSub;
        this.avatarUrl = avatarUrl;
        this.bio = bio == null ? "" : bio;
        this.role = role;
        this.status = status;
        this.academic = academic;
        this.professional = professional;
        this.knowledge = List.copyOf(knowledge);
        this.teachSkills = List.copyOf(teachSkills);
        this.learningInterests = List.copyOf(learningInterests);
        this.emailPreferences = new EnumMap<>(NotificationType.class);
        this.emailPreferences.putAll(defaultEmailPreferences());
        this.emailPreferences.putAll(emailPreferences);
        this.createdAtEpochMs = createdAtEpochMs;
        this.updatedAtEpochMs = updatedAtEpochMs;
    }

    /** Cuenta nueva con rol user y preferencias por defecto. El email ya viene normalizado. */
    public static User register(String id, String fullName, String email, String passwordHash,
                                String googleSub, String avatarUrl, long now) {
        return new User(id, fullName.trim(), email, passwordHash, googleSub, avatarUrl, "",
                Role.USER, AccountStatus.ACTIVE, null, null, List.of(), List.of(), List.of(),
                defaultEmailPreferences(), now, now);
    }

    /** Mismas reglas que el registro de la Fase I. Devuelve el correo normalizado. */
    public static String validateRegistration(String fullName, String email, String password) {
        List<FieldIssue> issues = new ArrayList<>();
        String cleanEmail = normalizeEmail(email);
        if (fullName == null || fullName.trim().length() < 3) {
            issues.add(new FieldIssue("fullName", "Escribe tu nombre completo."));
        }
        if (!EMAIL.matcher(cleanEmail).matches()) {
            issues.add(new FieldIssue("email", "Formato de correo inválido."));
        }
        if (password == null || password.length() < MIN_PASSWORD) {
            issues.add(new FieldIssue("password", "La contraseña debe tener al menos 8 caracteres."));
        }
        ValidationException.throwIfAny(issues);
        return cleanEmail;
    }

    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    public static Map<NotificationType, Boolean> defaultEmailPreferences() {
        Map<NotificationType, Boolean> prefs = new EnumMap<>(NotificationType.class);
        for (NotificationType t : NotificationType.values()) {
            prefs.put(t, true);
        }
        return prefs;
    }

    // ---------- Comportamiento ----------

    public void updateProfile(String newFullName, String newBio, String newAvatarUrl,
                              AcademicInfo newAcademic, ProfessionalInfo newProfessional,
                              boolean academicSent, boolean professionalSent, long now) {
        String name = newFullName == null ? "" : newFullName.trim();
        String cleanBio = newBio == null ? "" : newBio.trim();
        List<FieldIssue> issues = new ArrayList<>();
        if (name.length() < 3) {
            issues.add(new FieldIssue("fullName", "Escribe tu nombre completo."));
        }
        if (cleanBio.length() > MAX_BIO) {
            issues.add(new FieldIssue("bio", "La descripción admite hasta 600 caracteres."));
        }
        if (newAvatarUrl != null && newAvatarUrl.length() > MAX_AVATAR_CHARS) {
            issues.add(new FieldIssue("avatarUrl", "La foto es demasiado grande (máximo 2 MB)."));
        }
        ValidationException.throwIfAny(issues);
        this.fullName = name;
        this.bio = cleanBio;
        this.avatarUrl = newAvatarUrl == null || newAvatarUrl.isBlank() ? null : newAvatarUrl;
        if (academicSent) {
            this.academic = newAcademic;
        }
        if (professionalSent) {
            this.professional = newProfessional;
        }
        touch(now);
    }

    public void replaceKnowledge(List<String> items, long now) {
        List<String> clean = items.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .toList();
        if (clean.stream().anyMatch(s -> s.length() > MAX_KNOWLEDGE_CHARS)) {
            throw ValidationException.of("knowledge", "Cada conocimiento admite hasta 60 caracteres.");
        }
        this.knowledge = new ArrayList<>(new LinkedHashSet<>(clean)).stream().limit(MAX_KNOWLEDGE_ITEMS).toList();
        touch(now);
    }

    /** Los nombres ya vienen resueltos del catálogo; se quitan duplicados y se limita a 15. */
    public void replaceTeachSkills(List<TeachSkill> skills, long now) {
        List<TeachSkill> out = new ArrayList<>();
        for (TeachSkill s : skills) {
            if (out.stream().noneMatch(o -> o.skillId().equals(s.skillId()))) {
                out.add(s);
            }
        }
        this.teachSkills = out.stream().limit(MAX_SKILLS).toList();
        touch(now);
    }

    public void replaceLearningInterests(List<LearningInterest> interests, long now) {
        List<LearningInterest> out = new ArrayList<>();
        for (LearningInterest s : interests) {
            if (out.stream().noneMatch(o -> o.skillId().equals(s.skillId()))) {
                out.add(s);
            }
        }
        this.learningInterests = out.stream().limit(MAX_SKILLS).toList();
        touch(now);
    }

    /** Solo cambia las claves que vienen; las demás se conservan. */
    public void updateEmailPreferences(Map<NotificationType, Boolean> changes, long now) {
        emailPreferences.putAll(changes);
        touch(now);
    }

    public void changeStatus(AccountStatus newStatus, long now) {
        this.status = newStatus;
        touch(now);
    }

    public void linkGoogle(String sub, long now) {
        this.googleSub = sub;
        touch(now);
    }

    public boolean isActive() {
        return status == AccountStatus.ACTIVE;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public UserSummary summary() {
        return new UserSummary(id, fullName, avatarUrl);
    }

    private void touch(long now) {
        this.updatedAtEpochMs = now;
    }

    // ---------- Getters ----------

    public String id() { return id; }
    public String fullName() { return fullName; }
    public String email() { return email; }
    public String passwordHash() { return passwordHash; }
    public String googleSub() { return googleSub; }
    public String avatarUrl() { return avatarUrl; }
    public String bio() { return bio; }
    public Role role() { return role; }
    public AccountStatus status() { return status; }
    public AcademicInfo academic() { return academic; }
    public ProfessionalInfo professional() { return professional; }
    public List<String> knowledge() { return knowledge; }
    public List<TeachSkill> teachSkills() { return teachSkills; }
    public List<LearningInterest> learningInterests() { return learningInterests; }
    public Map<NotificationType, Boolean> emailPreferences() { return Map.copyOf(emailPreferences); }
    public long createdAtEpochMs() { return createdAtEpochMs; }
    public long updatedAtEpochMs() { return updatedAtEpochMs; }
}
