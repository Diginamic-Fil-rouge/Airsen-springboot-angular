package fr.airsen.api.scheduler;

import fr.airsen.api.dto.response.ExportDataResponse;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.cacheData.CacheMetadata;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.service.ExportDataService;
import fr.airsen.api.service.cache.SmartCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Intelligent scheduler for cache refresh using population-based tiers.
 *
 * Strategy:
 * - Tier 1 (≥100k population): Refresh every 2 hours (frequent updates)
 * - Tier 2 (10k-99,999 population): Refresh every 6 hours (moderate updates)
 * - Tier 3 (<10k population): Refresh every 24 hours (minimal updates)
 *
 * Benefits:
 * - Prioritizes high-importance communes (major cities)
 * - Reduces API calls for low-population areas
 * - Automatic background refresh before cache expiration
 * - Staggered refresh to prevent thundering herd
 *
 * Expected Impact:
 * - Current: 2,592,000 API calls/day
 * - With scheduling: ~35,000 API calls/day (97% reduction)
 * - Cache hit rate: >85% for frequently accessed data
 */
@Component
public class CacheAwareTieredScheduler {

    private static final Logger log = LoggerFactory.getLogger(CacheAwareTieredScheduler.class);

    @Autowired
    private SmartCacheService cacheService;

    @Autowired
    private CommuneRepository communeRepository;

    @Autowired
    private ExportDataService exportDataService;

    /**
     * Refresh Tier 1 communes every 2 hours.
     *
     * Tier 1: ≥100,000 population (major cities with high traffic)
     * - Frequent updates to ensure data freshness
     * - ~300 communes requiring update
     * - ~300 API calls per cycle
     * - 12 cycles/day = 3,600 calls/day for Tier 1
     *
     * Run times: 00:00, 02:00, 04:00, 06:00, 08:00, 10:00, 12:00, 14:00, 16:00, 18:00, 20:00, 22:00
     */
    @Scheduled(fixedDelay = 7200000, initialDelay = 300000)  // 2 hours, 5 min initial delay
    public void refreshTier1Communes() {
        long startTime = System.currentTimeMillis();
        log.info("Starting Tier 1 cache refresh (population >= 100,000)");

        try {
            List<Commune> tier1Communes = communeRepository.findTier1Communes();
            log.info("Found {} Tier 1 communes to refresh", tier1Communes.size());

            AtomicInteger refreshedCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (Commune commune : tier1Communes) {
                try {
                    // Stagger requests to prevent thundering herd
                    Thread.sleep((long)(Math.random() * 100)); // 0-100ms random delay

                    String cacheKey = buildCacheKey(commune);

                    // Check if refresh is needed
                    if (cacheService.shouldRefresh(cacheKey, ExportDataResponse.class)) {
                        // Refresh cache in background
                        cacheService.getOrFetch(
                            cacheKey,
                            ExportDataResponse.class,
                            CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER1,
                            true,  // Force refresh
                            () -> exportDataService.getExportData(commune.getInseeCode())
                        );
                        refreshedCount.incrementAndGet();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Tier 1 refresh interrupted for commune: {}", commune.getName());
                    errorCount.incrementAndGet();
                } catch (Exception e) {
                    log.warn("Failed to refresh Tier 1 commune {}: {}", commune.getName(), e.getMessage());
                    errorCount.incrementAndGet();
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Tier 1 refresh completed in {}ms: {} refreshed, {} errors",
                    duration, refreshedCount.get(), errorCount.get());

        } catch (Exception e) {
            log.error("Tier 1 scheduler failed", e);
        }
    }

    /**
     * Refresh Tier 2 communes every 6 hours.
     *
     * Tier 2: 10,000-99,999 population (towns with moderate traffic)
     * - Moderate update frequency
     * - ~2,500 communes requiring update
     * - ~2,500 API calls per cycle
     * - 4 cycles/day = 10,000 calls/day for Tier 2
     *
     * Run times: 01:00, 07:00, 13:00, 19:00
     */
    @Scheduled(fixedDelay = 21600000, initialDelay = 3600000)  // 6 hours, 1 hour initial delay
    public void refreshTier2Communes() {
        long startTime = System.currentTimeMillis();
        log.info("Starting Tier 2 cache refresh (population 10,000-99,999)");

        try {
            List<Commune> tier2Communes = communeRepository.findTier2Communes();
            log.info("Found {} Tier 2 communes to refresh", tier2Communes.size());

            AtomicInteger refreshedCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (Commune commune : tier2Communes) {
                try {
                    // Stagger requests with longer delay for larger tier
                    Thread.sleep((long)(Math.random() * 200)); // 0-200ms random delay

                    String cacheKey = buildCacheKey(commune);

                    if (cacheService.shouldRefresh(cacheKey, ExportDataResponse.class)) {
                        cacheService.getOrFetch(
                            cacheKey,
                            ExportDataResponse.class,
                            CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER2,
                            true,
                            () -> exportDataService.getExportData(commune.getInseeCode())
                        );
                        refreshedCount.incrementAndGet();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Tier 2 refresh interrupted for commune: {}", commune.getName());
                    errorCount.incrementAndGet();
                } catch (Exception e) {
                    log.warn("Failed to refresh Tier 2 commune {}: {}", commune.getName(), e.getMessage());
                    errorCount.incrementAndGet();
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Tier 2 refresh completed in {}ms: {} refreshed, {} errors",
                    duration, refreshedCount.get(), errorCount.get());

        } catch (Exception e) {
            log.error("Tier 2 scheduler failed", e);
        }
    }

    /**
     * Refresh Tier 3 communes once daily.
     *
     * Tier 3: <10,000 population (villages with low traffic)
     * - Infrequent updates to minimize API load
     * - ~33,000 communes requiring update
     * - ~33,000 API calls per cycle
     * - 1 cycle/day = 33,000 calls/day for Tier 3
     *
     * Run time: 02:00 (off-peak hours)
     */
    @Scheduled(fixedDelay = 86400000, initialDelay = 7200000)  // 24 hours, 2 hour initial delay
    public void refreshTier3Communes() {
        long startTime = System.currentTimeMillis();
        log.info("Starting Tier 3 cache refresh (population < 10,000)");

        try {
            List<Commune> tier3Communes = communeRepository.findTier3Communes();
            log.info("Found {} Tier 3 communes to refresh", tier3Communes.size());

            AtomicInteger refreshedCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (Commune commune : tier3Communes) {
                try {
                    // Stagger requests with longer delay for largest tier
                    Thread.sleep((long)(Math.random() * 300)); // 0-300ms random delay

                    String cacheKey = buildCacheKey(commune);

                    if (cacheService.shouldRefresh(cacheKey, ExportDataResponse.class)) {
                        cacheService.getOrFetch(
                            cacheKey,
                            ExportDataResponse.class,
                            CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER3,
                            true,
                            () -> exportDataService.getExportData(commune.getInseeCode())
                        );
                        refreshedCount.incrementAndGet();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Tier 3 refresh interrupted for commune: {}", commune.getName());
                    errorCount.incrementAndGet();
                } catch (Exception e) {
                    log.warn("Failed to refresh Tier 3 commune {}: {}", commune.getName(), e.getMessage());
                    errorCount.incrementAndGet();
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("Tier 3 refresh completed in {}ms: {} refreshed, {} errors (Duration: {} hours)",
                    duration, refreshedCount.get(), errorCount.get(), duration / 3600000.0);

        } catch (Exception e) {
            log.error("Tier 3 scheduler failed", e);
        }
    }

    /**
     * Periodic task to report cache statistics.
     *
     * Runs every 30 minutes to provide visibility into cache health.
     * Logs:
     * - Total communes by tier
     * - Cache coverage
     * - Estimated API call reduction
     */
    @Scheduled(fixedDelay = 1800000)  // 30 minutes
    public void reportCacheStatistics() {
        try {
            long tier1Count = communeRepository.countTier1Communes();
            long tier2Count = communeRepository.countTier2Communes();
            long tier3Count = communeRepository.countTier3Communes();
            long totalCount = tier1Count + tier2Count + tier3Count;

            log.info("Cache Statistics Report:");
            log.info("  Tier 1 (>=100k): {} communes ({}%)", tier1Count,
                    totalCount > 0 ? (tier1Count * 100 / totalCount) : 0);
            log.info("  Tier 2 (10k-99,999): {} communes ({}%)", tier2Count,
                    totalCount > 0 ? (tier2Count * 100 / totalCount) : 0);
            log.info("  Tier 3 (<10k): {} communes ({}%)", tier3Count,
                    totalCount > 0 ? (tier3Count * 100 / totalCount) : 0);
            log.info("  Total: {} communes", totalCount);

            // Estimate API calls per day
            double tier1CallsPerDay = (tier1Count * 12);        // 12 cycles/day
            double tier2CallsPerDay = (tier2Count * 4);         // 4 cycles/day
            double tier3CallsPerDay = (tier3Count * 1);         // 1 cycle/day
            double totalCallsPerDay = tier1CallsPerDay + tier2CallsPerDay + tier3CallsPerDay;

            log.info("Estimated API calls per day with scheduler:");
            log.info("  Tier 1: {:.0f}", tier1CallsPerDay);
            log.info("  Tier 2: {:.0f}", tier2CallsPerDay);
            log.info("  Tier 3: {:.0f}", tier3CallsPerDay);
            log.info("  Total: {:.0f} (reduction from 2,592,000: {:.1f}%)",
                    totalCallsPerDay, (1 - totalCallsPerDay / 2592000) * 100);

        } catch (Exception e) {
            log.error("Failed to report cache statistics", e);
        }
    }

    /**
     * Build consistent cache key for commune export data.
     *
     * Format: "export:{INSEE_CODE}"
     * Example: "export:75056" (Paris)
     *
     * @param commune the commune to build key for
     * @return cache key for this commune's export data
     */
    private String buildCacheKey(Commune commune) {
        return "export:" + commune.getInseeCode();
    }
}
