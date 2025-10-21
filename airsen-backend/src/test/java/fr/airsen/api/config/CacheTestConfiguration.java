package fr.airsen.api.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Test configuration for Redis cache testing.
 *
 * This configuration mirrors the production {@link RedisConfig} but is optimized
 * for testing with shorter TTL values to speed up test execution.
 *
 * Usage in tests:
 * {@code
 * @SpringBootTest
 * @Import(CacheTestConfiguration.class)
 * class MyServiceTest {
 *     // Test code
 * }
 * }
 *
 * @see RedisConfig
 */
@TestConfiguration
@EnableCaching
public class CacheTestConfiguration {

    /**
     * Configures cache manager for testing with reduced TTL values.
     *
     * TTL values are reduced for faster test execution:
     * - air-quality: 5 minutes (vs 1 hour in production)
     * - weather: 2 minutes (vs 30 minutes in production)
     * - population: 10 minutes (vs 24 hours in production)
     * - geography/communes/departments/regions: 15 minutes (vs 7 days in production)
     *
     * @param connectionFactory Redis connection factory from TestContainers
     * @return configured CacheManager for testing
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Base configuration with JSON serialization
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
            )
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
            )
            .disableCachingNullValues()
            .prefixCacheNameWith("airsen:");

        // Cache-specific configurations with reduced TTL for testing
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Air quality data - real-time updates (5 minutes for testing)
        cacheConfigurations.put("air-quality", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Weather data - frequent updates (2 minutes for testing)
        cacheConfigurations.put("weather", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        // Population data - relatively static (10 minutes for testing)
        cacheConfigurations.put("population", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Geographic data - rarely changes (15 minutes for testing)
        cacheConfigurations.put("geography", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("communes", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("departments", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put("regions", defaultConfig.entryTtl(Duration.ofMinutes(15)));

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigurations)
            .build();
    }
}
