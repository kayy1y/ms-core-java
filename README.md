# ms-core-java — MS1 Core de SkillGrow

Microservicio principal del Proyecto 2: usuarios, login, disponibilidad del mentor y sesiones de mentoría.
Java 21 · Spring Boot 3.5 · arquitectura hexagonal · Azure SQL · publica eventos en Azure Service Bus.

> README en construcción. La versión completa se escribe al cerrar el servicio.

## Correr en local

```bash
docker compose up -d                                   # SQL Server 2022 + base coredb
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
curl http://localhost:8080/health
```

## Tests

```bash
./mvnw -B verify
```
