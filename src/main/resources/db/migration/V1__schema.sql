-- MS1 Core · Azure SQL / SQL Server
-- Fechas: instantes en epoch ms (BIGINT); días de calendario en DATE.
-- Los nombres y títulos usan collation CI_AI: la búsqueda ignora mayúsculas y tildes ("maria" encuentra "María").

-- ---------- Usuarios ----------
CREATE TABLE users (
    id                       VARCHAR(40)    NOT NULL PRIMARY KEY,
    full_name                NVARCHAR(120)  COLLATE Latin1_General_CI_AI NOT NULL,
    email                    NVARCHAR(254)  NOT NULL,
    password_hash            VARCHAR(100)   NULL,
    google_sub               VARCHAR(64)    NULL,
    avatar_url               NVARCHAR(MAX)  NULL,
    bio                      NVARCHAR(1000) NOT NULL CONSTRAINT df_users_bio DEFAULT '',
    role                     VARCHAR(10)    NOT NULL,
    status                   VARCHAR(12)    NOT NULL,
    academic_institution     NVARCHAR(150)  NULL,
    academic_career          NVARCHAR(150)  NULL,
    academic_level           NVARCHAR(80)   NULL,
    professional_occupation  NVARCHAR(150)  NULL,
    professional_experience  NVARCHAR(500)  NULL,
    knowledge                NVARCHAR(MAX)  NOT NULL CONSTRAINT df_users_knowledge DEFAULT '[]',
    email_preferences        NVARCHAR(MAX)  NOT NULL CONSTRAINT df_users_prefs DEFAULT '{}',
    created_at_epoch_ms      BIGINT         NOT NULL,
    updated_at_epoch_ms      BIGINT         NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('user', 'admin')),
    CONSTRAINT ck_users_status CHECK (status IN ('active', 'suspended'))
);
CREATE UNIQUE INDEX ux_users_google_sub ON users (google_sub) WHERE google_sub IS NOT NULL;
CREATE INDEX ix_users_created ON users (created_at_epoch_ms);

CREATE TABLE user_teach_skills (
    user_id   VARCHAR(40)   NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    position  INT           NOT NULL,
    skill_id  VARCHAR(40)   NOT NULL,
    name      NVARCHAR(100) NOT NULL,
    level     VARCHAR(12)   NOT NULL,
    CONSTRAINT pk_user_teach_skills PRIMARY KEY (user_id, position),
    CONSTRAINT ck_teach_level CHECK (level IN ('basico', 'intermedio', 'avanzado'))
);

CREATE TABLE user_learning_interests (
    user_id   VARCHAR(40)   NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    position  INT           NOT NULL,
    skill_id  VARCHAR(40)   NOT NULL,
    name      NVARCHAR(100) NOT NULL,
    CONSTRAINT pk_user_learning_interests PRIMARY KEY (user_id, position)
);

-- ---------- Disponibilidad ----------
CREATE TABLE availability_rules (
    id           VARCHAR(40) NOT NULL PRIMARY KEY,
    mentor_id    VARCHAR(40) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    day_of_week  TINYINT     NOT NULL,
    CONSTRAINT uq_rules_mentor_day UNIQUE (mentor_id, day_of_week),
    CONSTRAINT ck_rules_day CHECK (day_of_week BETWEEN 0 AND 6)
);

CREATE TABLE availability_rule_services (
    rule_id     VARCHAR(40) NOT NULL REFERENCES availability_rules (id) ON DELETE CASCADE,
    position    INT         NOT NULL,
    service_id  VARCHAR(40) NOT NULL,
    CONSTRAINT pk_rule_services PRIMARY KEY (rule_id, position)
);

CREATE TABLE blocked_dates (
    id          VARCHAR(40)   NOT NULL PRIMARY KEY,
    mentor_id   VARCHAR(40)   NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    blocked_on  DATE          NOT NULL,
    reason      NVARCHAR(200) NULL,
    CONSTRAINT uq_blocked_mentor_date UNIQUE (mentor_id, blocked_on)
);

-- ---------- Sesiones ----------
-- service_*: copia (snapshot) de los datos del servicio de MS2 al momento de reservar.
CREATE TABLE sessions (
    id                        VARCHAR(40)   NOT NULL PRIMARY KEY,
    service_id                VARCHAR(40)   NOT NULL,
    service_title             NVARCHAR(150) COLLATE Latin1_General_CI_AI NOT NULL,
    service_duration_minutes  INT           NOT NULL,
    service_type              VARCHAR(12)   NOT NULL,
    service_capacity          INT           NOT NULL,
    service_modality          VARCHAR(12)   NOT NULL,
    mentor_id                 VARCHAR(40)   NOT NULL REFERENCES users (id),
    session_date              DATE          NOT NULL,
    modality                  VARCHAR(12)   NOT NULL,
    status                    VARCHAR(14)   NOT NULL,
    platform                  VARCHAR(20)   NULL,
    other_platform_name       NVARCHAR(60)  NULL,
    meeting_link              NVARCHAR(500) NULL,
    location_name             NVARCHAR(150) NULL,
    location_address          NVARCHAR(300) NULL,
    location_notes            NVARCHAR(500) NULL,
    learner_note              NVARCHAR(500) NULL,
    proposed_by               VARCHAR(40)   NULL,
    cancel_reason             VARCHAR(30)   NULL,
    cancel_comment            NVARCHAR(500) NULL,
    created_at_epoch_ms       BIGINT        NOT NULL,
    version                   BIGINT        NOT NULL CONSTRAINT df_sessions_version DEFAULT 0,
    CONSTRAINT ck_sessions_status CHECK (status IN ('pendiente', 'confirmada', 'completada', 'cancelada', 'reprogramada')),
    CONSTRAINT ck_sessions_modality CHECK (modality IN ('virtual', 'presencial'))
);

-- Regla de negocio en la base: un servicio admite UNA sola sesión activa por día.
-- Si dos personas reservan el mismo día a la vez, la segunda inserción falla aquí → DAY_UNAVAILABLE.
CREATE UNIQUE INDEX ux_sessions_service_day_active
    ON sessions (service_id, session_date)
    WHERE status IN ('pendiente', 'confirmada', 'reprogramada');

CREATE INDEX ix_sessions_mentor ON sessions (mentor_id, status);
CREATE INDEX ix_sessions_date ON sessions (session_date);
CREATE INDEX ix_sessions_status ON sessions (status);

CREATE TABLE session_learners (
    session_id  VARCHAR(40) NOT NULL REFERENCES sessions (id) ON DELETE CASCADE,
    position    INT         NOT NULL,
    learner_id  VARCHAR(40) NOT NULL REFERENCES users (id),
    CONSTRAINT pk_session_learners PRIMARY KEY (session_id, position),
    CONSTRAINT uq_session_learner UNIQUE (session_id, learner_id)
);
CREATE INDEX ix_session_learners_learner ON session_learners (learner_id);

CREATE TABLE session_history (
    session_id   VARCHAR(40)   NOT NULL REFERENCES sessions (id) ON DELETE CASCADE,
    position     INT           NOT NULL,
    status       VARCHAR(14)   NOT NULL,
    at_epoch_ms  BIGINT        NOT NULL,
    by_user_id   VARCHAR(40)   NOT NULL,
    comment      NVARCHAR(500) NULL,
    CONSTRAINT pk_session_history PRIMARY KEY (session_id, position)
);

-- ---------- Gamificación ----------
CREATE TABLE badges (
    id           VARCHAR(40)   NOT NULL PRIMARY KEY,
    name         NVARCHAR(40)  NOT NULL,
    description  NVARCHAR(200) NOT NULL,
    icon         VARCHAR(40)   NOT NULL,
    criterion    VARCHAR(30)   NOT NULL,
    threshold    FLOAT         NOT NULL,
    min_reviews  INT           NULL,
    active       BIT           NOT NULL
);

CREATE TABLE user_badges (
    user_id             VARCHAR(40)   NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    badge_id            VARCHAR(40)   NOT NULL REFERENCES badges (id),
    earned_at_epoch_ms  BIGINT        NOT NULL,
    reason              NVARCHAR(200) NOT NULL,
    CONSTRAINT pk_user_badges PRIMARY KEY (user_id, badge_id)
);
