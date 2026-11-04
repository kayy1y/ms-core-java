# ms-core-java — MS1 Core de SkillGrow

Microservicio principal del Proyecto 2 (Diseño de Software): **usuarios, login, disponibilidad del mentor y sesiones de mentoría**. Cada cambio de una sesión publica un evento en **Azure Service Bus**, y MS3 (notificaciones) lo convierte en una notificación web y un push.

| | |
|---|---|
| Lenguaje y framework | Java 21 · Spring Boot 3.5 |
| Arquitectura | Hexagonal (puertos y adaptadores), vigilada por tests de ArchUnit |
| Base de datos | Azure SQL Database (SQL Server), migraciones con Flyway |
| Mensajería | Azure Service Bus, cola `notifications` |
| Ruta pública | `https://<apim>.azure-api.net/core/v1/...` → backend `/v1/...` |
| Dueño | Persona 1 |

---

## Arquitectura hexagonal

```
src/main/java/com/skillgrow/core/
├── domain/                 ← NÚCLEO: Java puro, sin Spring, JPA ni Azure
│   ├── model/              Session (máquina de estados), User, AvailabilityRule, ServiceSnapshot…
│   ├── event/              SessionEvent, NotificationEvent
│   ├── service/            AvailabilityPolicy (días reservables), NotificationTexts, SpanishDates
│   ├── exception/          Errores de negocio con su código (DAY_UNAVAILABLE, INVALID_STATUS_TRANSITION…)
│   └── port/in · port/out  Casos de uso (entrada) y lo que el dominio necesita del exterior (salida)
├── application/service/    Casos de uso: AuthService, SessionService, AvailabilityService, AdminService…
└── infrastructure/         ← ADAPTADORES (aquí sí hay Spring y Azure)
    ├── adapter/in/rest/    Controllers, DTOs (epoch ms), GlobalExceptionHandler
    ├── adapter/out/persistence/  Entidades JPA, Spring Data y adaptadores de Azure SQL
    ├── adapter/out/messaging/    ServiceBusEventPublisher (después del commit) y LoggingEventPublisher
    ├── adapter/out/catalog/      Cliente de MS2 (http por APIM) y catálogo stub
    ├── adapter/out/security/     BCrypt, JWT, verificación del token de Google
    ├── security/           Filtros: correlation id, certificado de APIM, JWT, rol admin
    └── config/             BeanConfig (wiring de los casos de uso), OpenAPI, reloj
```

Reglas de dependencia (las revisa `HexagonalArchitectureTest`):
- `domain` no importa `org.springframework`, `jakarta.persistence`, `org.hibernate`, `com.azure` ni `com.fasterxml`.
- `application` solo habla con los ports. No conoce JPA ni Azure.
- Los controllers (`adapter/in`) nunca usan adaptadores de salida.
- Las entidades JPA y los modelos del dominio son clases distintas; un mapper las convierte.

**Si se cambia Azure SQL por otra base de datos, solo cambia `infrastructure/adapter/out/persistence`.**

---

## Endpoints

Todos bajo `/v1`, con `Authorization: Bearer <JWT>`, salvo `/health` y `/v1/auth/{register,login,google}`. La especificación completa está en [`docs/openapi.yaml`](docs/openapi.yaml), y con el servicio corriendo también en `/swagger-ui.html`.

| Grupo | Endpoints |
|---|---|
| Salud | `GET /health` · `GET /v1/health` |
| Auth | `POST /v1/auth/register` · `POST /v1/auth/login` · `POST /v1/auth/google` · `POST /v1/auth/logout` |
| Perfil | `GET/PUT /v1/me` · `PUT /v1/me/knowledge` · `/teach-skills` · `/interests` · `/email-preferences` · `GET /v1/users/{id}` |
| Disponibilidad | `GET /v1/me/availability` · `POST/PUT/DELETE /v1/me/availability/rules[/{id}]` · `POST/DELETE /v1/me/availability/blocked-dates[/{id}]` · `GET /v1/services/{id}/available-days` |
| Sesiones | `GET /v1/sessions` · `GET /v1/sessions/{id}` · `POST /v1/sessions` · `GET /v1/me/requests` · `PATCH /v1/sessions/{id}/accept\|reject\|reschedule\|cancel\|complete` · `PUT /v1/sessions/{id}/meeting\|location` |
| Gamificación | `GET /v1/badges` · `GET /v1/users/{id}/badges` · `GET /v1/me/achievements` |
| Admin (rol admin) | `GET /v1/admin/users` · `PATCH /v1/admin/users/{id}/status` · `GET /v1/admin/sessions` · `GET /v1/admin/stats` · `GET/POST/PUT/PATCH /v1/admin/badges…` |

**Convenciones**
- **Fechas:** los instantes van en epoch ms (`createdAtEpochMs`, `history[].atEpochMs`). Los días de calendario de una sesión o de una fecha bloqueada van como `"YYYY-MM-DD"`.
- **Listas:** `?page=1&pageSize=10` (máximo 50) → `{ totalRecords, page, pageSize, items }`.
- **Errores:** `{ status, code, message, details: [{ field, message }], timestampEpochMs, path }`. Nunca se devuelve el stack trace.

| HTTP | `code` |
|---|---|
| 400 | `VALIDATION_ERROR`, `EMAIL_ALREADY_EXISTS`, `INVALID_CREDENTIALS`, `DAY_UNAVAILABLE`, `INVALID_STATUS_TRANSITION` |
| 401 / 403 / 404 / 409 / 503 | `UNAUTHORIZED` / `FORBIDDEN` / `NOT_FOUND` / `CONFLICT` / `SERVICE_UNAVAILABLE` |

---

## Correr en local

Requisitos: JDK 21 o superior y Docker. No hace falta instalar Maven, porque `./mvnw` lo descarga.

```bash
docker compose up -d                       # SQL Server 2022 + base coredb
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
curl http://localhost:8080/health          # si el 8080 está ocupado: PORT=8090 ./mvnw spring-boot:run …
```

El perfil `local` usa:
- el catálogo **stub** (los servicios de la Fase I, en lugar de llamar a MS2);
- **eventos solo en el log** (no hay Service Bus);
- **sin certificado de APIM**;
- el **login demo de Google** (`{"idToken":"demo"}`).

Al arrancar, Flyway crea las tablas y carga el seed de la Fase I. Todos los usuarios tienen la contraseña `Skill123!`:

| Correo | Rol |
|---|---|
| `aprendiz@skillgrow.test` | aprendiz (Valeria, `usr_001`) |
| `mentor@skillgrow.test` | mentora (Andrea, `usr_002`) |
| `admin@skillgrow.test` | admin (`usr_003`) |

## Tests

```bash
./mvnw -B verify
```

| Tipo | Qué cubre |
|---|---|
| ArchUnit | Reglas de la arquitectura hexagonal |
| Dominio | Máquina de estados completa, días reservables y textos de las notificaciones (sin Spring ni BD) |
| Casos de uso | Mockito con todos los ports simulados |
| Web | `@WebMvcTest` con los filtros reales: 401, 403 en admin, formato de error, epoch, visibilidad del enlace |
| Integración (`*IT`) | Testcontainers con SQL Server real: Flyway, índice único, concurrencia optimista y búsqueda sin tildes. Se saltan si no hay Docker |

**Postman:** `postman/MS1-Core.postman_collection.json` tiene 59 requests y 104 aserciones. Se puede correr varias veces seguidas.

```bash
npx newman run postman/MS1-Core.postman_collection.json -e postman/MS1-Core-local.postman_environment.json
```

Si se cambia algo de la colección, se regenera con `python3 tools/postman/build_collection.py`.

---

## Variables de entorno

Ver [`.env.example`](.env.example). En Azure van en **App Service → Configuration**; nunca en el repo.

| Variable | Para qué |
|---|---|
| `SQL_URL`, `SQL_USER`, `SQL_PASSWORD` | Azure SQL (`databaseName=coredb`) |
| `JWT_SECRET` | Clave **base64** de al menos 32 bytes (`openssl rand -base64 48`). El mismo valor va en APIM y en MS3 |
| `GOOGLE_CLIENT_ID` | Client id de Google, el mismo de la web y el móvil |
| `EVENTS_ENABLED=true`, `SERVICEBUS_CONNECTION`, `SERVICEBUS_QUEUE=notifications` | Publicar en Service Bus |
| `APIM_CERT_CHECK=true`, `APIM_CERT_THUMBPRINT` | Aceptar solo el certificado de cliente de APIM |
| `SERVICE_CATALOG_MODE=http`, `DOMINIO_BASE_URL=https://<apim>.azure-api.net/dominio`, `APIM_SUBSCRIPTION_KEY` | Leer servicios y habilidades de MS2 a través de APIM |
| `APP_ZONE=America/Costa_Rica` | Zona para calcular "hoy" (reservas desde mañana) |

## Despliegue en Azure App Service

1. Web App Linux con **Java 21 (Java SE)** en el plan B1. El artefacto es `target/app.jar`.
2. Configurar las variables de arriba.
3. **Settings → Configuration → General:**
   - Certificados de cliente: **Required**;
   - health check en `/health`;
   - ARR affinity **Off** (el servicio no guarda estado en memoria).
4. **GitHub → Settings:**
   - secrets: `AZURE_WEBAPP_PUBLISH_PROFILE`, `APIM_BASE_URL`, `APIM_SUBSCRIPTION_KEY`;
   - variable: `AZURE_WEBAPP_NAME`.
5. Con push a `main`, el workflow corre `./mvnw verify`, despliega y hace el smoke test a `/core/v1/health` **a través de APIM**.

---

## Avisos para el resto del equipo

**P4 (APIM)**
- Importar `docs/openapi.yaml` con el sufijo `core`.
- `validate-jwt` con `<issuer-signing-keys><key>{{jwt-signing-key}}</key></issuer-signing-keys>`. El Named value es el mismo `JWT_SECRET` (ya está en base64), con `issuer = skillgrow-core`.
- Quitar `validate-jwt` en `/health` y `/v1/auth/*`.
- Exigir `role = admin` en `/v1/admin/*`.

**P3 (MS3)**
- El body del mensaje sigue el doc 03 §6. `messageId = eventId`, `subject = eventType` y `data = { entityId, entityType, link }`. El campo `body` es el `message` de la notificación.
- Para validar el JWT del socket: `jwt.verify(token, Buffer.from(JWT_SECRET, 'base64'))`.

**P2 (web y MS2)**
- `/services/{id}/available-days` es de **MS1**: el mapa de prefijos debe mandarla a `/core`.
- MS1 espera que `GET /dominio/v1/services/{id}` traiga `mentor.id` (o `mentorId`), `title`, `durationMinutes`, `type`, `capacity`, `modality` y `status`. También que `GET /dominio/v1/catalog/skills` traiga `[{ id, name }]`.
- `ratingAverage` y `ratingCount` salen en 0 en MS1, porque las reseñas son de MS2.

**Todos**
- El formato de error usa `details[].message` (lo que ya lee la web), no `details[].issue` como dice el doc 03.

## Mejoras documentadas (no implementadas)

- **Outbox:** guardar el evento en la misma transacción SQL y publicarlo desde un job, para no perderlo si Service Bus no responde.
- **Recordatorios** `SESSION_REMINDER`: necesitan un proceso programado.
- **Logout real:** lista de tokens revocados; hoy el JWT vence a las 8 h.
