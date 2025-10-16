package fr.airsen.api.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Health indicator for Redis connectivity and status.
 *
 * Provides health check information for Spring Boot Actuator,
 * allowing monitoring systems to track Redis availability.
 *
 * Health Check Features:
 * - Tests Redis connectivity with PING command
 * - Returns UP status when Redis is accessible
 * - Returns DOWN status with error details when Redis fails
 * - Includes Redis host information for diagnostics
 *
 * Integration:
 * - Accessible via /actuator/health/redisHealth endpoint
 * - Included in overall application health status
 * - Used by monitoring tools and container orchestration
 */
@Component("redisHealth")
public class RedisHealthIndicator implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(RedisHealthIndicator.class);

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Checks Redis health by sending PING command.
     *
     * @return Health status with details
     */
    @Override
    public Health health() {
        try {
            // Test Redis connectivity with PING command
            String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();

            log.trace("Redis health check successful - PING returned: {}", pong);

            return Health.up()
                .withDetail("redis", "Available")
                .withDetail("response", pong != null ? pong : "PONG")
                .withDetail("status", "Connected")
                .build();

        } catch (Exception e) {
            log.error("Redis health check failed - Connection unavailable: {}", e.getMessage());

            return Health.down()
                .withDetail("redis", "Unavailable")
                .withDetail("error", e.getMessage())
                .withDetail("errorType", e.getClass().getSimpleName())
                .withDetail("status", "Disconnected")
                .build();
        }
    }
}
