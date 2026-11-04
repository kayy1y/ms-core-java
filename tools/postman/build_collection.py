"""Genera la colección de Postman de MS1 (v2.1) y sus entornos.

    python3 tools/postman/build_collection.py

Salida:
    postman/MS1-Core.postman_collection.json
    postman/MS1-Core-local.postman_environment.json   (http://localhost:8090)
    postman/MS1-Core-azure.postman_environment.json   (APIM, sin secretos)

La colección se puede correr varias veces seguidas: la sesión que crea termina cancelada (libera el
día), la fecha bloqueada se borra y el perfil se vuelve a guardar con sus mismos datos.
"""
import json
import pathlib
import uuid

ROOT = pathlib.Path(__file__).resolve().parents[2]
OUT = ROOT / "postman"


def tests(*lines):
    return {"listen": "test", "script": {"type": "text/javascript", "exec": list(lines)}}


def pre(*lines):
    return {"listen": "prerequest", "script": {"type": "text/javascript", "exec": list(lines)}}


def status(code):
    return f'pm.test("status {code}", () => pm.response.to.have.status({code}));'


def error_code(code):
    return f'pm.test("code {code}", () => pm.expect(pm.response.json().code).to.eql("{code}"));'


def req(name, method, path, token="learnerToken", body=None, events=(), query=None):
    headers = [{"key": "Ocp-Apim-Subscription-Key", "value": "{{subscriptionKey}}"}]
    if token:
        headers.append({"key": "Authorization", "value": "Bearer {{" + token + "}}"})
    url_path = path.lstrip("/")
    url = {"raw": "{{baseUrl}}/" + url_path, "host": ["{{baseUrl}}"], "path": url_path.split("?")[0].split("/")}
    if query:
        url["raw"] += "?" + "&".join(f"{k}={v}" for k, v in query)
        url["query"] = [{"key": k, "value": v} for k, v in query]
    item = {"name": name, "event": list(events),
            "request": {"method": method, "header": headers, "url": url}}
    if body is not None:
        headers.append({"key": "Content-Type", "value": "application/json"})
        item["request"]["body"] = {"mode": "raw", "raw": body if isinstance(body, str) else json.dumps(body, ensure_ascii=False, indent=2)}
    return item


def folder(name, *items):
    return {"name": name, "item": list(items)}


ERROR_SHAPE = ('pm.test("formato de error estándar", () => { const b = pm.response.json(); '
               'pm.expect(b).to.have.keys("status","code","message","details","timestampEpochMs","path"); '
               'pm.expect(b.timestampEpochMs).to.be.a("number"); });')
PAGE_SHAPE = ('pm.test("lista paginada", () => { const b = pm.response.json(); '
              'pm.expect(b).to.have.keys("totalRecords","page","pageSize","items"); pm.expect(b.items).to.be.an("array"); });')


def login(name, email, var):
    return req(name, "POST", "/v1/auth/login", token=None,
               body={"email": email, "password": "Skill123!"},
               events=[tests(status(200),
                             'const b = pm.response.json();',
                             'pm.test("token y usuario", () => { pm.expect(b.token).to.be.a("string"); '
                             'pm.expect(b.user.createdAtEpochMs).to.be.a("number"); });',
                             f'pm.collectionVariables.set("{var}", b.token);')])


auth = folder(
    "00-Auth",
    login("Login aprendiz (Valeria)", "aprendiz@skillgrow.test", "learnerToken"),
    login("Login mentora (Andrea)", "mentor@skillgrow.test", "mentorToken"),
    login("Login admin", "admin@skillgrow.test", "adminToken"),
    req("Login con contraseña incorrecta → 400", "POST", "/v1/auth/login", token=None,
        body={"email": "aprendiz@skillgrow.test", "password": "otra"},
        events=[tests(status(400), error_code("INVALID_CREDENTIALS"), ERROR_SHAPE)]),
    req("Registro inválido → 400 con details", "POST", "/v1/auth/register", token=None,
        body={"fullName": "A", "email": "no-es-correo", "password": "123"},
        events=[tests(status(400), error_code("VALIDATION_ERROR"),
                      'pm.test("3 campos", () => pm.expect(pm.response.json().details.map(d => d.field))'
                      '.to.eql(["fullName","email","password"]));')]),
    req("Registro nuevo → 201", "POST", "/v1/auth/register", token=None,
        body='{\n  "fullName": "Persona de Prueba",\n  "email": "{{newEmail}}",\n  "password": "Prueba123!"\n}',
        events=[pre('pm.collectionVariables.set("newEmail", `qa+${Date.now()}@skillgrow.test`);'),
                tests(status(201), 'pm.test("rol user", () => pm.expect(pm.response.json().user.role).to.eql("user"));')]),
    req("Registro con correo existente → 400", "POST", "/v1/auth/register", token=None,
        body={"fullName": "Valeria Rojas", "email": "aprendiz@skillgrow.test", "password": "Skill123!"},
        events=[tests(status(400), error_code("EMAIL_ALREADY_EXISTS"))]),
    req("Logout → 204", "POST", "/v1/auth/logout", events=[tests(status(204))]),
)

security = folder(
    "01-Seguridad",
    req("Health es público", "GET", "/v1/health", token=None,
        events=[tests(status(200), 'pm.test("UP", () => pm.expect(pm.response.json().status).to.eql("UP"));')]),
    req("Sin token → 401", "GET", "/v1/me", token=None, events=[tests(status(401), error_code("UNAUTHORIZED"))]),
    req("Token inválido → 401", "GET", "/v1/me", token="invalidToken",
        events=[pre('pm.collectionVariables.set("invalidToken", "abc.def.ghi");'), tests(status(401))]),
    req("Usuario sin rol admin en /admin → 403", "GET", "/v1/admin/users",
        events=[tests(status(403), error_code("FORBIDDEN"))]),
)

profile = folder(
    "Perfil",
    req("GET /me", "GET", "/v1/me", events=[tests(
        status(200),
        'const b = pm.response.json();',
        'pm.test("perfil completo", () => { pm.expect(b.email).to.eql("aprendiz@skillgrow.test"); '
        'pm.expect(b.stats).to.have.property("completedAsLearner"); pm.expect(b.createdAtEpochMs).to.be.a("number"); });',
        'pm.collectionVariables.set("myFullName", b.fullName);',
        'pm.collectionVariables.set("myBio", b.bio);',
        'pm.collectionVariables.set("myKnowledge", JSON.stringify(b.knowledge));',
        'pm.collectionVariables.set("myTeach", JSON.stringify(b.teachSkills.map(t => ({skillId: t.skillId, level: t.level}))));',
        'pm.collectionVariables.set("myInterests", JSON.stringify(b.learningInterests.map(i => ({skillId: i.skillId}))));')]),
    req("PUT /me (mismos datos)", "PUT", "/v1/me",
        body='{\n  "fullName": "{{myFullName}}",\n  "bio": {{myBioJson}}\n}',
        events=[pre('pm.collectionVariables.set("myBioJson", JSON.stringify(pm.collectionVariables.get("myBio") || ""));'),
                tests(status(200))]),
    req("PUT /me con nombre corto → 400", "PUT", "/v1/me", body={"fullName": "A"},
        events=[tests(status(400), 'pm.test("campo fullName", () => pm.expect(pm.response.json().details[0].field).to.eql("fullName"));')]),
    req("PUT /me/knowledge", "PUT", "/v1/me/knowledge", body='{ "knowledge": {{myKnowledge}} }', events=[tests(status(200))]),
    req("PUT /me/teach-skills", "PUT", "/v1/me/teach-skills", body='{ "teachSkills": {{myTeach}} }', events=[tests(status(200))]),
    req("PUT /me/teach-skills con habilidad inexistente → 400", "PUT", "/v1/me/teach-skills",
        body={"teachSkills": [{"skillId": "skl_no_existe", "level": "basico"}]}, events=[tests(status(400))]),
    req("PUT /me/interests", "PUT", "/v1/me/interests", body='{ "learningInterests": {{myInterests}} }', events=[tests(status(200))]),
    req("PUT /me/email-preferences", "PUT", "/v1/me/email-preferences", body={"SESSION_REMINDER": True},
        events=[tests(status(200), 'pm.test("guardada", () => pm.expect(pm.response.json().emailPreferences.SESSION_REMINDER).to.eql(true));')]),
    req("Perfil público sin correo", "GET", "/v1/users/usr_002", events=[tests(
        status(200), 'pm.test("sin email", () => pm.expect(pm.response.json()).to.not.have.property("email"));')]),
    req("Perfil inexistente → 404", "GET", "/v1/users/usr_no_existe", events=[tests(status(404), error_code("NOT_FOUND"))]),
)

availability = folder(
    "Disponibilidad",
    req("GET /me/availability (mentora)", "GET", "/v1/me/availability", token="mentorToken", events=[tests(
        status(200),
        'const b = pm.response.json();',
        'pm.test("reglas y bloqueos", () => { pm.expect(b.rules).to.be.an("array"); pm.expect(b.blockedDates).to.be.an("array"); });',
        'if (b.rules.length) pm.collectionVariables.set("mentorDay", b.rules[0].dayOfWeek);')]),
    req("Día repetido → 400", "POST", "/v1/me/availability/rules", token="mentorToken",
        body='{ "dayOfWeek": {{mentorDay}}, "serviceIds": [] }',
        events=[tests(status(400), 'pm.test("mensaje", () => pm.expect(pm.response.json().message).to.include("Ya marcaste"));')]),
    req("Día fuera de rango → 400", "POST", "/v1/me/availability/rules", token="mentorToken",
        body={"dayOfWeek": 9, "serviceIds": []}, events=[tests(status(400), error_code("VALIDATION_ERROR"))]),
    req("Bloquear fecha → 201", "POST", "/v1/me/availability/blocked-dates", token="mentorToken",
        body='{ "date": "{{blockDate}}", "reason": "Prueba de Postman" }',
        events=[pre('const d = new Date(Date.now() + 200 * 86400000); pm.collectionVariables.set("blockDate", d.toISOString().slice(0, 10));'),
                tests(status(201), 'pm.collectionVariables.set("blockedId", pm.response.json().id);')]),
    req("Bloquear fecha pasada → 400", "POST", "/v1/me/availability/blocked-dates", token="mentorToken",
        body={"date": "2020-01-01"}, events=[tests(status(400))]),
    req("Desbloquear → 204", "DELETE", "/v1/me/availability/blocked-dates/{{blockedId}}", token="mentorToken",
        events=[tests(status(204))]),
    req("Días disponibles del servicio", "GET", "/v1/services/{{serviceId}}/available-days", events=[tests(
        status(200),
        'const days = pm.response.json();',
        'pm.test("al menos 2 días", () => pm.expect(days.length).to.be.at.least(2));',
        'pm.test("formato YYYY-MM-DD", () => pm.expect(days[0].date).to.match(/^\\d{4}-\\d{2}-\\d{2}$/));',
        'pm.collectionVariables.set("dayA", days[0].date); pm.collectionVariables.set("dayB", days[1].date);')]),
)

session_body = '{\n  "serviceId": "{{serviceId}}",\n  "date": "{{dayA}}",\n  "modality": "virtual",\n  "learnerNote": "Prueba de Postman"\n}'
sessions = folder(
    "Sesiones",
    req("Reservar → 201 pendiente", "POST", "/v1/sessions", body=session_body, events=[tests(
        status(201),
        'const b = pm.response.json();',
        'pm.test("pendiente y sin enlace", () => { pm.expect(b.status).to.eql("pendiente"); pm.expect(b.meetingLink).to.eql(null); });',
        'pm.test("historial en epoch", () => pm.expect(b.history[0].atEpochMs).to.be.a("number"));',
        'pm.collectionVariables.set("sessionId", b.id);')]),
    req("Reservar el mismo día → 400 DAY_UNAVAILABLE", "POST", "/v1/sessions", body=session_body,
        events=[tests(status(400), error_code("DAY_UNAVAILABLE"))]),
    req("Reservar sin datos → 400", "POST", "/v1/sessions", body={}, events=[tests(status(400), error_code("VALIDATION_ERROR"))]),
    req("Reservar servicio propio → 400", "POST", "/v1/sessions", token="mentorToken", body=session_body,
        events=[tests(status(400), error_code("VALIDATION_ERROR"),
                      'pm.test("mensaje", () => pm.expect(pm.response.json().message).to.eql("No puedes solicitar tu propio servicio."));')]),
    req("Detalle", "GET", "/v1/sessions/{{sessionId}}", events=[tests(status(200))]),
    req("Sesión inexistente → 404", "GET", "/v1/sessions/ses_no_existe", events=[tests(status(404))]),
    req("No participante → 403", "GET", "/v1/sessions/{{sessionId}}", token="adminToken", events=[tests(status(403))]),
    req("Mis sesiones (próximas)", "GET", "/v1/sessions", query=[("scope", "upcoming"), ("pageSize", "5")],
        events=[tests(status(200), PAGE_SHAPE)]),
    req("Solicitudes de la mentora", "GET", "/v1/me/requests", token="mentorToken", events=[tests(
        status(200), PAGE_SHAPE,
        'pm.test("incluye la reserva", () => pm.expect(pm.response.json().items.map(s => s.id)).to.include(pm.collectionVariables.get("sessionId")));')]),
    req("Aceptar sin enlace → 400", "PATCH", "/v1/sessions/{{sessionId}}/accept", token="mentorToken",
        body={"platform": "zoom"}, events=[tests(status(400), error_code("VALIDATION_ERROR"))]),
    req("El aprendiz no puede aceptar → 403", "PATCH", "/v1/sessions/{{sessionId}}/accept",
        body={"platform": "zoom", "meetingLink": "https://zoom.us/j/1"}, events=[tests(status(403))]),
    req("Aceptar → confirmada", "PATCH", "/v1/sessions/{{sessionId}}/accept", token="mentorToken",
        body={"platform": "google_meet", "meetingLink": "https://meet.google.com/abc-defg-hij"},
        events=[tests(status(200), 'pm.test("confirmada", () => pm.expect(pm.response.json().status).to.eql("confirmada"));')]),
    req("El aprendiz ve el enlace", "GET", "/v1/sessions/{{sessionId}}", events=[tests(
        status(200), 'pm.test("enlace visible", () => pm.expect(pm.response.json().meetingLink).to.include("meet.google.com"));')]),
    req("Completar antes del día → 400", "PATCH", "/v1/sessions/{{sessionId}}/complete", token="mentorToken",
        events=[tests(status(400), error_code("INVALID_STATUS_TRANSITION"))]),
    req("Reprogramar (aprendiz)", "PATCH", "/v1/sessions/{{sessionId}}/reschedule",
        body='{ "date": "{{dayB}}", "comment": "¿Podemos otro día?" }',
        events=[tests(status(200), 'pm.test("reprogramada", () => pm.expect(pm.response.json().status).to.eql("reprogramada"));')]),
    req("Quien propuso no confirma → 400", "PATCH", "/v1/sessions/{{sessionId}}/accept", body={},
        events=[tests(status(400), error_code("INVALID_STATUS_TRANSITION"))]),
    req("La mentora confirma el nuevo día", "PATCH", "/v1/sessions/{{sessionId}}/accept", token="mentorToken", body={},
        events=[tests(status(200), 'pm.test("nuevo día", () => pm.expect(pm.response.json().date).to.eql(pm.collectionVariables.get("dayB")));')]),
    req("Cambiar enlace", "PUT", "/v1/sessions/{{sessionId}}/meeting", token="mentorToken",
        body={"platform": "zoom", "meetingLink": "https://zoom.us/j/123"}, events=[tests(status(200))]),
    req("Cancelar (libera el día)", "PATCH", "/v1/sessions/{{sessionId}}/cancel", body={"comment": "Fin de la prueba"},
        events=[tests(status(200), 'pm.test("cancelada por el aprendiz", () => pm.expect(pm.response.json().cancelReason).to.eql("CANCELLED_BY_LEARNER"));')]),
    req("Rechazar una cancelada → 400", "PATCH", "/v1/sessions/{{sessionId}}/reject", token="mentorToken",
        events=[tests(status(400), error_code("INVALID_STATUS_TRANSITION"))]),
)

admin = folder(
    "Administración",
    req("Usuarios", "GET", "/v1/admin/users", token="adminToken", query=[("search", "valeria")],
        events=[tests(status(200), PAGE_SHAPE)]),
    req("Cambiar estado sin motivo → 400", "PATCH", "/v1/admin/users/usr_001/status", token="adminToken",
        body={"status": "suspended", "reason": "x"}, events=[tests(status(400))]),
    req("Suspender a un admin → 403", "PATCH", "/v1/admin/users/usr_003/status", token="adminToken",
        body={"status": "suspended", "reason": "Prueba de permisos"}, events=[tests(status(403))]),
    req("Sesiones", "GET", "/v1/admin/sessions", token="adminToken", query=[("status", "completada")],
        events=[tests(status(200), PAGE_SHAPE)]),
    req("Estadísticas", "GET", "/v1/admin/stats", token="adminToken", events=[tests(
        status(200), 'pm.test("30 días de actividad", () => pm.expect(pm.response.json().activityLast30Days).to.have.length(30));')]),
)

gamification = folder(
    "Gamificación",
    req("Insignias activas", "GET", "/v1/badges", events=[tests(status(200))]),
    req("Insignias de la mentora", "GET", "/v1/users/usr_002/badges", events=[tests(status(200))]),
    req("Mis logros", "GET", "/v1/me/achievements", token="mentorToken", events=[tests(
        status(200), 'pm.test("5 criterios", () => pm.expect(pm.response.json()).to.have.length(5));')]),
    req("Admin: todas las insignias", "GET", "/v1/admin/badges", token="adminToken", events=[tests(status(200), PAGE_SHAPE)]),
    req("Admin: insignia inválida → 400", "POST", "/v1/admin/badges", token="adminToken",
        body={"name": "X", "description": "corta", "criterion": "nada", "threshold": 0}, events=[tests(status(400))]),
)

collection = {
    "info": {
        "_postman_id": str(uuid.uuid5(uuid.NAMESPACE_URL, "skillgrow/ms-core-java")),
        "name": "SkillGrow · MS1 Core",
        "description": "Pruebas de MS1 (Java). baseUrl local: http://localhost:8090 · Azure: https://<apim>.azure-api.net/core",
        "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json",
    },
    "item": [auth, security, folder("02-MS1-Core", profile, availability, sessions, admin, gamification)],
    "variable": [{"key": "serviceId", "value": "srv_001"}],
}

OUT.mkdir(exist_ok=True)
(OUT / "MS1-Core.postman_collection.json").write_text(json.dumps(collection, ensure_ascii=False, indent=2) + "\n")
for env_name, base in [("local", "http://localhost:8090"), ("azure", "https://<apim>.azure-api.net/core")]:
    env = {"name": f"MS1-Core-{env_name}", "values": [
        {"key": "baseUrl", "value": base, "enabled": True},
        # La key real va solo en "Current value" (no se exporta).
        {"key": "subscriptionKey", "value": "", "type": "secret", "enabled": True},
    ], "_postman_variable_scope": "environment"}
    (OUT / f"MS1-Core-{env_name}.postman_environment.json").write_text(json.dumps(env, indent=2) + "\n")

count = sum(len(f["item"]) for f in [auth, security, profile, availability, sessions, admin, gamification])
print(f"{count} requests")
