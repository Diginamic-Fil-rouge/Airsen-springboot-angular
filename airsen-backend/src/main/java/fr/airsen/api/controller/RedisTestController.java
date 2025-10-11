package fr.airsen.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Test controller for Redis cache operations and diagnostics.
 *
 * Provides endpoints for:
 * - Testing Redis connectivity
 * - Viewing cache statistics
 * - Clearing caches (admin only)
 * - Debugging cache keys
 */
@RestController
@RequestMapping("/test/redis")
public class RedisTestController {

    private static final Logger log = LoggerFactory.getLogger(RedisTestController.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;

    public RedisTestController(RedisTemplate<String, Object> redisTemplate, CacheManager cacheManager) {
        this.redisTemplate = redisTemplate;
        this.cacheManager = cacheManager;
    }

    /**
     * Tests Redis connection and returns server status.
     *
     * @return connection status with ping response
     */
    @GetMapping("/connection")
    public ResponseEntity<Map<String, String>> testConnection() {
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();

            log.info("Redis connection test successful");

            return ResponseEntity.ok(Map.of(
                "status", "connected",
                "message", "Redis is running and accessible",
                "response", pong != null ? pong : "PONG"
            ));
        } catch (Exception e) {
            log.error("Redis connection test failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Redis connection failed: " + e.getMessage()
            ));
        }
    }

    /**
     * Gets cache statistics for all configured caches.
     *
     * @return map of cache names to their statistics
     */
    @GetMapping("/cache-stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof RedisCache redisCache) {
                stats.put(cacheName, Map.of(
                    "name", cacheName,
                    "type", "Redis",
                    "nativeCache", redisCache.getNativeCache().getClass().getSimpleName()
                ));
            } else if (cache != null) {
                stats.put(cacheName, Map.of(
                    "name", cacheName,
                    "type", cache.getClass().getSimpleName()
                ));
            }
        }

        log.info("Retrieved cache statistics for {} caches", stats.size());

        return ResponseEntity.ok(Map.of(
            "timestamp", System.currentTimeMillis(),
            "cacheCount", stats.size(),
            "caches", stats
        ));
    }

    /**
     * Gets detailed cache metrics including key counts.
     *
     * @return detailed metrics for each cache
     */
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getCacheMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                Set<String> keys = redisTemplate.keys("airsen:" + cacheName + ":*");

                metrics.put(cacheName, Map.of(
                    "name", cacheName,
                    "keysCount", keys != null ? keys.size() : 0,
                    "sampleKeys", keys != null ? keys.stream().limit(5).toList() : java.util.List.of()
                ));
            }
        }

        return ResponseEntity.ok(Map.of(
            "timestamp", System.currentTimeMillis(),
            "caches", metrics
        ));
    }

    /**
     * Gets all Redis keys matching the Airsen pattern (for debugging).
     * Requires ADMIN role.
     *
     * @return set of all cache keys
     */
    @GetMapping("/keys")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllKeys() {
        try {
            Set<String> keys = redisTemplate.keys("airsen:*");

            log.info("Retrieved {} Redis keys", keys != null ? keys.size() : 0);

            return ResponseEntity.ok(Map.of(
                "keyCount", keys != null ? keys.size() : 0,
                "keys", keys != null ? keys : java.util.Set.of()
            ));
        } catch (Exception e) {
            log.error("Failed to retrieve Redis keys", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to retrieve keys: " + e.getMessage()
            ));
        }
    }

    /**
     * Clears all caches. Requires ADMIN role.
     *
     * @return confirmation message
     */
    @DeleteMapping("/clear-cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> clearAllCaches() {
        int clearedCount = 0;

        for (String cacheName : cacheManager.getCacheNames()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                clearedCount++;
                log.info("Cleared cache: {}", cacheName);
            }
        }

        log.info("Cleared all {} caches", clearedCount);

        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "All caches cleared successfully",
            "clearedCount", String.valueOf(clearedCount)
        ));
    }

    /**
     * Clears a specific cache by name. Requires ADMIN role.
     *
     * @param cacheName name of the cache to clear
     * @return confirmation message
     */
    @DeleteMapping("/clear-cache/{cacheName}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> clearCache(@PathVariable String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);

        if (cache != null) {
            cache.clear();
            log.info("Cleared cache: {}", cacheName);

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Cache '" + cacheName + "' cleared successfully"
            ));
        } else {
            log.warn("Cache '{}' not found", cacheName);

            return ResponseEntity.status(404).body(Map.of(
                "status", "error",
                "message", "Cache '" + cacheName + "' not found"
            ));
        }
    }

    /**
     * Gets information about the Redis server.
     *
     * @return Redis server information
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getRedisInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            info.put("connected", true);
            info.put("cacheManagerType", cacheManager.getClass().getSimpleName());
            info.put("cacheNames", cacheManager.getCacheNames());

            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.error("Failed to get Redis info", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to get Redis info: " + e.getMessage()
            ));
        }
    }
}
