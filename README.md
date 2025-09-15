# Airsen - Air Quality Monitoring Application

**Airsen** is a French air quality monitoring web application that provides real-time environmental data through a Spring Boot REST API backend.

## Features

- Air Quality Monitoring (ATMO indices and pollutant concentrations)
- Weather Data (current conditions and forecasts)
- Geographic Data (French regions, departments, communes)
- User Management (profiles, favorites, alerts)
- Forum System (community discussions)
- Export System (PDF and CSV reports)
- JWT Authentication with role-based authorization
- Interactive Swagger UI documentation

## Prerequisites

- Docker 20.0+ with Docker Compose v2.0+
- Git for version control

## Quick Start

### 1. Clone Repository
```bash
git clone https://github.com/Diginamic-Fil-rouge/Airsen-springboot-angular.git
cd Airsen-springboot-angular
```

### 2. Environment Setup
Create `.env` file:
```env
DB_HOST=82.165.152.149
DB_PORT=3307
DB_NAME=2025-d08b-g1
DB_USERNAME=main1
DB_PASSWORD=your_password
JWT_SECRET=your-super-secret-jwt-key-with-at-least-32-characters
REDIS_HOST=redis
REDIS_PORT=6379
```

### 3. Start Application
```bash
# Start backend services
docker-compose --profile backend up -d

# Access Swagger UI
http://localhost:8080/api/v1/swagger-ui/index.html
```

## Docker Environment

### Available Profiles

**Backend Profile:**
```bash
docker-compose --profile backend up -d
```
Services: Redis + Spring Boot API

**Full Stack Profile:**
```bash
docker-compose --profile full-stack up -d
```
Services: Redis + Backend + Angular Frontend

**Development Profile:**
```bash
docker-compose --profile development up -d
```
Services: All + Redis Commander (web GUI for Redis cache management)

### Container Management
```bash
# View status
docker-compose ps

# View logs
docker-compose logs -f airsen-backend

# Stop services
docker-compose down

# Clean rebuild
docker-compose down -v && docker-compose --profile backend build --no-cache
```

## Available Services

- **Swagger UI**: http://localhost:8080/api/v1/swagger-ui/index.html
- **Health Check**: http://localhost:8080/api/v1/test/health
- **Redis Commander**: http://localhost:8082 (development profile - Redis cache management)

## License

MIT License