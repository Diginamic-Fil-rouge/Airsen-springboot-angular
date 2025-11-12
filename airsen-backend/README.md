# Airsen Backend

Spring Boot 3.2 service that aggregates ATMO, Open-Meteo and INSEE data, exposes REST and WebSocket APIs, secures access with JWT, and orchestrates email plus alert workflows for the Airsen platform.

---

## 1. Tech Overview

| Layer | Stack |
| --- | --- |
| Core Framework | Spring Boot 3.2 (Java 21) |
| Data Stores | MariaDB 11.x, Redis 7.x (cache, rate limiting, WebSocket sessions) |
| Integrations | ATMO France, Open-Meteo, INSEE |
| Tooling | Maven 3.9+, MapStruct, Resilience4j, Bucket4j, Testcontainers, JaCoCo |

Source layout follows standard Spring conventions:

```
airsen-backend/
├── src/main/java/…        # Controllers, services, schedulers
├── src/main/resources/     # application.yml and profile overrides
├── src/test/java/…         # Unit and integration tests
├── Dockerfile              # Multi-stage runtime image
└── pom.xml                 # Dependencies and build plugins
```

---

## 2. Prerequisites

| Tool | Version | Notes |
| --- | --- | --- |
| JDK | 21 (Temurin recommended) | Required to build and run |
| Maven | 3.9+ | Wrapper not committed, install globally |
| Docker | 24+ with Compose v2 | Needed for MariaDB, Redis, Nginx, local prod profile |

> Ports: backend listens on `8080`, MariaDB on `3307` (host), Redis on `6379`.

---

## 3. Environment Variables

Backend reads configuration from `application.yml` and profile overlays. Provide the following via `.env.local` (used by `docker-compose.yml`) or OS environment.

| Variable | Description |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` (default), `test`, or `prod` |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | MariaDB connection |
| `REDIS_HOST`, `REDIS_PORT` | Redis cache |
| `JWT_SECRET` | Base64-encoded signing key (at least 32 chars) |
| `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM_ADDRESS`, `MAIL_FROM_NAME` | SMTP credentials |
| `ATMO_API_URL`, `ATMO_USERNAME`, `ATMO_PASSWORD`, `ATMO_JWT_TOKEN` | ATMO API access |
| `OPEN_METEO_URL`, `INSEE_API_URL` | External API base URLs |
| `ENABLE_SCHEDULING`, `TIERED_SCHEDULER_ENABLED`, `ALERT_DETECTION_ENABLED` | Optional toggles |

Example `.env.local` (project root):

```dotenv
SPRING_PROFILES_ACTIVE=dev
DB_HOST=localhost
DB_PORT=3307
DB_NAME=airsen_dev
DB_USERNAME=airsen_dev
DB_PASSWORD=dev_password
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=change-me-min-32-chars
MAIL_USERNAME=your_gmail@gmail.com
MAIL_PASSWORD=app-specific-password
ATMO_API_URL=https://admindata.atmo-france.org
OPEN_METEO_URL=https://api.open-meteo.com/v1
INSEE_API_URL=https://api.insee.fr/metadonnees/V1
ENABLE_SCHEDULING=false
```

---

## 4. Local Development

1. **Start infrastructure**
   ```bash
   docker compose --profile backend up -d
   ```

2. **Run API from source**
   ```bash
   cd airsen-backend
   export SPRING_PROFILES_ACTIVE=dev
   mvn spring-boot:run
   ```

   Dev profile:
   - Scheduling disabled by default to avoid external API quotas.
   - Smaller cache TTLs, verbose logging, conservative rate limits.

3. **Access services**
   - REST base: `http://localhost:8080/api/v1`
   - Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`
   - Actuator health: `http://localhost:8080/actuator/health`

---

## 5. Testing and Quality

| Command | Description |
| --- | --- |
| `mvn clean test` | Unit and integration tests (spins up MariaDB and Redis via Testcontainers) |
| `mvn verify` | Tests plus JaCoCo coverage report at `target/site/jacoco/index.html` |

Testing tips:
- `SPRING_PROFILES_ACTIVE=test` disables schedulers, uses mock API clients.
- Docker must be running for Testcontainers.
- Set `TESTCONTAINERS_RYUK_DISABLED=false` in restricted environments.

---

## 6. Production Build and Deployment

### Jar
```bash
cd airsen-backend
mvn clean package -DskipTests
java -jar target/airsen-api-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

`application-prod.yml` enables full scheduling, production cache TTLs, analytics logging, and expects real credentials.

### Docker image
```bash
docker build -t airsen-api:prod -f Dockerfile .
```

### Compose stack
From repo root:
```bash
API_TARGET=prod WEB_TARGET=prod docker compose --profile prod up -d --build
```

Hardening checklist:
- Store secrets outside env files (Vault, AWS Secrets Manager, etc.).
- Provide dedicated MariaDB/Redis with backups.
- Set `ENABLE_SCHEDULING=true`, `TIERED_SCHEDULER_ENABLED=true`, `ALERT_DETECTION_ENABLED=true`.
- Configure `CORS_ALLOWED_ORIGINS` and logging via environment variables defined in `docker-compose.yml`.

---

## 7. Useful Commands

```bash
# Format or lint via Maven plugins (if configured)
mvn spotless:apply

# Inspect scheduled tasks (dev profile exposes endpoint)
curl http://localhost:8080/actuator/scheduledtasks

# Inspect caches
curl http://localhost:8080/actuator/caches
```

---

## 8. Troubleshooting

| Symptom | Fix |
| --- | --- |
| Cannot obtain JDBC connection | Ensure `docker compose --profile backend up -d` is running and credentials match `.env.local`. |
| Redis connection refused | Port 6379 in use; stop local Redis or change `REDIS_PORT`. |
| Frontend CORS failures | Set `CORS_ALLOWED_ORIGINS=http://localhost:4200`. |
| Tests hang while pulling containers | Pre-pull images and ensure Docker Desktop has network access. |
| Scheduling runs unexpectedly in dev | Verify `ENABLE_SCHEDULING=false` and profile is `dev`. |

For deeper comparisons of profile settings, review `application_CONFIG_COMPARISON.md`.
