package fr.airsen.api.service.cacheData;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.airsen.api.dto.cacheData.CachedEntry;
import fr.airsen.api.entity.cacheData.CacheMetadata;
import fr.airsen.api.entity.cacheData.CacheMetadata.DataSource;
import fr.airsen.api.service.metrics.CacheMetricsService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Smart cache service with source-aware TTL and intelligent staleness detection.
 *
 * Core Features:
 * 1. Source-aware TTL: Different data sources have different TTL values
 * 2. Intelligent staleness detection: Knows when to refresh before expiration
 * 3. Stale fallback: Uses stale data if API is down
 * 4. Metrics recording: Tracks cache operations for monitoring
 * 5. Thread-safe operations: Safe for concurrent access
 *
 * Usage Example:
 * <pre>
 * CachedEntry<ExportDataResponse> cached = smartCacheService.getOrFetch(
 *     "export-data:75056",
 *     ExportDataResponse.class,
 *     DataSource.ON_DEMAND_FETCH,
 *     false,  // forceRefresh
 *     () -> exportService.getCompleteExportData("75056")
 * );
 *
 * ExportDataResponse data = cached.getData();
 * String source = cached.isFresh() ? "cache" : "api";
 * </pre>
 */
@Service
public class SmartCacheService {

    private static final Logger log = LoggerFactory.getLogger(SmartCacheService.class);
    private static final String CACHE_PREFIX = "airsen:smart-cache:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final CacheMetricsService cacheMetricsService;

    public SmartCacheService(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry,
            CacheMetricsService cacheMetricsService) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
        this.cacheMetricsService = cacheMetricsService;
    }

    /**
     * Main caching method: Get cached data or fetch from supplier if miss/stale.
     *
     * Decision Tree:
     * 1. If forceRefresh=true: Skip cache, fetch fresh data
     * 2. If cache HIT and fresh: Return cached data
     * 3. If cache HIT and stale: Try background refresh, return stale as fallback
     * 4. If cache MISS: Fetch from supplier
     * 5. If API error: Fall back to stale cache if available
     *
     * @param <T> Type of cached data
     * @param key Cache key (without prefix)
     * @param dataClass Class of cached data type
     * @param source Data source (determines TTL)
     * @param forceRefresh Skip cache and fetch fresh data
     * @param fetchFunction Function that fetches data from external API
     * @return CachedEntry with data and metadata
     * @throws Exception if fetch fails and no stale cache available
     */
    public <T> CachedEntry<T> getOrFetch(
            String key,
            Class<T> dataClass,
            DataSource source,
            boolean forceRefresh,
            Supplier<T> fetchFunction) {

        String fullKey = CACHE_PREFIX + key;

        // Force refresh requested - skip cache completely
        if (forceRefresh) {
            log.debug("Force refresh requested for key: {}", key);
            meterRegistry.counter("cache.operations",
                    "type", "force-refresh",
                    "key", key).increment();

            T freshData = fetchFunction.get();
            CacheMetadata metadata = new CacheMetadata(key, source);
            cache(fullKey, freshData, metadata);

            return new CachedEntry<>(freshData, metadata);
        }

        // Try to get from cache
        Optional<CachedEntry<T>> cachedOpt = get(fullKey, dataClass);

        if (cachedOpt.isPresent()) {
            CachedEntry<T> cached = cachedOpt.get();

            // Cache hit - fresh data
            if (cached.isFresh()) {
                log.debug("Cache HIT (fresh) for key: {} - age: {}",
                        key, cached.getMetadata().getAgeDescription());

                meterRegistry.counter("cache.operations",
                        "type", "hit",
                        "freshness", "fresh",
                        "key", key).increment();

                cacheMetricsService.recordCacheHit(0); // 0ms for Redis hit
                cacheMetricsService.recordCachedApiCall();

                return cached;
            }

            // Cache hit - stale but usable
            log.debug("Cache HIT (stale) for key: {} - staleness: {:.1f}%",
                    key, cached.getMetadata().getStaleness() * 100);

            meterRegistry.counter("cache.operations",
                    "type", "hit",
                    "freshness", "stale",
                    "key", key).increment();

            cacheMetricsService.recordStalenessDetection();

            // Try to refresh in background, return stale data
            try {
                T freshData = fetchFunction.get();
                CacheMetadata metadata = new CacheMetadata(key, source);
                cache(fullKey, freshData, metadata);

                log.debug("Background refresh succeeded for key: {}", key);
                meterRegistry.counter("cache.operations",
                        "type", "background-refresh",
                        "status", "success",
                        "key", key).increment();

                return new CachedEntry<>(freshData, metadata);

            } catch (Exception e) {
                log.warn("Background refresh failed for key: {}, returning stale data", key, e);
                meterRegistry.counter("cache.operations",
                        "type", "stale-fallback",
                        "reason", "fetch-failed",
                        "key", key).increment();

                return cached; // Return stale data on error
            }
        }

        // Cache miss - fetch fresh data
        log.debug("Cache MISS for key: {}", key);
        meterRegistry.counter("cache.operations",
                "type", "miss",
                "key", key).increment();

        try {
            long startTime = System.currentTimeMillis();
            T freshData = fetchFunction.get();
            long elapsedMs = System.currentTimeMillis() - startTime;

            CacheMetadata metadata = new CacheMetadata(key, source);
            cache(fullKey, freshData, metadata);

            meterRegistry.counter("cache.operations",
                    "type", "miss-fetch",
                    "status", "success",
                    "key", key).increment();

            cacheMetricsService.recordCacheMiss(elapsedMs);
            cacheMetricsService.recordExternalApiCall();

            return new CachedEntry<>(freshData, metadata);

        } catch (Exception e) {
            log.error("Fetch failed for key: {} - No cache available", key, e);
            meterRegistry.counter("cache.operations",
                    "type", "miss-fetch",
                    "status", "failed",
                    "key", key).increment();

            throw e;
        }
    }

    /**
     * Get cached entry with metadata from Redis.
     *
     * Handles JSON deserialization and error cases gracefully.
     * Returns empty Optional if key doesn't exist or deserialization fails.
     *
     * @param <T> Type of cached data
     * @param fullKey Full cache key (with prefix)
     * @param dataClass Class of cached data type
     * @return Optional containing CachedEntry if found, empty otherwise
     */
    private <T> Optional<CachedEntry<T>> get(String fullKey, Class<T> dataClass) {
        try {
            Object cached = redisTemplate.opsForValue().get(fullKey);
            if (cached == null) {
                return Optional.empty();
            }

            // Deserialize from Redis value
            String json = objectMapper.writeValueAsString(cached);
            CachedEntry<T> entry = objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructParametricType(
                            CachedEntry.class, dataClass));

            return Optional.of(entry);

        } catch (Exception e) {
            log.error("Failed to deserialize cached entry for key: {}", fullKey, e);
            return Optional.empty();
        }
    }

    /**
     * Cache data with metadata and TTL.
     *
     * Stores CachedEntry (data + metadata) in Redis with source-specific TTL.
     * TTL is determined by the data source in metadata.
     *
     * @param <T> Type of cached data
     * @param fullKey Full cache key (with prefix)
     * @param data The data to cache
     * @param metadata Cache metadata (source, TTL info)
     */
    private <T> void cache(String fullKey, T data, CacheMetadata metadata) {
        try {
            CachedEntry<T> entry = new CachedEntry<>(data, metadata);

            redisTemplate.opsForValue().set(
                    fullKey,
                    entry,
                    metadata.getTtlSeconds(),
                    TimeUnit.SECONDS
            );

            log.debug("Cached data for key: {} with TTL: {}s (source: {})",
                    metadata.getKey(),
                    metadata.getTtlSeconds(),
                    metadata.getSource());

            meterRegistry.counter("cache.operations",
                    "type", "set",
                    "source", metadata.getSource().name(),
                    "key", metadata.getKey()).increment();

        } catch (Exception e) {
            log.error("Failed to cache data for key: {}", fullKey, e);
            meterRegistry.counter("cache.operations",
                    "type", "set",
                    "status", "failed",
                    "key", fullKey).increment();
        }
    }

    /**
     * Invalidate cached entry immediately.
     *
     * Used when data changes and old cache should be discarded.
     * Called by admin endpoints or when data is manually updated.
     *
     * @param key Cache key (without prefix)
     */
    public void invalidate(String key) {
        String fullKey = CACHE_PREFIX + key;
        redisTemplate.delete(fullKey);

        log.debug("Invalidated cache for key: {}", key);
        meterRegistry.counter("cache.operations",
                "type", "invalidate",
                "key", key).increment();
    }

    /**
     * Check if cache should be refreshed.
     *
     * Returns true if:
     * - Cache key doesn't exist (MISS)
     * - Cache entry is expired
     * - Cache entry is stale (shouldRefresh = true)
     *
     * Used by schedulers to decide whether to fetch fresh data.
     *
     * @param <T> Type of cached data
     * @param key Cache key (without prefix)
     * @param dataClass Class of cached data type
     * @return true if key should be refreshed
     */
    public <T> boolean shouldRefresh(String key, Class<T> dataClass) {
        String fullKey = CACHE_PREFIX + key;
        Optional<CachedEntry<T>> cachedOpt = get(fullKey, dataClass);

        if (cachedOpt.isEmpty()) {
            return true; // Missing = needs refresh
        }

        CachedEntry<T> cached = cachedOpt.get();
        return !cached.isFresh(); // Needs refresh if not fresh
    }

    /**
     * Check if cache entry exists and is still valid.
     *
     * Used for monitoring and cache statistics.
     *
     * @param key Cache key (without prefix)
     * @return true if cache key exists and is not expired
     */
    public boolean exists(String key) {
        String fullKey = CACHE_PREFIX + key;
        return Boolean.TRUE.equals(redisTemplate.hasKey(fullKey));
    }

    /**
     * Get cache statistics for monitoring.
     *
     * Returns metadata about cached entry without accessing the data.
     * Useful for cache hit rate analysis and staleness tracking.
     *
     * @param key Cache key (without prefix)
     * @return Optional containing cache metadata if entry exists
     */
    public Optional<CacheMetadata> getMetadata(String key) {
        String fullKey = CACHE_PREFIX + key;
        try {
            @SuppressWarnings("unchecked")
            Optional<CachedEntry<Object>> entryOpt = (Optional<CachedEntry<Object>>) (Object) get(fullKey, Object.class);
            return entryOpt.map(CachedEntry::getMetadata);
        } catch (Exception e) {
            log.debug("Failed to get metadata for key: {}", key, e);
            return Optional.empty();
        }
    }

    /**
     * Clear all cache entries (use with caution).
     *
     * Useful for testing and maintenance operations.
     * In production, prefer invalidate() for specific keys.
     */
    public void clearAll() {
        try {
            // Get all keys matching pattern
            java.util.Set<String> keys = redisTemplate.keys(CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared {} cache entries", keys.size());
                meterRegistry.counter("cache.operations",
                        "type", "clear-all",
                        "status", "success").increment();
            }
        } catch (Exception e) {
            log.error("Failed to clear cache", e);
        }
    }
}
