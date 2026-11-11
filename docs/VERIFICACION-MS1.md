# Verificación de MS1 — 11 de noviembre de 2026

## Evidencia comprobada

| Comprobación | Resultado |
|---|---|
| GitHub Actions | [Ejecución 42984973130, intento 6](https://github.com/kayy1y/ms-core-java/actions/runs/42984973130), compilación, pruebas, despliegue y comprobación de salud correctos |
| Versión desplegada | `da2fac47b78041fe8e2f5a825a438af3e4826226`; las ampliaciones siguientes de Postman no cambian la lógica Java |
| Salud pública | HTTP 200, `status=UP`, `service=ms-core-java` |
| Colección local ampliada | 83 solicitudes y 148 aserciones; cero fallos; salida 0 |
| Colección contra Azure | 83 solicitudes y 148 aserciones; cero fallos; salida 0; 26,3 s |
| Hora de finalización en Azure | 11 de noviembre, 03:06 a. m., America/Costa_Rica |
| Cobertura estática de rutas | 42 de 42 operaciones del OpenAPI tienen una solicitud con aserciones |

Destino probado:

```text
https://app-skillgrow-core-kay-epcahpd2ade0awfb.eastus-01.azurewebsites.net
```

Se utilizó la colección del repositorio con Postman CLI y `--no-report-events`.
Los registros completos quedan localmente en `target/qa-ms1/`, excluidos de Git.
Las pruebas realizan escrituras sobre datos de QA: crean una cuenta y una insignia inactiva,
cancelan sus reservas y eliminan sus reglas y bloqueos temporales.

## Comportamientos ejercitados

- Registro, acceso con contraseña, credenciales inválidas, correo duplicado y logout.
- Rechazo de token de Google inválido; no se comprobó aquí el acceso con Google real.
- Autenticación obligatoria, autorización administrativa y acceso exclusivo de participantes.
- Edición de perfil, conocimientos, habilidades, intereses y preferencias.
- Creación, edición y eliminación de disponibilidad, propiedad del servicio y fechas bloqueadas.
- Reservas, rechazo de duplicados, aceptación, reprogramación, cancelación, enlace y ubicación presencial.
- Restricción de completar una sesión futura; las demás transiciones se cubren además con las pruebas Java.
- Suspensión y reactivación de la cuenta QA y su efecto sobre el inicio de sesión.
- Consulta, creación, edición, activación y desactivación de insignias.

Tener una solicitud por operación no equivale a cubrir todos sus casos posibles.

## Configuración y límites de la prueba

- Web App Linux, Java 21 SE, plan F1; Azure SQL `coredb` con oferta gratuita y pausa al agotar el límite.
- `SERVICE_CATALOG_MODE=stub`: no se ha comprobado la conexión con MS2 real.
- `EVENTS_ENABLED=false`: no se ha comprobado la publicación en Service Bus ni la entrega de MS3.
- `APIM_CERT_CHECK=false`, prueba directa: APIM, certificados y bloqueo de acceso directo siguen pendientes.
- Calificaciones e insignias por reputación siguen necesitando el acuerdo de integración con MS2.

La Web App temporal `app-skillgrow-core-kay-f1` se creó para diagnosticar el bloqueo F1.
GitHub Actions sigue apuntando a la Web App principal; la temporal no contiene el despliegue de MS1.

## Incidencias encontradas

1. La URL de comprobación de GitHub no tenía `https://`; se corrigió su variable de entorno.
2. SQL rechazaba la salida de App Service con código 40615; se configuraron las IP declaradas para la Web App.
3. La instancia F1 reportó `QuotaExceeded` y `403.67 Site Disabled`, con contadores visibles en cero.
   Se ajustó la configuración de memoria y conexiones. Posteriormente el alojamiento se recuperó;
   no se identificó de forma concluyente qué contador provocó el bloqueo.

## Pendientes de integración de P1

1. MS2: dirección y contrato real de catálogo; comprobar el snapshot al reservar y los fallos del proveedor.
2. P4/P3: conexión de Service Bus; publicar una reserva y verificar su consumo y destinatario.
3. P4: APIM y certificados; activar el filtro ya implementado y ejecutar la colección mediante APIM.
4. Google: configurar el client ID acordado y verificar un ID token real.
5. P2: completar la información de reputación y acordar la evaluación de insignias por reseñas.

El despliegue inicial está comprobado. La integración distribuida completa todavía no se declara terminada.
