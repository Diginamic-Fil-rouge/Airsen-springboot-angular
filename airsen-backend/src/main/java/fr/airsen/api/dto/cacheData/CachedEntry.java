package fr.airsen.api.dto.cacheData;

import fr.airsen.api.entity.cacheData.CacheMetadata;

/**
 * Wrapper for cached data with metadata.
 *
 * Generic type T allows caching any response object:
 * - ExportDataResponse
 * - AirQualityResponseDTO
 * - WeatherDataDTO
 * - Any other DTO
 *
 * @param <T> Type of cached data
 */
public class CachedEntry<T> {
    private T data;
    private CacheMetadata metadata;

    /**
     * Create a cached entry with data and metadata.
     *
     * @param data the cached data
     * @param metadata the cache metadata (source, staleness, TTL)
     */
    public CachedEntry(T data, CacheMetadata metadata) {
        this.data = data;
        this.metadata = metadata;
    }

    /**
     * Is this entry fresh and ready to serve immediately?
     *
     * True if:
     * - Not expired (current time < expiresAt)
     * - Not stale (staleness < 0.8)
     *
     * Fresh entries are served directly without any background refresh.
     *
     * @return true if entry is fresh
     */
    public boolean isFresh() {
        return !metadata.isExpired() && !metadata.shouldRefresh();
    }

    /**
     * Can this entry be used as a fallback?
     *
     * True if not completely expired (even if stale).
     * Used when external API is down or rate-limited.
     *
     * Stale-but-usable entries are served while background refresh is attempted.
     * If refresh fails, the stale data is used and returned to client.
     *
     * @return true if entry is not expired (even if stale)
     */
    public boolean isUsableAsStale() {
        return !metadata.isExpired();
    }

    /**
     * Get the cached data.
     *
     * @return the cached data of type T
     */
    public T getData() {
        return data;
    }

    /**
     * Get the cache metadata.
     *
     * Metadata includes source, staleness, TTL, and age information.
     * Used for response headers and monitoring.
     *
     * @return the cache metadata
     */
    public CacheMetadata getMetadata() {
        return metadata;
    }
}
