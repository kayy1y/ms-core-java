package com.skillgrow.core.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * Fila de la tabla users. Es una clase distinta del modelo de dominio User: un mapper las convierte.
 * Campos públicos a propósito: es solo un contenedor de datos de persistencia, sin reglas.
 */
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    public String id;
    @Column(name = "full_name")
    public String fullName;
    public String email;
    @Column(name = "password_hash")
    public String passwordHash;
    @Column(name = "google_sub")
    public String googleSub;
    @Column(name = "avatar_url")
    public String avatarUrl;
    public String bio;
    public String role;
    public String status;
    @Column(name = "academic_institution")
    public String academicInstitution;
    @Column(name = "academic_career")
    public String academicCareer;
    @Column(name = "academic_level")
    public String academicLevel;
    @Column(name = "professional_occupation")
    public String professionalOccupation;
    @Column(name = "professional_experience")
    public String professionalExperience;
    /** JSON: ["React", "SQL"] */
    public String knowledge;
    /** JSON: {"SESSION_ACCEPTED": true, …} */
    @Column(name = "email_preferences")
    public String emailPreferences;
    @Column(name = "created_at_epoch_ms")
    public long createdAtEpochMs;
    @Column(name = "updated_at_epoch_ms")
    public long updatedAtEpochMs;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_teach_skills", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "position")
    public List<TeachSkillEmbeddable> teachSkills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_learning_interests", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "position")
    public List<InterestEmbeddable> learningInterests = new ArrayList<>();
}
