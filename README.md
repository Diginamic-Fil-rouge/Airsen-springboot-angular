# Airsen - Environmental Monitoring Platform

**Airsen** is a full-stack environmental monitoring platform delivering real-time air quality and weather data to French citizens. Built with Spring Boot 3.2.0 (Java 21) backend and Angular 20 (NgModule-based) frontend, integrating ATMO France OAuth2, Open-Meteo, and INSEE official data sources. Supports 35,000+ communes with interactive mapping, community forums, and admin-controlled notifications.

## Key Features

- **Real-Time Air Quality**: ATMO indices (1-6 scale) with pollutant measurements (PM10, PM2.5, O3, NO2, SO2)
- **Weather Integration**: Current conditions and 7-day forecasts correlated with air quality
- **Interactive Map**: Leaflet-based map with 35,000+ French communes, clustering, color-coded AQI
- **Geodistance Fallback**: 20km radius for communes without direct monitoring (Haversine distance)
- **Community Forums**: Category-based discussions with voting and reputation system
- **Admin Notifications**: Automated alert signals with admin-approval workflow
- **Data Export**: Client-side PDF/CSV generation (zero server load)
- **Authentication**: JWT (24h access, 7d refresh), BCrypt passwords, role-based access

## Technology Stack

| Layer | Tech Stack | Version |
|-------|-----------|---------|
| **Backend** | Spring Boot + Java | 3.2.0 / 21 LTS |
| **Frontend** | Angular + Material (NgModule-based) | 20 / v7 |
| **Database** | MariaDB (UTF-8MB4) | 11.6 |
| **Cache** | Redis (LRU, 256MB) | 7.4 |
| **Security** | JWT (HS512) + BCrypt | jjwt 0.11.5 / strength 12 |
| **Testing** | JUnit 5 + TestContainers + Jasmine | Latest |

## Quick Start

### Prerequisites
- Docker 20.0+ with Compose v2.0+
- Git
- Ports available: 3306, 6379, 8080, 4200

### Setup

```bash
# 1. Clone and configure
git clone https://github.com/Diginamic-Fil-rouge/Airsen-springboot-angular.git
cd Airsen-springboot-angular
cp .env.example .env.dev

# 2. Edit .env.dev with your credentials (DB_PASSWORD, JWT_SECRET, ATMO credentials, etc.)
# See .env.example for complete configuration

# 3. Start application
make dev                    # Full stack (backend + frontend)
# OR
make dev-backend           # Backend only (port 8080)
docker-compose -f docker-compose.yml --env-file .env.dev --profile full-stack up -d

# 4. Verify
curl http://localhost:8080/actuator/health
open http://localhost:4200  # Frontend
open http://localhost:8080/api/v1/swagger-ui.html  # API docs
```

## Development Commands

### Backend (Spring Boot)
```bash
cd airsen-backend
mvn test jacoco:report                          # Tests + coverage
mvn test -Dtest=CommuneServiceTest              # Specific test
mvn clean package                               # Build JAR
mvn spring-boot:run                             # Run locally (requires DB + Redis)
```

### Frontend (Angular)
```bash
cd airsen-frontend
npm install && npm start                        # Dev server on :4200
npm test && npm run test:coverage               # Tests + coverage
npm run build                                   # Production build
```

### Docker Management
```bash
make logs                   # All service logs
make logs-backend           # Backend only
make status                 # Container status
make health                 # Service health
make down                   # Stop services
make rebuild                # Clean rebuild
```

## Database & Cache

**MariaDB** (via Docker):
```bash
docker-compose exec mariadb mariadb -u airsen_dev -pdev_password -D airsen_dev
SELECT COUNT(*) FROM communes;  # 35,000+ communes
```

**Redis** (via Docker):
```bash
docker-compose exec redis redis-cli
KEYS "*"                    # View cached keys
FLUSHALL                    # Clear cache
```

## Key Architecture Patterns

1. **Layered 3-Tier**: Controller → Service → Repository (one-way dependencies)
2. **Admin-Controlled Notifications**: No user-configurable thresholds (spam prevention)
3. **Circuit Breaker + Retry**: Resilience4j for ATMO/Open-Meteo APIs
4. **Smart Caching**: Redis (1h TTL air quality/weather, 24h communes) + stale-cache serving (6h on failure)
5. **Client-Side Exports**: PDFs/CSVs generated in browser for infinite scalability
6. **Geodistance Fallback**: Automatic Haversine distance calculation for 20km radius
7. **TestContainers**: Integration tests use real MariaDB + Redis (production-matching)

## Security Model

| Layer | Implementation |
|-------|-----------------|
| **HTTP** | HTTPS (prod), CORS validation |
| **Authentication** | JWT HS512, 24h access + 7d refresh, blacklist logout |
| **Authorization** | Role-based: VISITOR (read-only), USER (data + forum), ADMIN (management) |
| **Data** | Parameterized queries, SQL injection protection |
| **Password** | BCrypt strength 12 (~200-250ms per login) |
| **GDPR** | Soft-delete (30-day grace), profile visibility controls |
| **Audit** | AdminActionLog tracks privileged operations |

## REST API Summary

| Domain | Endpoints | Access |
|--------|-----------|--------|
| **Auth** | POST /auth/login, register, refresh, logout | Public |
| **Communes** | GET /communes, /departments/{id}/communes | USER/ADMIN |
| **Data** | GET /atmo/{id}/latest, /weather/{id}/current | USER/ADMIN |
| **Favorites** | POST/DELETE /favorites, GET /favorites | USER |
| **Forum** | GET /forum/*, POST/PUT/DELETE manage | GET: public, modify: USER |
| **Admin** | GET /admin/statistics, POST /admin/notifications | ADMIN only |

## Testing

**Backend**: 80% coverage target (JaCoCo enforcement)
```bash
cd airsen-backend && mvn test jacoco:report
open target/site/jacoco/index.html
```

**Frontend**: 80% coverage target
```bash
cd airsen-frontend && npm run test:coverage
open coverage/index.html
```

## CI/CD Pipeline

GitHub Actions (`.github/workflows/`) runs:
1. Backend: Maven test + JaCoCo (80% coverage)
2. Frontend: npm test + Karma (80% coverage)
3. Linting & security scan
4. Docker multi-stage build
5. Deploy to Railway (prod only)

## Key URLs

| Service | URL |
|---------|-----|
| **Backend API** | http://localhost:8080/api/v1 |
| **Swagger UI** | http://localhost:8080/api/v1/swagger-ui.html |
| **Health** | http://localhost:8080/actuator/health |
| **Frontend** | http://localhost:4200 |
| **Redis Commander** | http://localhost:8082 (dev profile) |

## Troubleshooting

**Port Conflicts**: Check `lsof -i :8080` and adjust BACKEND_PORT in .env.dev

**Database Connection**:
```bash
docker-compose exec mariadb mariadb -u airsen_dev -pdev_password -e "SELECT 'Connected!' as status;"
```

**Clean Start** (WARNING: deletes all data):
```bash
docker-compose down -v
docker-compose --env-file .env.dev --profile full-stack up -d
```

## License

MIT License

---

**Last Updated:** February 11, 2026 | **Branch:** refactor/map | **Phase 2 Status:** In Progress

---

**Airsen**: Delivering environmental data to French citizens | Built with care by the development team
