package fr.airsen.api.entity.cacheData;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Metadata for cache entries with source-aware TTL strategy.
 *
 * Tracks where cached data came from and how fresh it is. Different data sources
 * have different confidence levels and TTL values:
 * - USER_UPDATE: Manual refresh by user (highest confidence, 12h)
 * - SCHEDULED_UPDATE: Automatic scheduler (medium confidence, 2-24h by tier)
 * - ON_DEMAND_FETCH: Cache miss or force refresh (medium confidence, 6h)
 * - EXPORT_SNAPSHOT: Short-lived export data (15m)
 * - HISTORICAL_DATA: Time-series data (24h)
 */
public class CacheMetadata {

    /**
     * Data source determines TTL and refresh strategy.
     * Each source has different confidence level and staleness tolerance.
     */
    public enum DataSource {
        USER_UPDATE(Duration.ofHours(12)),
        SCHEDULED_UPDATE_TIER1(Duration.ofHours(2)),           // ≥100k population, needs frequent updates
        SCHEDULED_UPDATE_TIER2(Duration.ofHours(6)),           // 10k-99,999 population
        SCHEDULED_UPDATE_TIER3(Duration.ofHours(24)),          // <10k population
        ON_DEMAND_FETCH(Duration.ofHours(6)),
        EXPORT_SNAPSHOT(Duration.ofMinutes(15)),
        HISTORICAL_DATA(Duration.ofHours(24));

        private final Duration defaultTtl;

        DataSource(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public Duration getDefaultTtl() {
            return defaultTtl;
        }
    }

    private String key;
    private DataSource source;
    private LocalDateTime cachedAt;
    private LocalDateTime expiresAt;
    private long ttlSeconds;
    private String version = "1.0";

    /**
     * Create cache metadata with source-aware TTL.
     *
     * @param key the cache key
     * @param source the data source (determines TTL)
     */
    public CacheMetadata(String key, DataSource source) {
        this.key = key;
        this.source = source;
        this.cachedAt = LocalDateTime.now();
        this.ttlSeconds = source.getDefaultTtl().getSeconds();
        this.expiresAt = cachedAt.plusSeconds(ttlSeconds);
    }

    /**
     * Calculate staleness ratio between 0.0 (just cached) and 1.0+ (expired).
     *
     * Used by schedulers to decide whether to refresh:
     * - 0.0-0.8: Cache still fresh enough
     * - 0.8-1.0: Stale but usable, should refresh soon
     * - 1.0+: Expired, must not use
     *
     * @return staleness ratio
     */
    public double getStaleness() {
        long age = Duration.between(cachedAt, LocalDateTime.now()).getSeconds();
        return Math.min((double) age / ttlSeconds, 1.0);
    }

    /**
     * Should this cache entry be refreshed?
     *
     * True when staleness >= 80%, preventing thundering herd at exact TTL expiration.
     * Gives 20% buffer for background refresh operations.
     *
     * @return true if staleness >= 0.8
     */
    public boolean shouldRefresh() {
        return getStaleness() >= 0.8;
    }

    /**
     * Is this cache entry completely expired?
     *
     * @return true if current time > expiresAt
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Human-readable age description for response headers.
     * Examples: "just now", "15 minutes ago", "3 hours ago", "2 days ago"
     *
     * @return human-readable age string
     */
    public String getAgeDescription() {
        long minutes = Duration.between(cachedAt, LocalDateTime.now()).toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes + " minute" + (minutes == 1 ? "" : "s") + " ago";

        long hours = minutes / 60;
        if (hours < 24) return hours + " hour" + (hours == 1 ? "" : "s") + " ago";

        long days = hours / 24;
        return days + " day" + (days == 1 ? "" : "s") + " ago";
    }

    // Getters
    public String getKey() {
        return key;
    }

    public DataSource getSource() {
        return source;
    }

    public LocalDateTime getCachedAt() {
        return cachedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public String getVersion() {
        return version;
    }
}
