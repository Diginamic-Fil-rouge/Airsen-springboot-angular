# Airsen - Environmental Information Exchange Platform

**Airsen** is an environmental monitoring platform delivering real-time air quality and weather data to French citizens. Built with Spring Boot 3.2.0 (Java 21) and Angular 20, it integrates official sources (ATMO France, Open-Meteo, INSEE) with community-driven discussions.

## Key Features

- **Real-Time Air Quality**: ATMO indices and pollutant data from French monitoring stations
- **Weather Integration**: Current conditions and 7-day forecasts correlated with air quality
- **Interactive Map**: Leaflet-based map with 35,000+ French communes, filtering, and clustering
- **Community Forums**: Category-based discussions with voting system
- **Admin Notification System**: Automated signal detection with admin-approved email broadcasts
- **Data Export**: Client-side PDF and CSV generation for personal tracking
- **Secure Access**: JWT authentication with role-based authorization (Visitor/User/Admin)

## Technology Stack

| Category | Technology | Version |
|----------|-----------|---------|
| **Backend** | Spring Boot | 3.2.0 |
| **Language** | Java | 21 |
| **Database** | MariaDB | 11.6 |
| **Cache** | Redis | 7 |
| **Frontend** | Angular | 20 |
| **Infrastructure** | Docker Compose | v2.0+ |

**Full Stack**: Spring Boot + Angular + MariaDB + Redis + Docker

## Quick Start

### Prerequisites

- Docker 20.0+ with Docker Compose v2.0+
- Git
- Available ports: 3306, 6379, 8080, 4200

### 1. Clone Repository

```bash
git clone https://github.com/Diginamic-Fil-rouge/Airsen-springboot-angular.git
cd Airsen-springboot-angular
```

### 2. Configure Environment

Copy `.env.example` to `.env.dev` and configure:

```env
# Database
DB_HOST=mariadb
DB_NAME=airsen_dev
DB_USERNAME=airsen_dev
DB_PASSWORD=dev_password

# Redis
REDIS_HOST=redis

# JWT Secret (generate with: openssl rand -base64 32)
JWT_SECRET=your-base64-encoded-secret-32-characters-minimum

# External APIs
ATMO_USERNAME=your_atmo_username
ATMO_PASSWORD=your_atmo_password
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_gmail_app_password

# Frontend
API_BASE_URL=http://localhost:8080/api/v1
```

See `.env.example` for complete configuration options.

### 3. Start Application

**Backend Only** (API + Database):
```bash
docker-compose --env-file .env.dev --profile backend up -d
```

**Full Stack** (Backend + Frontend):
```bash
docker-compose --env-file .env.dev --profile full-stack up -d
```

**Production** (with Nginx reverse proxy):
```bash
docker-compose --env-file .env.prod --profile prod up -d
```

### 4. Verify Installation

```bash
# Check services status
docker-compose ps

# Test backend health
curl http://localhost:8080/actuator/health

# Access Swagger UI (API documentation)
open http://localhost:8080/api/v1/swagger-ui.html

# Access frontend (if full-stack profile)
open http://localhost:4200
```

## Development Commands

### Backend (Spring Boot)

```bash
cd airsen-backend

# Run tests with coverage
mvn test jacoco:report
open target/site/jacoco/index.html

# Run single test
mvn test -Dtest=CommuneServiceTest

# Build JAR
mvn clean package

# Run locally (requires MariaDB + Redis)
mvn spring-boot:run
```

### Frontend (Angular)

```bash
cd airsen-frontend

# Install dependencies
npm install

# Start dev server
npm start  # http://localhost:4200

# Run tests
npm test

# Run tests with coverage
npm run test:coverage
open coverage/index.html

# Build for production
npm run build
```

## Docker Compose Profiles

| Profile | Services | Use Case |
|---------|----------|----------|
| **backend** | MariaDB + Redis + API | Backend development |
| **full-stack** | All services | Full application |
| **prod** | All + Nginx | Production deployment |
| **dev** | + Redis Commander | Development with tools |

## Essential Docker Commands

```bash
# Start services
docker-compose --env-file .env.dev --profile backend up -d

# View logs
docker-compose logs -f api

# Stop services (keeps data)
docker-compose stop

# Stop and remove containers (keeps data)
docker-compose down

# Stop and remove everything including data (DELETES DATABASE!)
docker-compose down -v

# Restart specific service
docker-compose restart api

# Rebuild service
docker-compose --env-file .env.dev --profile backend up --build -d api

# Check resource usage
docker stats
```

## Database & Cache Management

**MariaDB**:
```bash
# Access console
docker-compose exec mariadb mariadb -u airsen_dev -pdev_password -D airsen_dev

# Run query
docker-compose exec mariadb mariadb -u airsen_dev -pdev_password -D airsen_dev \
  -e "SELECT COUNT(*) FROM communes;"

# Backup
docker-compose exec mariadb mariadb-dump -u airsen_dev -pdev_password airsen_dev > backup.sql

# Restore
docker-compose exec -T mariadb mariadb -u airsen_dev -pdev_password airsen_dev < backup.sql
```

**Redis**:
```bash
# Access CLI
docker-compose exec redis redis-cli

# Check status
docker-compose exec redis redis-cli PING

# View cached keys
docker-compose exec redis redis-cli KEYS "*"

# Clear cache
docker-compose exec redis redis-cli FLUSHALL
```

## Key URLs

| Service | URL |
|---------|-----|
| Backend API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/api/v1/swagger-ui.html |
| Health Check | http://localhost:8080/actuator/health |
| Frontend | http://localhost:4200 |
| Redis Commander | http://localhost:8082 (dev profile) |

## Testing

**Backend**:
- Unit Tests: JUnit 5 + Mockito
- Integration Tests: TestContainers (MariaDB + Redis)
- Coverage Target: 80% (enforced by JaCoCo)

**Frontend**:
- Unit Tests: Jasmine + Karma
- E2E Tests: Cypress
- Coverage Target: 80%

**Run All Tests**:
```bash
# Backend
cd airsen-backend && mvn test

# Frontend
cd airsen-frontend && npm test
```

## Troubleshooting

**Container Won't Start**:
```bash
docker-compose logs api
docker-compose ps
docker stats
```

**Port Conflicts**:
```bash
# Check port usage
lsof -i :8080

# Change port in .env.dev
BACKEND_PORT=8081
```

**Database Issues**:
```bash
# Test connection
docker-compose exec mariadb mariadb -u airsen_dev -pdev_password \
  -e "SELECT 'Connected!' as status;"

# Reset database (WARNING: deletes all data)
docker-compose down -v
docker-compose --env-file .env.dev --profile backend up -d
```

**Clean Everything**:
```bash
docker-compose down -v
docker system prune -a --volumes
docker-compose --env-file .env.dev --profile backend build --no-cache
```

## CI/CD Pipeline

**GitHub Actions** (`.github/workflows/ci-cd.yml`):
1. Build & test backend (Maven + JaCoCo)
2. Build & test frontend (npm + Karma)
3. Lint & security scan
4. Docker build (multi-stage)
5. Deploy (production only)

**Coverage Requirements**: 80% for backend and frontend (enforced in CI)

## Key Architectural Decisions

1. **Admin-Controlled Notifications**: Users receive notifications only when admins approve broadcasts (NO user-configurable thresholds)
2. **Client-Side Exports**: PDFs and CSVs generated in browser (zero server load, infinite scalability)
3. **Geodistance Fallback**: 20km threshold for communes without direct monitoring data (Haversine distance)
4. **Commune-Centric Geography**: Frontend loads all communes once, filters client-side (optimized for map)
5. **TestContainers**: Integration tests use real MariaDB + Redis (production-matching)


## License

MIT License

---

**Built with ❤️ for French citizens**
