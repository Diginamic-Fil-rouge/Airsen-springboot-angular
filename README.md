# Airsen - Environmental Information Exchange Platform

**Airsen** is an environmental information platform delivering real-time environmental data to French citizens. Built with Spring Boot and Angular, it integrates official monitoring sources (ATMO France, INSEE, Open-Meteo) with community-driven contributions.

## Key Features

- **Air Quality Monitoring**: Real-time ATMO indices and pollutant data from French monitoring stations
- **Weather Integration**: Current conditions and forecasts correlated with air quality
- **Community Forums**: Category-based discussions on environmental topics with voting system
- **Admin Alert System**: Automated detection of environmental signals (ATMO + Open-Meteo) with admin-approved broadcast notifications
- **Data Export**: PDF and CSV reports for personal tracking
- **Secure Access**: JWT authentication with role-based authorization (Visitor/User/Admin)

## Notification & Alert System

The platform implements an **admin-controlled broadcast system**:

### How It Works
1. **Automated Detection**: System monitors ATMO France (air quality episodes) and Open-Meteo (weather thresholds: heat ≥35°C, wind ≥70 km/h, rain ≥30mm/24h)
2. **Admin Alert Center**: Detected signals appear in admin dashboard with pre-filled notification templates
3. **Admin Review & Broadcast**: Admins review, edit, and approve notifications to be sent to users by geographic scope (France/Region/Department/Commune)
4. **User Notifications**: Users receive email notifications only when admins approve and send campaigns

**Key Points**:
- Users receive notifications sent by admins
- Admins receive alerts from external APIs and monitor thresholds
- No automatic user alerts without admin approval
- No user-defined thresholds or personalized alerts


## Technology Stack

- **Backend**: Spring Boot 3.2.0 (Java 21) + MariaDB 11.6 + Redis 7
- **Frontend**: Angular 20 + TypeScript + Node.js 22.19.0
- **Infrastructure**: Docker + Docker Compose with profiles
- **Security**: Spring Security with JWT + GDPR-compliant data handling

## Prerequisites

- Docker 20.0+ with Docker Compose v2.0+
- Git
- Available ports: 3306, 6379, 8080, 4200

## Quick Start

### 1. Clone Repository
```bash
git clone https://github.com/Diginamic-Fil-rouge/Airsen-springboot-angular.git
cd Airsen-springboot-angular
```

### 2. Configure Environment

Create `.env.dev` file in project root:

```env
# Database
DB_HOST=mariadb
DB_PORT=3306
DB_NAME=airsen_dev
DB_USERNAME=airsen_dev
DB_PASSWORD=dev_password
DB_ROOT_PASSWORD=root123

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# JWT (use strong secret in production!)
JWT_SECRET=your-super-secret-jwt-key-with-at-least-32-characters-base64-encoded

# Spring Boot
SPRING_PROFILES_ACTIVE=dev
BACKEND_PORT=8080
FRONTEND_PORT=4200

# External APIs
ATMO_API_URL=https://api.atmo-france.org/v1
ATMO_USERNAME=your_atmo_username
ATMO_PASSWORD=your_atmo_password
ATMO_JWT_TOKEN=your_atmo_jwt_token
OPEN_METEO_URL=https://api.open-meteo.com/v1
INSEE_API_URL=https://api.insee.fr/metadonnees/V1

# Email (use Gmail App Password)
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# Frontend
API_BASE_URL=http://localhost:8080/api/v1
```

### 3. Start Application

Choose the profile matching your needs:

```bash
# Development Mode
# Start all dev services with hot reload
docker-compose --env-file .env.dev --profile dev up

# Start specific services
docker-compose --env-file .env.dev --profile dev up api-dev web-dev mariadb redis

# With dev tools (Adminer, Redis Commander)
docker-compose --env-file .env.dev --profile dev up
```
```bash
# Production Mode
# Build and start production stack
docker-compose --profile prod up --build -d

# View logs
docker-compose --profile prod logs -f

# Scale services (if needed)
docker-compose --profile prod up --scale api=3
```
### 4. Verify Installation

```bash
# Check status
docker-compose ps

# Test backend
curl http://localhost:8080/actuator/health

# Access Swagger UI
open http://localhost:8080/api/v1/swagger-ui.html
```

## Docker Compose Commands

### Basic Operations

```bash
# Start services
docker-compose --profile dev backend up -d

# Stop services (keeps data)
docker-compose stop

# Stop and remove containers (keeps data)
docker-compose down

# Stop and remove everything including data (deletes database!)
docker-compose down -v

# View status
docker-compose ps

# View logs (follow mode)
docker-compose logs -f

# View logs for specific service
docker-compose logs -f api-dev

# Check health status
docker-compose --profile prod ps

# Rebuild specific service
docker-compose --profile dev up --build api-dev

# Clean everything
docker-compose down -v --remove-orphans
```

### Service Management

```bash
# Restart specific service
docker-compose restart api-dev

# Restart all services
docker-compose restart

# Start stopped services
docker-compose start

# Check resource usage
docker stats
```

## MariaDB Commands

```bash
# Access MariaDB console
docker-compose exec mariadb mariadb -u airsen_dev -pdev_password -D airsen_dev

# Run SQL query
docker-compose exec mariadb mariadb -u airsen_dev -pdev_password -D airsen_dev \
  -e "SELECT COUNT(*) FROM users;"

# Backup database
docker-compose exec mariadb mariadb-dump -u airsen_dev -pdev_password airsen_dev > backup.sql

# Restore database
docker-compose exec -T mariadb mariadb -u airsen_dev -pdev_password airsen_dev < backup.sql

# Reset database (deletes all data!)
docker-compose down -v
docker-compose --env-file .env.dev --profile backend up -d
```

## Redis Commands

```bash
# Access Redis CLI
docker-compose exec redis redis-cli

# Check Redis status
docker-compose exec redis redis-cli PING

# View all cached keys
docker-compose exec redis redis-cli KEYS "*"

# Clear all cache
docker-compose exec redis redis-cli FLUSHALL
```

## Available Endpoints

### Backend API
- **Base URL**: http://localhost:8080/api/v1
- **Swagger UI**: http://localhost:8080/api/v1/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

### Frontend (full-stack profile)
- **Angular App**: http://localhost:4200

## Troubleshooting

### Container Won't Start
```bash
# View logs
docker-compose logs api-dev

# Check status
docker-compose ps

# Check resources
docker stats
```

### Port Conflicts
```bash
# Check port usage
lsof -i :8080

# Change port in .env.local
BACKEND_PORT=8081

# Restart
docker-compose down
docker-compose --env-file .env.dev --profile backend up -d
```

### Database Issues
```bash
# Test connection
docker-compose exec mariadb mariadb -u airsen_dev -pdev_password \
  -e "SELECT 'Connected!' as status;"

# Check logs
docker-compose logs mariadb

# Verify configuration
docker-compose config
```

### Clean Everything
```bash
# Nuclear option: remove all containers and data
docker-compose down -v
docker system prune -a --volumes
docker-compose --profile backend build --no-cache
docker-compose --profile backend up -d
```


## Docker Profiles Summary

| Profile | Services | Use Case |
|---------|----------|----------|
| **backend** | MariaDB + Redis + API | API development |
| **full-stack** | All services | Complete application |
| **database** | MariaDB + Redis | Run backend/frontend locally |
| **frontend** | Angular | Frontend development |

## License

MIT License

---

**Built with ❤️ for French citizens**
