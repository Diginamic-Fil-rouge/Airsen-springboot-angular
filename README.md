# Airsen - Environmental Information Exchange Platform

**Airsen** is an environmental information platform designed to deliver French citizens with real-time environmental data. Built as a Spring Boot REST API, Airsen provides environmental information from official monitoring sources and comunity-drivent contributions. The goal is to make complex environmental data accessble, actinable and secure. 

## Mission Statement

Airsen connects official monitoring systems with user engagement. By combining verified environmental data with citizen input, it supports both short-term decision-making (daily activities) and long-term awareness (climate and health impact).

## Key Features

### Environmental Intelligence
- **Air Quality Monitoring**: Real-time ATMO indices and pollutant concentrations from official French monitoring stations
- **Weather Integration**: Current conditions and forecasts correlated with air quality data
- **Geographic Coverage**: Complete French administrative hierarchy (regions, departments, communes)

### Community Engagement
- **Discussion Forums**: Category-based community discussions on environmental topics
- **Citizen Reporting**: Share local environmental observations and experiences
- **Voting System**: Community-driven content validation and engagement

### Personalized Alerts
- **Smart Notifications**: Customizable air quality and weather alerts based on user preferences
- **Threshold Management**: Personal health-based alert configurations
- **Multi-channel Delivery**: Email and in-app notification systems

### Data & Insights
- **Open Data Integration**: ATMO France, INSEE, and Open-Meteo APIs
- **Export Capabilities**: PDF and CSV reports for personal tracking
- **Real-time Dashboard**: Live environmental data visualization

### Secure Platform
- **JWT Authentication**: Secure user authentication with role-based authorization (Visitor/User/Admin)
- **Privacy-First**: GDPR-compliant data handling and user privacy protection
- **API Documentation**: Interactive Swagger UI for developers and integrators

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