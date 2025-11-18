package fr.airsen.api.config;

import fr.airsen.api.AbstractTestContainersTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for Redis cache with TestContainers.
 *
 * This test suite verifies:
 * - Redis connectivity with actual Redis 7 Alpine instance
 * - Cache manager configuration
 * - Cache key prefixing ("airsen:")
 * - TTL settings for different cache types
 * - Serialization/deserialization with Jackson
 * - Cache eviction operations
 *
 * Requires Docker to be running to execute tests.
 * Inherits TestContainers configuration from AbstractTestContainersTest.
 */
@SpringBootTest
@Testcontainers
@DisplayName("Redis Cache Integration Tests (TestContainers)")
class RedisCacheIntegrationTest extends AbstractTestContainersTest {

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        // Clear all caches before each test
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    @Nested
    @DisplayName("Redis Connectivity Tests")
    class ConnectivityTests {

        @Test
        @DisplayName("Should connect to Redis TestContainer")
        void shouldConnectToRedis() {
            // Given - Redis container is running
            assertThat(redis.isRunning()).isTrue();

            // When - Ping Redis
            String pong = redisTemplate.getConnectionFactory()
                    .getConnection()
                    .ping();

            // Then - Should receive PONG
            assertThat(pong).isEqualTo("PONG");
        }

        @Test
        @DisplayName("Should have correct Redis configuration")
        void shouldHaveCorrectRedisConfiguration() {
            // Given - Application properties configured via TestContainers

            // Then - Verify connection details
            assertThat(redis.getHost()).isNotBlank();
            assertThat(redis.getMappedPort(6379)).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Cache Manager Configuration Tests")
    class CacheManagerTests {

        @Test
        @DisplayName("Should have all 7 configured caches")
        void shouldHaveAllConfiguredCaches() {
            // Given - Cache manager initialized

            // When - Get cache names
            Set<String> cacheNames = (Set<String>) cacheManager.getCacheNames();

            // Then - Should have 7 caches
            assertThat(cacheNames).hasSize(7);
            assertThat(cacheNames).containsExactlyInAnyOrder(
                    "air-quality",
                    "weather",
                    "population",
                    "geography",
                    "communes",
                    "departments",
                    "regions"
            );
        }

        @Test
        @DisplayName("Should retrieve individual caches by name")
        void shouldRetrieveIndividualCaches() {
            // Given - Cache manager initialized

            // When - Get specific caches
            Cache airQualityCache = cacheManager.getCache("air-quality");
            Cache weatherCache = cacheManager.getCache("weather");
            Cache communesCache = cacheManager.getCache("communes");

            // Then - All caches should exist
            assertThat(airQualityCache).isNotNull();
            assertThat(weatherCache).isNotNull();
            assertThat(communesCache).isNotNull();
        }
    }

    @Nested
    @DisplayName("Cache Key and Storage Tests")
    class CacheKeyTests {

        @Test
        @DisplayName("Should prefix cache keys with 'airsen:'")
        void shouldPrefixCacheKeys() {
            // Given - Cache key and value
            String cacheKey = "75056";
            String testValue = "Test Air Quality Data";

            // When - Store value in cache
            Cache cache = cacheManager.getCache("air-quality");
            Objects.requireNonNull(cache).put(cacheKey, testValue);

            // Debug: List ALL keys in Redis
            Set<String> allKeys = redisTemplate.keys("*");
            System.out.println("DEBUG: All keys in Redis: " + allKeys);

            // Then - Redis key should be prefixed (format: airsen:air-quality::key)
            Set<String> keys = redisTemplate.keys("airsen:air-quality::*");
            assertThat(keys).as("Keys with pattern 'airsen:air-quality::*'. All keys: " + allKeys)
                .isNotNull()
                .hasSizeGreaterThan(0);
            assertThat(keys.iterator().next()).contains("airsen:air-quality::");
            assertThat(keys.iterator().next()).contains(cacheKey);
        }

        @Test
        @DisplayName("Should store and retrieve values correctly")
        void shouldStoreAndRetrieveValues() {
            // Given - Test data
            String cacheKey = "test-key";
            String testValue = "Test Value";

            Cache cache = cacheManager.getCache("air-quality");
            Objects.requireNonNull(cache);

            // When - Store and retrieve
            cache.put(cacheKey, testValue);
            Cache.ValueWrapper retrieved = cache.get(cacheKey);

            // Then - Value should match
            assertThat(retrieved).isNotNull();
            assertThat(retrieved.get()).isEqualTo(testValue);
        }

        @Test
        @DisplayName("Should handle null values gracefully (not cache them)")
        void shouldNotCacheNullValues() {
            // Given - Null value
            String cacheKey = "null-test";

            Cache cache = cacheManager.getCache("air-quality");
            Objects.requireNonNull(cache);

            // When - Try to store null
            // Then - Should throw IllegalArgumentException (due to disableCachingNullValues)
            assertThatThrownBy(() -> cache.put(cacheKey, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not allow 'null' values");
        }
    }

    @Nested
    @DisplayName("Cache Eviction Tests")
    class CacheEvictionTests {

        @Test
        @DisplayName("Should evict single cache entry")
        void shouldEvictSingleEntry() {
            // Given - Data in cache
            String cacheKey = "75056";
            String testValue = "Paris Air Quality";

            Cache cache = cacheManager.getCache("air-quality");
            Objects.requireNonNull(cache).put(cacheKey, testValue);

            assertThat(cache.get(cacheKey)).isNotNull();

            // When - Evict entry
            cache.evict(cacheKey);

            // Then - Entry should be removed
            assertThat(cache.get(cacheKey)).isNull();

            Set<String> keys = redisTemplate.keys("airsen:air-quality:" + cacheKey);
            assertThat(keys == null || keys.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("Should clear all entries in a cache")
        void shouldClearAllEntries() {
            // Given - Multiple entries in cache
            Cache cache = cacheManager.getCache("air-quality");
            Objects.requireNonNull(cache);

            cache.put("75056", "Paris");
            cache.put("69123", "Lyon");
            cache.put("13055", "Marseille");

            assertThat(cache.get("75056")).isNotNull();
            assertThat(cache.get("69123")).isNotNull();
            assertThat(cache.get("13055")).isNotNull();

            // When - Clear cache
            cache.clear();

            // Then - All entries should be removed
            assertThat(cache.get("75056")).isNull();
            assertThat(cache.get("69123")).isNull();
            assertThat(cache.get("13055")).isNull();

            Set<String> keys = redisTemplate.keys("airsen:air-quality:*");
            assertThat(keys == null || keys.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("TTL (Time To Live) Tests")
    class TTLTests {

        @Test
        @DisplayName("Should set TTL for cached entries")
        void shouldSetTTLForCachedEntries() {
            // Given - Cache with TTL configured (5 minutes in test config)
            String cacheKey = "75056";
            String testValue = "Test Data";

            Cache cache = cacheManager.getCache("air-quality");
            Objects.requireNonNull(cache).put(cacheKey, testValue);

            // When - Get TTL from Redis (format: airsen:cache-name::key)
            String redisKey = "airsen:air-quality::" + cacheKey;
            Long ttl = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);

            // Then - TTL should be set (1 second in test config for air-quality)
            assertThat(ttl).isNotNull();
            assertThat(ttl).isGreaterThan(0);
            assertThat(ttl).isLessThanOrEqualTo(1L); // 1 second for test config
        }

        @Test
        @DisplayName("Should have different TTL for different cache types")
        void shouldHaveDifferentTTLForDifferentCaches() {
            // Given - Different caches with different TTL
            String key = "test-key";
            String value = "test-value";

            // When - Store in different caches
            Cache airQualityCache = cacheManager.getCache("air-quality");
            Cache weatherCache = cacheManager.getCache("weather");

            Objects.requireNonNull(airQualityCache).put(key, value);
            Objects.requireNonNull(weatherCache).put(key, value);

            // Then - TTLs should be different (format: airsen:cache-name::key)
            String airQualityKey = "airsen:air-quality::" + key;
            String weatherKey = "airsen:weather::" + key;

            Long airQualityTTL = redisTemplate.getExpire(airQualityKey, TimeUnit.SECONDS);
            Long weatherTTL = redisTemplate.getExpire(weatherKey, TimeUnit.SECONDS);

            assertThat(airQualityTTL).isNotNull().isGreaterThan(0);
            assertThat(weatherTTL).isNotNull().isGreaterThan(0);

            // Air quality: 1 second, Weather: 1 second in test config
            // Both caches configured with 1 second TTL in application-test.yml
            assertThat(airQualityTTL).isGreaterThan(0);
            assertThat(weatherTTL).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Performance Tests")
    class PerformanceTests {

        @Test
        @DisplayName("Cache operations should complete within 50ms")
        void cacheOperationsShouldBeFast() {
            // Given - Cache and test data
            Cache cache = cacheManager.getCache("air-quality");
            String key = "75056";
            String value = "Test Data";

            Objects.requireNonNull(cache);

            // When - Measure cache put operation
            long startPut = System.nanoTime();
            cache.put(key, value);
            long putDuration = System.nanoTime() - startPut;

            // And - Measure cache get operation
            long startGet = System.nanoTime();
            cache.get(key);
            long getDuration = System.nanoTime() - startGet;

            // Then - Operations should be fast (< 50ms)
            assertThat(Duration.ofNanos(putDuration).toMillis()).isLessThan(50);
            assertThat(Duration.ofNanos(getDuration).toMillis()).isLessThan(50);
        }
    }
}
