package fr.airsen.api.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for automatic cache refresh and maintenance.
 *
 * Implements time-based cache eviction strategy to ensure data freshness:
 * - Air quality cache: refreshed every hour (real-time data)
 * - Weather cache: refreshed every 30 minutes (frequently updated)
 * - Geographic caches: rarely cleared (static administrative data)
 */
@Component
@EnableScheduling
public class CacheRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(CacheRefreshScheduler.class);

    private final CacheManager cacheManager;

    public CacheRefreshScheduler(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * Refreshes air quality cache every hour.
     * Scheduled at the top of every hour (HH:00:00).
     */
    @Scheduled(cron = "0 0 * * * *")
    public void refreshAirQualityCache() {
        Cache cache = cacheManager.getCache("air-quality");
        if (cache != null) {
            cache.clear();
            log.info("✓ Cleared air quality cache (scheduled hourly refresh)");
        } else {
            log.warn("Air quality cache not found");
        }
    }

    /**
     * Refreshes weather cache every 30 minutes.
     * Scheduled at HH:00:00 and HH:30:00.
     */
    @Scheduled(cron = "0 */30 * * * *")
    public void refreshWeatherCache() {
        Cache cache = cacheManager.getCache("weather");
        if (cache != null) {
            cache.clear();
            log.info("✓ Cleared weather cache (scheduled 30-minute refresh)");
        } else {
            log.warn("Weather cache not found");
        }
    }

    /**
     * Logs cache statistics every 15 minutes for monitoring.
     * Helps track cache usage and identify performance issues.
     */
    @Scheduled(cron = "0 */15 * * * *")
    public void logCacheStats() {
        log.debug("=== Cache Statistics ===");
        cacheManager.getCacheNames().forEach(cacheName -> {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                log.debug("Cache '{}' is active and operational", cacheName);
            }
        });
        log.debug("=======================");
    }

    /**
     * Daily cache cleanup at 3:00 AM.
     * Clears all caches to ensure fresh data at the start of each day.
     * Scheduled during low-traffic hours to minimize user impact.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void dailyCacheCleanup() {
        log.info("Starting daily cache cleanup...");
        int clearedCount = 0;

        for (String cacheName : cacheManager.getCacheNames()) {
            // Skip geographic caches (regions, departments, communes) as they contain static data
            if (cacheName.equals("regions") || cacheName.equals("departments") || cacheName.equals("communes") || cacheName.equals("geography")) {
                log.debug("Skipping static cache: {}", cacheName);
                continue;
            }

            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                clearedCount++;
                log.info("✓ Cleared cache: {}", cacheName);
            }
        }

        log.info("✓ Daily cache cleanup complete - {} caches cleared", clearedCount);
    }
}
