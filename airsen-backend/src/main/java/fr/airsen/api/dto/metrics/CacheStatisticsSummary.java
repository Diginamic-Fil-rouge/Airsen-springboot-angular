package fr.airsen.api.dto.metrics;

/**
 * Data Transfer Object for cache statistics summary.
 * Provides a snapshot of all cache metrics at a given point in time.
 *
 * This record is used for:
 * - Exposing metrics via REST endpoint
 * - Logging statistics reports
 * - Monitoring dashboard display
 * - Performance analysis and trending
 *
 * @param cacheHits Total number of cache hits
 * @param cacheMisses Total number of cache misses
 * @param stalenessDetections Number of times stale cache was returned
 * @param externalApiCalls Total number of external API calls made
 * @param cachedApiCalls Total number of API calls avoided via cache
 * @param cacheHitRate Cache hit rate as percentage (0-100)
 * @param apiCallReduction API call reduction percentage (0-100)
 * @param averageCacheHitTimeMs Average response time for cache hits (milliseconds)
 * @param averageCacheMissTimeMs Average response time for cache misses/API calls (milliseconds)
 * @param estimatedDailyApiCalls Estimated daily API calls with scheduler (46,600)
 * @param estimatedCostReductionPercent Estimated cost reduction percentage (98.2%)
 *
 * @author AIRSEN Development Team
 * @version 1.0
 */
public record CacheStatisticsSummary(
        long cacheHits,
        long cacheMisses,
        long stalenessDetections,
        long externalApiCalls,
        long cachedApiCalls,
        double cacheHitRate,
        double apiCallReduction,
        double averageCacheHitTimeMs,
        double averageCacheMissTimeMs,
        long estimatedDailyApiCalls,
        double estimatedCostReductionPercent
) {
    /**
     * Create a builder for CacheStatisticsSummary.
     * Provides fluent interface for constructing statistics objects.
     *
     * @return new CacheStatisticsSummaryBuilder
     */
    public static CacheStatisticsSummaryBuilder builder() {
        return new CacheStatisticsSummaryBuilder();
    }

    /**
     * Builder class for CacheStatisticsSummary.
     * Provides fluent interface for constructing statistics objects.
     */
    public static class CacheStatisticsSummaryBuilder {
        private long cacheHits;
        private long cacheMisses;
        private long stalenessDetections;
        private long externalApiCalls;
        private long cachedApiCalls;
        private double cacheHitRate;
        private double apiCallReduction;
        private double averageCacheHitTimeMs;
        private double averageCacheMissTimeMs;
        private long estimatedDailyApiCalls;
        private double estimatedCostReductionPercent;

        public CacheStatisticsSummaryBuilder cacheHits(long cacheHits) {
            this.cacheHits = cacheHits;
            return this;
        }

        public CacheStatisticsSummaryBuilder cacheMisses(long cacheMisses) {
            this.cacheMisses = cacheMisses;
            return this;
        }

        public CacheStatisticsSummaryBuilder stalenessDetections(long stalenessDetections) {
            this.stalenessDetections = stalenessDetections;
            return this;
        }

        public CacheStatisticsSummaryBuilder externalApiCalls(long externalApiCalls) {
            this.externalApiCalls = externalApiCalls;
            return this;
        }

        public CacheStatisticsSummaryBuilder cachedApiCalls(long cachedApiCalls) {
            this.cachedApiCalls = cachedApiCalls;
            return this;
        }

        public CacheStatisticsSummaryBuilder cacheHitRate(double cacheHitRate) {
            this.cacheHitRate = cacheHitRate;
            return this;
        }

        public CacheStatisticsSummaryBuilder apiCallReduction(double apiCallReduction) {
            this.apiCallReduction = apiCallReduction;
            return this;
        }

        public CacheStatisticsSummaryBuilder averageCacheHitTimeMs(double averageCacheHitTimeMs) {
            this.averageCacheHitTimeMs = averageCacheHitTimeMs;
            return this;
        }

        public CacheStatisticsSummaryBuilder averageCacheMissTimeMs(double averageCacheMissTimeMs) {
            this.averageCacheMissTimeMs = averageCacheMissTimeMs;
            return this;
        }

        public CacheStatisticsSummaryBuilder estimatedDailyApiCalls(long estimatedDailyApiCalls) {
            this.estimatedDailyApiCalls = estimatedDailyApiCalls;
            return this;
        }

        public CacheStatisticsSummaryBuilder estimatedCostReductionPercent(double estimatedCostReductionPercent) {
            this.estimatedCostReductionPercent = estimatedCostReductionPercent;
            return this;
        }

        public CacheStatisticsSummary build() {
            return new CacheStatisticsSummary(
                    cacheHits,
                    cacheMisses,
                    stalenessDetections,
                    externalApiCalls,
                    cachedApiCalls,
                    cacheHitRate,
                    apiCallReduction,
                    averageCacheHitTimeMs,
                    averageCacheMissTimeMs,
                    estimatedDailyApiCalls,
                    estimatedCostReductionPercent
            );
        }
    }
}
