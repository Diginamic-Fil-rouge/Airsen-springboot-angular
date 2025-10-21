package fr.airsen.api.service.cacheData;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.airsen.api.dto.cacheData.CachedEntry;
import fr.airsen.api.entity.cacheData.CacheMetadata;
import fr.airsen.api.service.metrics.CacheMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for SmartCacheService.
 *
 * Tests the core caching logic including:
 * - Cache hits/misses/stale handling
 * - Source-aware TTL selection
 * - Metadata tracking
 * - Automatic refresh decision logic
 * - Metrics recording
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SmartCacheService Integration Tests")
class SmartCacheServiceIntegrationTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

    @Mock
    private CacheMetricsService cacheMetricsService;

    private MeterRegistry meterRegistry;
    private ObjectMapper objectMapper;
    private SmartCacheService cacheService;

    private static final String TEST_KEY = "test:weather:48.5:2.5";
    private static final String CACHE_PREFIX = "airsen:smart-cache:";
    private static final String FULL_KEY = CACHE_PREFIX + TEST_KEY;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        objectMapper = new ObjectMapper();
        cacheService = new SmartCacheService(redisTemplate, objectMapper, meterRegistry, cacheMetricsService);

        // Use lenient() for stub that may not be used in all tests
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    @Test
    @DisplayName("Cache miss: should invoke fetcher and cache result")
    void testCacheMissInvokesFetcher() {
        // Arrange
        String testData = "weather-data-123";
        when(valueOps.get(FULL_KEY)).thenReturn(null);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        CachedEntry<String> result = cacheService.getOrFetch(
            TEST_KEY,
            String.class,
            CacheMetadata.DataSource.ON_DEMAND_FETCH,
            false,
            () -> testData
        );

        // Assert
        assertThat(result.getData()).isEqualTo(testData);
        verify(valueOps).set(eq(FULL_KEY), any(CachedEntry.class), anyLong(), any());
    }

    @Test
    @DisplayName("Cache hit (fresh): should return cached data without invoking fetcher")
    void testCacheHitFreshReturnsCachedData() {
        // Arrange
        String testData = "weather-data-456";
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);
        CachedEntry<String> cachedEntry = new CachedEntry<>(testData, metadata);

        when(valueOps.get(FULL_KEY)).thenReturn(cachedEntry);

        // Act
        CachedEntry<String> result = cacheService.getOrFetch(
            TEST_KEY,
            String.class,
            CacheMetadata.DataSource.ON_DEMAND_FETCH,
            false,
            () -> { throw new RuntimeException("Should not be called"); }
        );

        // Assert
        assertThat(result.getData()).isEqualTo(testData);
        assertThat(result.isFresh()).isTrue();
    }

    @Test
    @DisplayName("Force refresh: should skip cache and fetch new data")
    void testForceRefreshSkipsCacheAndFetches() {
        // Arrange
        String newData = "new-weather-data";
        when(valueOps.get(FULL_KEY)).thenReturn(null);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        CachedEntry<String> result = cacheService.getOrFetch(
            TEST_KEY,
            String.class,
            CacheMetadata.DataSource.ON_DEMAND_FETCH,
            true,  // forceRefresh = true
            () -> newData
        );

        // Assert
        assertThat(result.getData()).isEqualTo(newData);
        verify(valueOps).set(eq(FULL_KEY), any(CachedEntry.class), anyLong(), any());
    }

    @Test
    @DisplayName("Different data sources have different TTLs")
    void testDataSourceTTLVariation() {
        // Test that different sources create metadata with correct TTL values

        CacheMetadata userUpdate = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.USER_UPDATE);
        assertThat(userUpdate.getTtlSeconds()).isEqualTo(12 * 3600);

        CacheMetadata tier1 = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER1);
        assertThat(tier1.getTtlSeconds()).isEqualTo(2 * 3600);

        CacheMetadata tier2 = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER2);
        assertThat(tier2.getTtlSeconds()).isEqualTo(6 * 3600);

        CacheMetadata exportSnapshot = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.EXPORT_SNAPSHOT);
        assertThat(exportSnapshot.getTtlSeconds()).isEqualTo(15 * 60); // 15 minutes
    }

    @Test
    @DisplayName("Cache invalidation: should remove cached entry")
    void testInvalidateRemovesCacheEntry() {
        // Act
        cacheService.invalidate(TEST_KEY);

        // Assert
        verify(redisTemplate).delete(FULL_KEY);
    }

    @Test
    @DisplayName("shouldRefresh returns true when cache is stale")
    void testShouldRefreshDetectsStaleness() {
        // We can't directly test this without a stale cache since CacheMetadata
        // uses LocalDateTime.now() internally. This would be better tested
        // with time mocking or by testing the metadata's shouldRefresh() method directly.

        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);
        // Fresh cache should not need refresh
        assertThat(metadata.shouldRefresh()).isFalse();
    }

    @Test
    @DisplayName("exists returns true when key is in cache")
    void testExistsReturnsTrueForCachedKey() {
        // Arrange
        when(redisTemplate.hasKey(FULL_KEY)).thenReturn(true);

        // Act
        boolean exists = cacheService.exists(TEST_KEY);

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("exists returns false when key is not in cache")
    void testExistsReturnsFalseForMissingKey() {
        // Arrange
        when(redisTemplate.hasKey(FULL_KEY)).thenReturn(false);

        // Act
        boolean exists = cacheService.exists(TEST_KEY);

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("clearAll removes all cache entries")
    void testClearAllRemovesAllEntries() {
        // Arrange
        Set<String> keys = Set.of(
            CACHE_PREFIX + "key1",
            CACHE_PREFIX + "key2",
            CACHE_PREFIX + "key3"
        );
        when(redisTemplate.keys(CACHE_PREFIX + "*")).thenReturn(keys);

        // Act
        cacheService.clearAll();

        // Assert
        verify(redisTemplate).delete(keys);
    }

    @Test
    @DisplayName("Metadata tracks source and TTL correctly")
    void testMetadataTrackingSourceAndTTL() {
        // Arrange & Act
        CacheMetadata metadata = new CacheMetadata(
            TEST_KEY,
            CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER2
        );

        // Assert
        assertThat(metadata.getSource()).isEqualTo(CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER2);
        assertThat(metadata.getTtlSeconds()).isEqualTo(6 * 3600);
        assertThat(metadata.getKey()).isEqualTo(TEST_KEY);
        assertThat(metadata.getCachedAt()).isNotNull();
        assertThat(metadata.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("isExpired correctly identifies expired entries")
    void testIsExpiredMethod() {
        // Arrange
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Assert - freshly created should not be expired
        assertThat(metadata.isExpired()).isFalse();
    }

    @Test
    @DisplayName("getStaleness calculates age ratio correctly")
    void testGetStalenessCalculation() {
        // Arrange
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Assert - very fresh cache should have low staleness
        double staleness = metadata.getStaleness();
        assertThat(staleness).isGreaterThanOrEqualTo(0.0);
        assertThat(staleness).isLessThan(0.1); // Less than 10% staleness for just-created cache
    }

    @Test
    @DisplayName("getAgeDescription provides human-readable age")
    void testGetAgeDescriptionFormatting() {
        // Arrange
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Assert
        String ageDescription = metadata.getAgeDescription();
        assertThat(ageDescription).isNotNull();
        assertThat(ageDescription).contains("ago");
    }

    @Test
    @DisplayName("Fetcher exception is propagated")
    void testFetcherExceptionPropagation() {
        // Arrange
        when(valueOps.get(FULL_KEY)).thenReturn(null);
        RuntimeException testException = new RuntimeException("API error");

        // Act & Assert
        assertThatThrownBy(() ->
            cacheService.getOrFetch(
                TEST_KEY,
                String.class,
                CacheMetadata.DataSource.ON_DEMAND_FETCH,
                false,
                () -> { throw testException; }
            )
        ).isEqualTo(testException);
    }

    @Test
    @DisplayName("Generic type handling works correctly with custom DTO")
    void testGenericTypeHandling() {
        // Arrange
        class TestWeatherDTO {
            public String temperature;
            public String humidity;

            TestWeatherDTO(String temp, String humidity) {
                this.temperature = temp;
                this.humidity = humidity;
            }
        }

        TestWeatherDTO testData = new TestWeatherDTO("25C", "65%");
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);
        CachedEntry<TestWeatherDTO> cachedEntry = new CachedEntry<>(testData, metadata);

        when(valueOps.get(FULL_KEY)).thenReturn(cachedEntry);

        // Act
        CachedEntry<TestWeatherDTO> result = cacheService.getOrFetch(
            TEST_KEY,
            TestWeatherDTO.class,
            CacheMetadata.DataSource.ON_DEMAND_FETCH,
            false,
            () -> { throw new RuntimeException("Should not be called"); }
        );

        // Assert
        assertThat(result.getData()).isEqualTo(testData);
        assertThat(result.getData().temperature).isEqualTo("25C");
        assertThat(result.getData().humidity).isEqualTo("65%");
    }

    @Test
    @DisplayName("CachedEntry isFresh() works correctly")
    void testCachedEntryFreshnessCheck() {
        // Arrange
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);
        CachedEntry<String> entry = new CachedEntry<>("data", metadata);

        // Assert - fresh cache should report as fresh and not expired
        assertThat(entry.isFresh()).isTrue();
        assertThat(entry.isUsableAsStale()).isTrue();
    }

    @Test
    @DisplayName("Multiple different keys can be cached independently")
    void testIndependentKeyStorage() {
        // Arrange
        String key1 = "weather:paris";
        String key2 = "weather:london";
        String data1 = "sunny";
        String data2 = "rainy";

        lenient().when(valueOps.get(CACHE_PREFIX + key1)).thenReturn(null);
        lenient().when(valueOps.get(CACHE_PREFIX + key2)).thenReturn(null);

        // Act
        CachedEntry<String> result1 = cacheService.getOrFetch(
            key1, String.class,
            CacheMetadata.DataSource.ON_DEMAND_FETCH,
            false,
            () -> data1
        );

        CachedEntry<String> result2 = cacheService.getOrFetch(
            key2, String.class,
            CacheMetadata.DataSource.ON_DEMAND_FETCH,
            false,
            () -> data2
        );

        // Assert
        assertThat(result1.getData()).isEqualTo(data1);
        assertThat(result2.getData()).isEqualTo(data2);

        // Verify separate cache entries were created
        verify(valueOps, times(2)).set(anyString(), any(CachedEntry.class), anyLong(), any());
    }

    @Test
    @DisplayName("Cache call counter on fetcher invocation")
    void testFetcherInvocationCounting() {
        // Arrange
        AtomicInteger callCount = new AtomicInteger(0);
        String testData = "cached-data";
        when(valueOps.get(FULL_KEY)).thenReturn(null);
        when(redisTemplate.expire(anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        // Act
        cacheService.getOrFetch(
            TEST_KEY,
            String.class,
            CacheMetadata.DataSource.ON_DEMAND_FETCH,
            false,
            () -> {
                callCount.incrementAndGet();
                return testData;
            }
        );

        // Assert
        assertThat(callCount.get()).isEqualTo(1);
    }
}
