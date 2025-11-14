# Scheduling Configuration Guide

This document explains the scheduling configuration for Airsen's data synchronization with external APIs (ATMO, OpenMeteo, INSEE).

## Table of Contents

- [Overview](#overview)
- [Profile-Based Configuration](#profile-based-configuration)
- [Quick Start](#quick-start)
- [Detailed Configuration](#detailed-configuration)
- [Rate Limiting](#rate-limiting)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)

---

## Overview

Airsen synchronizes data from three main external APIs:

1. **ATMO API** - Air quality data for French communes
2. **OpenMeteo API** - Weather data (temperature, wind, precipitation)
3. **INSEE API** - French administrative data (population, commune details)

### Current Scheduling (Production)

| Task | Schedule | Frequency | API Calls/Day |
|------|----------|-----------|---------------|
| ATMO Sync | `0 0 10 * * *` | Daily at 10:00 AM | ~35,000 |
| Weather Sync | `0 30 10 * * *` | Daily at 10:30 AM | ~35,000 |
| Air Quality Cache | `0 0 * * * *` | Hourly | Varies |
| Weather Cache | `0 */30 * * * *` | Every 30 min | Varies |
| Tiered Refresh | Various | Population-based | ~47,000 total |

### Intelligent Tiered Scheduling

To reduce API calls by **98%**, the system uses population-based tiers:

- **Tier 1** (≥100,000 pop): Updated every 2 hours (~200 cities)
- **Tier 2** (10,000-99,999 pop): Updated every 6 hours (~1,500 cities)
- **Tier 3** (<10,000 pop): Updated daily (~33,000 villages)

**Result**: ~47,000 API calls/day (vs. 2.6M without tiering)

---

## Profile-Based Configuration

The application supports three profiles, each with different scheduling strategies:

### 1. **Development Profile** (`application-dev.yml`)

**Purpose**: Local development with minimal API usage

**Key Features**:
- ✅ Scheduling **DISABLED by default** (`ENABLE_SCHEDULING=false`)
- ✅ Weekly schedules (Monday only) when enabled
- ✅ Small sample sizes (50 communes)
- ✅ Conservative rate limits (5-10 req/min)
- ✅ Verbose logging for debugging
- ✅ Long timeouts for debugging

**When to Use**: Local development, debugging, feature development

**Environment Variables**:
```bash
SPRING_PROFILES_ACTIVE=dev
ENABLE_SCHEDULING=false  # Keep disabled during development
```

### 2. **Test Profile** (`application-test.yml`)

**Purpose**: Automated testing with no real API calls

**Key Features**:
- ✅ All scheduling **ALWAYS DISABLED**
- ✅ Mock APIs via WireMock
- ✅ In-memory H2 database
- ✅ No Redis cache (predictable tests)
- ✅ Minimal logging
- ✅ Fast test execution

**When to Use**: Unit tests, integration tests, CI/CD pipelines

**Usage**:
```java
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
class WeatherServiceTest { ... }
```

### 3. **Production Profile** (`application-prod.yml`)

**Purpose**: Production deployment with full scheduling

**Key Features**:
- ✅ All scheduling **ENABLED**
- ✅ Optimized schedules (daily/hourly)
- ✅ Tiered refresh for efficiency
- ✅ Full rate limiting and circuit breakers
- ✅ Monitoring and alerting
- ✅ Performance tuning

**When to Use**: Production environments, staging

**Environment Variables**:
```bash
SPRING_PROFILES_ACTIVE=prod
ENABLE_SCHEDULING=true
TIERED_SCHEDULER_ENABLED=true
```

---

## Quick Start

### For Developers (Local Development)

1. **Copy the example environment file**:
   ```bash
   cp .env.example .env
   ```

2. **Ensure scheduling is DISABLED** (default in `.env.example`):
   ```bash
   ENABLE_SCHEDULING=false
   SPRING_PROFILES_ACTIVE=dev
   ```

3. **Start the application**:
   ```bash
   docker-compose up
   ```

4. **To manually trigger a sync** (when needed):
   ```bash
   # Option 1: Temporarily enable for one run
   ENABLE_SCHEDULING=true mvn spring-boot:run

   # Option 2: Call service methods from tests
   # Option 3: Use management endpoints (if implemented)
   ```

### For Testing

```bash
# Run tests with test profile
mvn test -Dspring.profiles.active=test

# Or specify in test class
@SpringBootTest
@ActiveProfiles("test")
class MyTest { ... }
```

### For Production

```bash
# Set environment variables
export SPRING_PROFILES_ACTIVE=prod
export ENABLE_SCHEDULING=true
export TIERED_SCHEDULER_ENABLED=true

# Required API credentials
export ATMO_USERNAME=your_username
export ATMO_PASSWORD=your_password
export OPEN_METEO_URL=https://api.open-meteo.com/v1
export INSEE_API_URL=https://geo.api.gouv.fr

# Start application
java -jar airsen-backend.jar
```

---

## Detailed Configuration

### Master Scheduling Control

All profiles support a **master switch** to enable/disable scheduling:

```yaml
scheduling:
  enabled: true  # or false
```

**Environment Variable**: `ENABLE_SCHEDULING=true/false`

When `false`, ALL scheduled tasks are disabled:
- ATMO sync
- Weather sync
- Cache refresh
- Alert detection
- Tiered scheduler

### ATMO Air Quality Sync

**Configuration**:
```yaml
scheduling:
  atmo:
    cron: 0 0 10 * * *  # Daily at 10 AM
    timezone: Europe/Paris
    batch-size: 50
    max-retries: 3
```

**Environment Variables**:
- `ATMO_SCHEDULE_CRON`: Cron expression
- `ATMO_SCHEDULE_TIMEZONE`: Timezone
- `ATMO_BATCH_SIZE`: Number of communes per batch

**Rate Limit**: 60 requests/minute (production), 30 (dev)

**Location**: `AtmoIntegrationService.java:78`

### Weather Sync

**Configuration**:
```yaml
scheduling:
  weather:
    cron: 0 30 10 * * *  # Daily at 10:30 AM (30 min after ATMO)
    timezone: Europe/Paris
    batch-size: 100
    delay-between-batches-ms: 60000  # 1 minute
    delay-between-requests-ms: 6000  # 6 seconds (~10 req/min)
    sample-size: -1  # -1 = all communes, 50 = sample 50
    sample-strategy: top-population  # or 'random'
```

**Environment Variables**:
- `WEATHER_SCHEDULE_CRON`: Cron expression
- `WEATHER_BATCH_SIZE`: Communes per batch
- `WEATHER_BATCH_DELAY`: Milliseconds between batches
- `WEATHER_REQUEST_DELAY`: Milliseconds between requests
- `WEATHER_SAMPLE_SIZE`: Number to sample (-1 for all)
- `WEATHER_SAMPLE_STRATEGY`: `top-population`, `random`, or `all`

**Rate Limit**: 10 requests/minute (production), 5 (dev)

**Location**: `WeatherService.java:42`

### Cache Refresh Schedules

**Configuration**:
```yaml
cache:
  refresh:
    air-quality-cron: 0 0 * * * *  # Hourly
    weather-cron: 0 */30 * * * *  # Every 30 min
    statistics-cron: 0 */15 * * * *  # Every 15 min
    cleanup-cron: 0 0 3 * * *  # Daily at 3 AM
```

**Environment Variables**:
- `CACHE_AIR_QUALITY_CRON`
- `CACHE_WEATHER_CRON`
- `CACHE_STATS_CRON`
- `CACHE_CLEANUP_CRON`

**Location**: `CacheRefreshScheduler.java`

### Tiered Intelligent Scheduler

**Configuration**:
```yaml
tiered-scheduler:
  enabled: true  # Recommended for production

  tier1:  # Major cities
    cron: 0 0 */2 * * *  # Every 2 hours
    population-threshold: 100000
    stagger-ms: 100
    inter-request-delay-ms: 50

  tier2:  # Medium cities
    cron: 0 0 1,7,13,19 * * *  # Every 6 hours
    population-min: 10000
    population-max: 99999
    stagger-ms: 200
    inter-request-delay-ms: 75

  tier3:  # Small communes
    cron: 0 0 2 * * *  # Daily
    population-threshold: 10000
    batch-size: 1000
    stagger-ms: 300
    inter-request-delay-ms: 100
    batch-pause-ms: 5000
```

**Environment Variable**: `TIERED_SCHEDULER_ENABLED=true/false`

**Location**: `CacheAwareTieredScheduler.java`

**Impact**:
- Reduces API calls from 2.6M/day to ~47K/day
- Smart cache-aware refresh (only before expiration)
- Geodistance fallback (20km radius)

---

## Rate Limiting

### External API Limits

| API | Production Limit | Development Limit | Timeout |
|-----|------------------|-------------------|---------|
| **ATMO** | 60 req/min | 30 req/min | 10s (15s dev) |
| **OpenMeteo** | 10 req/min | 5 req/min | 20s (30s dev) |
| **INSEE** | 30 req/min | 10 req/min | 5s (10s dev) |

### Rate Limiter Configuration

```yaml
rate-limiter:
  atmo:
    requests-per-minute: 60
    reject-on-limit: true
    circuit-breaker:
      enabled: true
      failure-threshold: 0.5  # 50%
      wait-duration-seconds: 60
```

**Environment Variables**:
- `ATMO_RATE_LIMIT`
- `WEATHER_RATE_LIMIT`
- `INSEE_RATE_LIMIT`

### Retry Logic

All API clients use `@Retryable`:

```java
@Retryable(
    maxAttempts = 3,
    backoff = @Backoff(delay = 500)
)
```

**Retry Sequence**: 500ms → 1000ms → 1500ms

---

## Monitoring

### Actuator Endpoints

**Development**:
```bash
# Health check
curl http://localhost:8080/api/v1/actuator/health

# Scheduled tasks
curl http://localhost:8080/api/v1/actuator/scheduledtasks

# Cache statistics
curl http://localhost:8080/api/v1/actuator/caches

# Metrics
curl http://localhost:8080/api/v1/actuator/metrics
```

**Production** (secured):
```bash
# Health (public)
curl https://api.airsen.fr/api/v1/actuator/health

# Prometheus metrics (for Grafana)
curl https://api.airsen.fr/api/v1/actuator/prometheus
```

### Logging

**Development** (verbose):
```yaml
logging:
  level:
    fr.airsen.api.scheduler: DEBUG
    fr.airsen.api.external.client: TRACE
```

**Production** (minimal):
```yaml
logging:
  level:
    fr.airsen.api.scheduler: INFO
    fr.airsen.api.external.client: WARN
```

### Key Metrics to Monitor

1. **API Call Counts**:
   - Daily ATMO calls: ~35,000 (47,000 with tiered)
   - Daily Weather calls: ~35,000 (included in tiered)
   - Alert if > 80% of limit

2. **Cache Hit Rates**:
   - Target: >70%
   - Alert if <50%

3. **Scheduler Execution**:
   - Success/failure rates
   - Execution duration
   - Alert if >5% failure rate

4. **Response Times**:
   - Target: <500ms average
   - Alert if >5s

---

## Troubleshooting

### Problem: API Rate Limit Exceeded

**Symptoms**:
```
429 Too Many Requests
Rate limit exceeded for ATMO API
```

**Solutions**:

1. **Disable scheduling during development**:
   ```bash
   ENABLE_SCHEDULING=false
   ```

2. **Reduce batch sizes**:
   ```bash
   WEATHER_BATCH_SIZE=10
   ATMO_BATCH_SIZE=25
   ```

3. **Increase delays**:
   ```bash
   WEATHER_REQUEST_DELAY=12000  # 12 seconds = 5 req/min
   ```

4. **Enable tiered scheduler** (production):
   ```bash
   TIERED_SCHEDULER_ENABLED=true
   ```

### Problem: Tests Failing Due to Scheduling

**Symptoms**:
```
Tests fail randomly due to background scheduled tasks
```

**Solution**:

Ensure test profile is active:
```java
@SpringBootTest
@ActiveProfiles("test")  // This disables all scheduling
class MyTest { ... }
```

### Problem: No Data Being Updated

**Symptoms**:
```
Database shows stale data
Scheduled tasks not running
```

**Solutions**:

1. **Check if scheduling is enabled**:
   ```bash
   # In .env or environment
   ENABLE_SCHEDULING=true
   ```

2. **Verify profile**:
   ```bash
   SPRING_PROFILES_ACTIVE=prod  # or dev with ENABLE_SCHEDULING=true
   ```

3. **Check scheduler status**:
   ```bash
   curl http://localhost:8080/api/v1/actuator/scheduledtasks
   ```

4. **Check logs**:
   ```bash
   docker-compose logs backend | grep "scheduler"
   ```

### Problem: Slow Performance During Development

**Symptoms**:
```
Application slow to start
High CPU usage
Constant API calls
```

**Solutions**:

1. **Disable scheduling**:
   ```bash
   ENABLE_SCHEDULING=false
   ```

2. **Disable tiered scheduler**:
   ```bash
   TIERED_SCHEDULER_ENABLED=false
   ```

3. **Use sample size**:
   ```bash
   WEATHER_SAMPLE_SIZE=50  # Only update 50 communes
   ```

### Problem: Authentication Errors with ATMO API

**Symptoms**:
```
401 Unauthorized
ATMO API authentication failed
```

**Solutions**:

1. **Check credentials**:
   ```bash
   # In .env
   ATMO_USERNAME=your_username
   ATMO_PASSWORD=your_password
   ```

2. **Or use JWT token**:
   ```bash
   ATMO_JWT_TOKEN=your_jwt_token
   ```

3. **Verify token cache** (expires after 24h):
   ```bash
   # Clear Redis cache
   docker-compose exec redis redis-cli FLUSHALL
   ```

---

## Cron Expression Guide

Common cron patterns used in the application:

| Expression | Meaning |
|------------|---------|
| `0 0 10 * * *` | Daily at 10:00 AM |
| `0 30 10 * * *` | Daily at 10:30 AM |
| `0 0 * * * *` | Every hour (top of the hour) |
| `0 */30 * * * *` | Every 30 minutes |
| `0 */15 * * * *` | Every 15 minutes |
| `0 0 3 * * *` | Daily at 3:00 AM |
| `0 0 */2 * * *` | Every 2 hours |
| `0 0 1,7,13,19 * * *` | At 1 AM, 7 AM, 1 PM, 7 PM |
| `0 0 10 * * MON` | Monday at 10:00 AM |
| `0 0 2 1 1 *` | January 1st at 2:00 AM (yearly) |

**Format**: `second minute hour day-of-month month day-of-week`

---

## Environment Variables Reference

### Quick Reference Table

| Variable | Dev Default | Test Default | Prod Default | Description |
|----------|-------------|--------------|--------------|-------------|
| `ENABLE_SCHEDULING` | `false` | `false` | `true` | Master switch |
| `TIERED_SCHEDULER_ENABLED` | `false` | `false` | `true` | Intelligent tiering |
| `ATMO_SCHEDULE_CRON` | `0 0 10 * * MON` | Never | `0 0 10 * * *` | ATMO sync schedule |
| `WEATHER_SCHEDULE_CRON` | `0 30 10 * * MON` | Never | `0 30 10 * * *` | Weather sync schedule |
| `WEATHER_SAMPLE_SIZE` | `50` | `5` | `-1` (all) | Communes to update |
| `ATMO_RATE_LIMIT` | `30` | `1000` | `60` | Requests per minute |
| `WEATHER_RATE_LIMIT` | `5` | `1000` | `10` | Requests per minute |

### Full List

See [.env.example](.env.example) for complete list with descriptions.

---

## Best Practices

### Development

1. ✅ **Keep scheduling DISABLED** by default (`ENABLE_SCHEDULING=false`)
2. ✅ Use **small sample sizes** (`WEATHER_SAMPLE_SIZE=50`)
3. ✅ Run syncs **manually** when needed, not on schedule
4. ✅ Use **verbose logging** to debug issues
5. ✅ **Mock external APIs** for unit tests

### Testing

1. ✅ **Always use `@ActiveProfiles("test")`** in tests
2. ✅ Use **WireMock** for integration tests
3. ✅ Keep **test data minimal** (5-10 records)
4. ✅ Use **TestContainers** for database/Redis tests
5. ✅ **Never call real APIs** from tests

### Production

1. ✅ **Enable tiered scheduler** (`TIERED_SCHEDULER_ENABLED=true`)
2. ✅ **Monitor API usage** (Grafana/Prometheus)
3. ✅ Set up **alerting** for rate limits
4. ✅ Configure **circuit breakers** for resilience
5. ✅ Implement **data retention policies**
6. ✅ **Backup database** before major updates
7. ✅ Test scheduling in **staging** environment first

---

## Related Files

- **Configuration Files**:
  - `application.yml` - Base configuration
  - `application-dev.yml` - Development profile
  - `application-test.yml` - Test profile
  - `application-prod.yml` - Production profile

- **Scheduler Classes**:
  - `CacheRefreshScheduler.java` - Cache refresh tasks
  - `CacheAwareTieredScheduler.java` - Intelligent tiered refresh
  - `AtmoIntegrationService.java:78` - ATMO sync
  - `WeatherService.java:42` - Weather sync
  - `AlertSignalDetectionService.java:35` - Alert detection
  - `InseeDataScheduler.java:23` - INSEE sync (yearly)

- **Configuration Classes**:
  - `SchedulingConfig.java` - Thread pool configuration
  - `AtmoApiConfig.java` - ATMO API settings
  - `OpenMeteoApiConfig.java` - OpenMeteo API settings
  - `WebClientConfig.java` - HTTP client configuration

---

## Support

For questions or issues:

1. Check this documentation
2. Review `.env.example` for configuration examples
3. Check application logs: `docker-compose logs backend`
4. Verify actuator endpoints: `/actuator/health`, `/actuator/scheduledtasks`
5. Contact the development team

---

**Last Updated**: 2025-11-07
**Version**: 1.0.0
