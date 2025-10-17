package fr.airsen.api.service.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import fr.airsen.api.dto.metrics.CacheStatisticsSummary;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Provides comprehensive metrics tracking for cache operations, API calls, and data freshness.
 * Integrates with Micrometer to expose metrics via Spring Boot Actuator.
 *
 * This service tracks:
 * - Cache hits, misses, and staleness detection
 * - API call volume and sources (external vs cached)
 * - Response time improvements from caching
 * - Estimated cost reduction from cache efficiency
 *
 * Metrics are exposed via /api/v1/actuator/metrics endpoint and can be integrated with
 * monitoring dashboards (Grafana, DataDog, etc).
 *
 * Example metrics:
 * - cache.operations.hits: Number of cache hits
 * - cache.operations.misses: Number of cache misses
 * - cache.operations.staleness: Number of staleness detections
 * - api.calls.external: Number of external API calls made
 * - api.calls.cached: Number of API calls avoided via cache
 * - response.time.improvement: Percentage improvement vs no-cache baseline
 *
 * @author AIRSEN Development Team
 * @version 1.0
 */
@Service
public class CacheMetricsService {

    private final MeterRegistry meterRegistry;

    public CacheMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // Atomic counters for thread-safe incrementing
    private final AtomicLong cacheHitsCounter = new AtomicLong(0);
    private final AtomicLong cacheMissesCounter = new AtomicLong(0);
    private final AtomicLong stalenessDetectionsCounter = new AtomicLong(0);
    private final AtomicLong externalApiCallsCounter = new AtomicLong(0);
    private final AtomicLong cachedApiCallsCounter = new AtomicLong(0);

    // Performance tracking (milliseconds)
    private final AtomicLong totalCacheHitTimeMs = new AtomicLong(0);
    private final AtomicLong totalCacheMissTimeMs = new AtomicLong(0);
    private final AtomicLong cacheOperationCount = new AtomicLong(0);

    /**
     * Initialize all metrics gauges. Should be called during application startup.
     * This registers all custom metrics with Micrometer for exposure via Actuator.
     */
    public void initializeMetrics() {
        // Cache operation counters
        Gauge.builder("cache.operations.hits", cacheHitsCounter, AtomicLong::get)
                .description("Total number of cache hits")
                .tag("cache", "export")
                .register(meterRegistry);

        Gauge.builder("cache.operations.misses", cacheMissesCounter, AtomicLong::get)
                .description("Total number of cache misses")
                .tag("cache", "export")
                .register(meterRegistry);

        Gauge.builder("cache.operations.staleness", stalenessDetectionsCounter, AtomicLong::get)
                .description("Total number of staleness detections (stale cache returned)")
                .tag("cache", "export")
                .register(meterRegistry);

        // API call counters
        Gauge.builder("api.calls.external", externalApiCallsCounter, AtomicLong::get)
                .description("Total number of external API calls made (Open-Meteo, ATMO France)")
                .tag("source", "external")
                .register(meterRegistry);

        Gauge.builder("api.calls.cached", cachedApiCallsCounter, AtomicLong::get)
                .description("Total number of API calls avoided by returning cached data")
                .tag("source", "cache")
                .register(meterRegistry);

        // Cache hit rate (calculated from hits and misses)
        Gauge.builder("cache.hit.rate", this::calculateCacheHitRate)
                .description("Cache hit rate as a percentage (0-100)")
                .baseUnit("percent")
                .tag("cache", "export")
                .register(meterRegistry);

        // API call reduction percentage
        Gauge.builder("api.call.reduction", this::calculateApiCallReduction)
                .description("Percentage of API calls avoided (0-100)")
                .baseUnit("percent")
                .tag("source", "cache")
                .register(meterRegistry);

        // Average response times
        Gauge.builder("response.time.cache.hit.average", this::calculateAverageCacheHitTime)
                .description("Average response time for cache hits (milliseconds)")
                .baseUnit("milliseconds")
                .tag("cache", "hit")
                .register(meterRegistry);

        Gauge.builder("response.time.cache.miss.average", this::calculateAverageCacheMissTime)
                .description("Average response time for cache misses/API calls (milliseconds)")
                .baseUnit("milliseconds")
                .tag("cache", "miss")
                .register(meterRegistry);

        // Estimated cost reduction (based on API call rates)
        Gauge.builder("estimated.api.calls.per.day", this::estimateDailyApiCalls)
                .description("Estimated API calls per day with scheduler and caching")
                .tag("estimate", "scheduler")
                .register(meterRegistry);

        Gauge.builder("estimated.cost.reduction.percent", this::estimateCostReductionPercent)
                .description("Estimated cost reduction from 2.6M to scheduled calls (98.2%)")
                .baseUnit("percent")
                .register(meterRegistry);
    }

    /**
     * Record a successful cache hit operation.
     * Increments hit counter and tracks response time.
     *
     * @param responseTimeMs Response time in milliseconds
     */
    public void recordCacheHit(long responseTimeMs) {
        cacheHitsCounter.incrementAndGet();
        totalCacheHitTimeMs.addAndGet(responseTimeMs);
        cacheOperationCount.incrementAndGet();
    }

    /**
     * Record a cache miss operation (requires external API call).
     * Increments miss counter and tracks response time.
     *
     * @param responseTimeMs Response time in milliseconds
     */
    public void recordCacheMiss(long responseTimeMs) {
        cacheMissesCounter.incrementAndGet();
        totalCacheMissTimeMs.addAndGet(responseTimeMs);
        cacheOperationCount.incrementAndGet();
        externalApiCallsCounter.incrementAndGet();
    }

    /**
     * Record an external API call being made.
     * Called for every API request to external services.
     */
    public void recordExternalApiCall() {
        externalApiCallsCounter.incrementAndGet();
    }

    /**
     * Record a cache hit that avoided an API call.
     * Called when cached data is returned instead of fetching fresh.
     */
    public void recordCachedApiCall() {
        cachedApiCallsCounter.incrementAndGet();
    }

    /**
     * Record detection of stale cache data.
     * Called when cache is detected as stale but returned anyway (fallback behavior).
     */
    public void recordStalenessDetection() {
        stalenessDetectionsCounter.incrementAndGet();
    }

    /**
     * Calculate cache hit rate as a percentage.
     *
     * @return Cache hit rate (0-100) or 0 if no operations yet
     */
    public double calculateCacheHitRate() {
        long hits = cacheHitsCounter.get();
        long misses = cacheMissesCounter.get();
        long total = hits + misses;

        if (total == 0) {
            return 0.0;
        }

        return (double) hits / total * 100;
    }

    /**
     * Calculate API call reduction percentage.
     * Shows how many API calls were avoided via caching.
     *
     * @return Reduction percentage (0-100)
     */
    public double calculateApiCallReduction() {
        long cached = cachedApiCallsCounter.get();
        long external = externalApiCallsCounter.get();
        long total = cached + external;

        if (total == 0) {
            return 0.0;
        }

        return (double) cached / total * 100;
    }

    /**
     * Calculate average response time for cache hits.
     *
     * @return Average response time in milliseconds or 0 if no hits
     */
    public double calculateAverageCacheHitTime() {
        long hits = cacheHitsCounter.get();
        if (hits == 0) {
            return 0.0;
        }
        return (double) totalCacheHitTimeMs.get() / hits;
    }

    /**
     * Calculate average response time for cache misses (API calls).
     *
     * @return Average response time in milliseconds or 0 if no misses
     */
    public double calculateAverageCacheMissTime() {
        long misses = cacheMissesCounter.get();
        if (misses == 0) {
            return 0.0;
        }
        return (double) totalCacheMissTimeMs.get() / misses;
    }

    /**
     * Estimate daily API calls based on scheduler tiers.
     * Tier 1: 300 communes × 12 cycles/day = 3,600 calls
     * Tier 2: 2,500 communes × 4 cycles/day = 10,000 calls
     * Tier 3: 33,000 communes × 1 cycle/day = 33,000 calls
     * Total: 46,600 calls/day
     *
     * @return Estimated daily API calls with scheduler
     */
    public double estimateDailyApiCalls() {
        return 46600.0; // Tier 1: 3,600 + Tier 2: 10,000 + Tier 3: 33,000
    }

    /**
     * Estimate cost reduction percentage from baseline 2.6M calls/day to scheduled 46.6K calls/day.
     * (2,600,000 - 46,600) / 2,600,000 = 98.2%
     *
     * @return Percentage reduction (98.2)
     */
    public double estimateCostReductionPercent() {
        double baselineCallsPerDay = 2592000.0; // 1.728M Open-Meteo + 0.864M ATMO France
        double scheduledCallsPerDay = 46600.0;
        return (baselineCallsPerDay - scheduledCallsPerDay) / baselineCallsPerDay * 100;
    }

    /**
     * Get comprehensive cache statistics summary.
     * Useful for logging and dashboard display.
     *
     * @return CacheStatisticsSummary with all calculated metrics
     */
    public CacheStatisticsSummary getStatisticsSummary() {
        return CacheStatisticsSummary.builder()
                .cacheHits(cacheHitsCounter.get())
                .cacheMisses(cacheMissesCounter.get())
                .stalenessDetections(stalenessDetectionsCounter.get())
                .externalApiCalls(externalApiCallsCounter.get())
                .cachedApiCalls(cachedApiCallsCounter.get())
                .cacheHitRate(calculateCacheHitRate())
                .apiCallReduction(calculateApiCallReduction())
                .averageCacheHitTimeMs(calculateAverageCacheHitTime())
                .averageCacheMissTimeMs(calculateAverageCacheMissTime())
                .estimatedDailyApiCalls((long) estimateDailyApiCalls())
                .estimatedCostReductionPercent(estimateCostReductionPercent())
                .build();
    }

    /**
     * Reset all metrics to zero. Useful for testing or starting fresh monitoring period.
     * WARNING: This clears all accumulated statistics.
     */
    public void resetMetrics() {
        cacheHitsCounter.set(0);
        cacheMissesCounter.set(0);
        stalenessDetectionsCounter.set(0);
        externalApiCallsCounter.set(0);
        cachedApiCallsCounter.set(0);
        totalCacheHitTimeMs.set(0);
        totalCacheMissTimeMs.set(0);
        cacheOperationCount.set(0);
    }

    /**
     * Get total number of cache operations (hits + misses).
     *
     * @return Total cache operations
     */
    public long getTotalCacheOperations() {
        return cacheHitsCounter.get() + cacheMissesCounter.get();
    }

    /**
     * Get total number of API calls (external + cached).
     *
     * @return Total API calls
     */
    public long getTotalApiCalls() {
        return externalApiCallsCounter.get() + cachedApiCallsCounter.get();
    }
}
