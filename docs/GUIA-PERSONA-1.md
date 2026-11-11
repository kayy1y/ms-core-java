# Guía de la Persona 1 — MS1 Core

Esta guía es para entender el servicio, terminar de desplegarlo y defenderlo. Primero va el estado, después los pasos en orden y al final las respuestas para la defensa.

## 1. Estado actualizado al 11 de noviembre

MS1 está desplegado en la Web App principal `app-skillgrow-core-kay`, conectado a Azure SQL y probado
directamente en F1. La ejecución de Actions y las 83 solicitudes / 148 aserciones de Postman pasaron.
Los detalles y límites de esta comprobación están en [VERIFICACION-MS1.md](VERIFICACION-MS1.md).

El catálogo sigue en `stub`, los eventos reales están desactivados y la integración con APIM y
certificados sigue pendiente de P4. P1 debe participar en las pruebas cuando P2/P3/P4 proporcionen
los servicios y la infraestructura. El login real de Google y la reputación dependiente de MS2
también requieren su integración.

### Historial: verificación local al 4 de noviembre

**Hecho y verificado en local:**
- Arquitectura hexagonal completa: dominio puro, casos de uso, adaptadores y reglas con ArchUnit.
- Los 42 endpoints del MS1, con la misma lógica, los mismos textos y los mismos errores que la Fase I.
- Azure SQL: esquema con Flyway, seed de la Fase I (39 usuarios, 398 sesiones), índice único y concurrencia optimista.
- Publicación de los eventos para MS3, después del commit.
- Seguridad: emisión y validación del JWT, filtro del certificado de APIM, rol admin y correlation id.
- 69 tests unitarios y de web + 5 tests de integración contra SQL Server real: **todos en verde**.
- Colección de Postman con 59 requests y 104 aserciones: **100% en verde con Newman**, dos corridas seguidas.
- `docs/openapi.yaml` listo para importar en APIM. Workflow de CI/CD listo.

**Integración final pendiente:**
- Conectar el Service Bus real.
- Activar el certificado de APIM.
- Cambiar el catálogo a `http` cuando MS2 esté arriba.
- Correr Postman contra APIM.

## 2. Plan original y criterios de integración

El despliegue inicial y la comprobación directa ya se realizaron. La tabla conserva el plan original;
las operaciones por APIM, certificados y mensajería se cierran cuando el equipo complete esas conexiones.

| # | Cuándo | Paso | Listo cuando |
|---|---|---|---|
| 1 | Mié 4 | Leer esta guía y recorrer el código en el orden de la sección 3. Correr `./mvnw -B verify` y el servicio en local (README) | Entiendes el recorrido de una reserva |
| 2 | Mié 4 | Crear `ms-core-java` en la GitHub org, `git remote add origin …` y `git push -u origin main`. Proteger `main` (1 aprobación + CI). Desde aquí, todo por rama `feat/…` y PR revisado por P3 | Actions en verde en GitHub |
| 3 | Mié 4 | Pedir a P4: Web App Java 21, base `coredb` en Azure SQL y connection string de Service Bus. Generar `JWT_SECRET` con `openssl rand -base64 48` y dárselo a P4 (APIM) y a P3 (MS3) por un canal privado | Variables cargadas en App Service |
| 4 | Jue 5 | Configurar los secrets de GitHub (`AZURE_WEBAPP_PUBLISH_PROFILE`, `APIM_BASE_URL`, `APIM_SUBSCRIPTION_KEY`, variable `AZURE_WEBAPP_NAME`) y hacer push a `main`. Al primer arranque, Flyway crea las tablas y carga el seed | `https://<apim>/core/v1/health` = 200 |
| 5 | Jue 5 | Correr Postman con el environment `azure` (baseUrl de APIM + subscription key en *Current value*) | **Control 5 nov** |
| 6 | Vie 6 | `EVENTS_ENABLED=true`. Reservar desde Postman y ver el mensaje en **Service Bus Explorer**. Probar con P3 que MS3 lo consume | P3 recibe el evento |
| 7 | Vie 6 | Con P4: `APIM_CERT_CHECK=true` + `APIM_CERT_THUMBPRINT`. Verificar que `curl` directo al App Service da 403 y que vía APIM da 200 | Acceso directo bloqueado |
| 8 | Sáb 7 | Cuando P2 despliegue MS2: `SERVICE_CATALOG_MODE=http`, `DOMINIO_BASE_URL`, `APIM_SUBSCRIPTION_KEY`. Reservar un servicio real | La reserva usa datos de MS2 |
| 9 | Dom 8 | Prueba completa con P3 y P4: reservar desde el teléfono → push al mentor | **Control 8 nov** |
| 10 | Lun 9–Sáb 14 | Corregir lo que reporten P2 y P4 al integrar. Pasar la colección a P4. Capturas: Actions en verde, Azure SQL, árbol hexagonal | Newman verde contra APIM |

## 3. Cómo leer el código (en este orden)

1. `domain/model/Session.java`: la **máquina de estados**. Cada método (`accept`, `reject`, `reschedule`, `cancel`, `complete`) revisa quién lo hace y en qué estado está, cambia el estado, agrega al historial y deja un `SessionEvent`.
2. `domain/service/AvailabilityPolicy.java`: cuándo un día es reservable.
3. `domain/port/in` y `domain/port/out`: los contratos. El dominio **define** lo que necesita y la infraestructura lo **implementa**.
4. `application/service/SessionService.java`: el patrón de cada caso de uso es *cargar → regla del dominio → guardar → publicar*.
5. `infrastructure/config/BeanConfig.java`: dónde se conectan los casos de uso con los adaptadores.
6. `infrastructure/adapter/out/persistence/SessionPersistenceAdapter.java`: cómo se traduce el choque del índice único a `DAY_UNAVAILABLE`.
7. `infrastructure/adapter/out/messaging/ServiceBusEventPublisher.java` + `AfterCommit.java`: cómo se publica después del commit.
8. `infrastructure/security/*Filter.java`: correlation id → certificado de APIM → JWT.
9. Tests: `SessionTest` (las reglas, en orden de la tabla del contrato) y `HexagonalArchitectureTest`.

## 4. Recorrido de una reserva

```
POST /core/v1/sessions  (APIM: subscription key + validate-jwt + certificado de cliente)
 └─ CorrelationIdFilter → ApimClientCertFilter → JwtAuthFilter → SessionController.create
     └─ SessionService.create  (@Transactional)
         ├─ valida campos (serviceId, date, modality)
         ├─ ServiceCatalogPort.findService → MS2 vía APIM (snapshot del servicio)
         ├─ Session.checkCanRequest (no es tu servicio, modalidad ofrecida)
         ├─ AvailabilityPolicy.isDayAvailable (día de atención, no bloqueado, desde mañana, con cupo)
         ├─ Session.request → estado pendiente + SessionEvent(SESSION_REQUESTED → mentor)
         ├─ SessionRepositoryPort.save → Azure SQL (índice único: si alguien ganó el día → DAY_UNAVAILABLE)
         └─ EventPublisherPort.publish → se registra para "después del commit"
 commit en Azure SQL → ServiceBusEventPublisher envía el mensaje (messageId = eventId)
 ← 201 al cliente (no espera a MS3)
 MS3 consume → guarda en Table Storage → Socket.IO al mentor → push FCM
```

## 5. Preguntas probables de la defensa

**¿Por qué el dominio no depende de Spring?**
Las reglas del negocio (la máquina de estados, los días reservables) son lo que más vale y lo que menos debe cambiar. Si dependieran de Spring o de JPA, cambiar el framework o la base obligaría a tocarlas. ArchUnit hace fallar el build si alguien rompe la regla. Además se prueban en milisegundos, sin levantar nada.

**¿Qué pasa si cambian Azure SQL por otra base?**
Solo cambia `adapter/out/persistence`: entidades, repositorios y el adaptador que implementa `SessionRepositoryPort`. El dominio, los casos de uso y los controllers no se tocan.

**¿Por qué Azure SQL para MS1?**
Usuario → disponibilidad → sesión → historial son datos relacionados. Además se necesita integridad: una reserva no puede quedar a medias y dos personas no pueden ganar el mismo día. Eso lo garantizan las transacciones ACID y un índice único filtrado.

**¿Cómo evitan dos reservas el mismo día si llegan al mismo tiempo?**
Hay dos capas:
1. El dominio revisa la disponibilidad.
2. La base tiene `ux_sessions_service_day_active`, un índice único sobre (servicio, día) que solo cuenta las sesiones activas. Si dos inserciones llegan a la vez, la segunda falla en la base y se responde `DAY_UNAVAILABLE`.

Para dos cambios a la misma sesión, `@Version` detecta la edición simultánea y responde 409.

**¿Por qué publicar después del commit?**
Si se publicara antes y la transacción fallara, MS3 notificaría una sesión que no existe. Con *after commit* solo se anuncia lo que quedó guardado. El riesgo contrario es que Service Bus falle justo después del commit: el SDK reintenta 3 veces y, si aun así falla, se registra el `eventId`. La solución completa es el **patrón outbox**: guardar el evento en una tabla dentro de la misma transacción y publicarlo desde un job.

**¿Qué pasa si MS3 está caído?**
MS1 responde igual: el mensaje queda en la cola. Cuando MS3 vuelve, lo procesa. Eso es el desacople asíncrono.

**¿Cómo sabe MS1 que la llamada viene de APIM?**
Por tres cosas:
1. **Red:** el App Service solo acepta la IP de APIM.
2. **Certificado:** App Service exige certificado de cliente y lo pasa en `X-ARR-ClientCert`; `ApimClientCertFilter` compara su huella SHA-1 con `APIM_CERT_THUMBPRINT`.
3. **Token:** el JWT se vuelve a validar aquí, aunque APIM ya lo validó.

**¿Diferencia entre la subscription key y el JWT?**
La key identifica a la **aplicación** cliente (web, móvil, Postman). El JWT identifica al **usuario** y su rol. Se necesitan las dos.

**¿Por qué MS1 llama a MS2?**
Al reservar se necesita el título, la duración, el cupo y el mentor del servicio, que son de MS2. Se pide una vez y se guarda una copia (*snapshot*) dentro de la sesión, así la sesión no depende de MS2 después. Si MS2 no responde, se devuelve 503 en 3 segundos en lugar de colgarse.

**¿Por qué epoch?**
Un instante en epoch ms es el mismo en todo el mundo. El cliente lo convierte a la zona del dispositivo. Los días sin hora (el día de la sesión) van como `YYYY-MM-DD`, porque un día de calendario no tiene zona.

## 6. Comandos útiles

```bash
./mvnw -B verify                                         # todos los tests
docker compose up -d && PORT=8090 ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
npx newman run postman/MS1-Core.postman_collection.json -e postman/MS1-Core-local.postman_environment.json
curl -s localhost:8090/v3/api-docs.yaml > docs/openapi.yaml   # reexportar OpenAPI (quitar el bloque servers)
```
