package fr.airsen.api.service.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for enforcing API rate limits using Token Bucket algorithm.
 *
 * Implements rate limiting for external API calls to prevent quota exhaustion:
 * - ATMO API: Configurable limit (default: 60 req/min in prod, 30 in dev)
 * - OpenMeteo API: Configurable limit (default: 10 req/min in prod, 5 in dev)
 * - INSEE API: Configurable limit (default: 30 req/min in prod, 10 in dev)
 *
 * Uses Bucket4j's Token Bucket algorithm for smooth rate limiting with burst support.
 *
 * Configuration:
 * - rate-limiter.atmo.requests-per-minute: ATMO API rate limit
 * - rate-limiter.weather.requests-per-minute: OpenMeteo API rate limit
 * - rate-limiter.insee.requests-per-minute: INSEE API rate limit
 * - rate-limiter.*.reject-on-limit: Whether to reject or wait when limit exceeded
 *
 * @see <a href="https://bucket4j.com/">Bucket4j Documentation</a>
 */
@Service
public class RateLimiterService {

    private static final Logger log = LoggerFactory.getLogger(RateLimiterService.class);

    // Rate limits per minute (configured via application.yml)
    @Value("${rate-limiter.atmo.requests-per-minute:60}")
    private int atmoRateLimit;

    @Value("${rate-limiter.weather.requests-per-minute:10}")
    private int weatherRateLimit;

    @Value("${rate-limiter.insee.requests-per-minute:30}")
    private int inseeRateLimit;

    @Value("${rate-limiter.atmo.reject-on-limit:true}")
    private boolean atmoRejectOnLimit;

    @Value("${rate-limiter.weather.reject-on-limit:true}")
    private boolean weatherRejectOnLimit;

    @Value("${rate-limiter.insee.reject-on-limit:true}")
    private boolean inseeRejectOnLimit;

    // Burst capacity (allow short bursts up to this percentage above the limit)
    @Value("${rate-limiter.burst-capacity-percentage:20}")
    private int burstCapacityPercentage;

    // Thread-safe bucket storage
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Attempt to consume a token for ATMO API.
     *
     * @return true if request is allowed, false if rate limit exceeded
     * @throws RateLimitExceededException if reject-on-limit is true and limit exceeded
     */
    public boolean tryConsumeAtmo() {
        return tryConsume("atmo", atmoRateLimit, atmoRejectOnLimit);
    }

    /**
     * Attempt to consume a token for Weather API (OpenMeteo).
     *
     * @return true if request is allowed, false if rate limit exceeded
     * @throws RateLimitExceededException if reject-on-limit is true and limit exceeded
     */
    public boolean tryConsumeWeather() {
        return tryConsume("weather", weatherRateLimit, weatherRejectOnLimit);
    }

    /**
     * Attempt to consume a token for INSEE API.
     *
     * @return true if request is allowed, false if rate limit exceeded
     * @throws RateLimitExceededException if reject-on-limit is true and limit exceeded
     */
    public boolean tryConsumeInsee() {
        return tryConsume("insee", inseeRateLimit, inseeRejectOnLimit);
    }

    /**
     * Get or create bucket for the specified API.
     *
     * Uses lazy initialization with double-check locking for thread safety.
     *
     * @param apiName API identifier
     * @param requestsPerMinute Rate limit
     * @return Token bucket for this API
     */
    private Bucket getBucket(String apiName, int requestsPerMinute) {
        return buckets.computeIfAbsent(apiName, key -> createBucket(requestsPerMinute));
    }

    /**
     * Create a new token bucket with specified rate limit.
     *
     * Token Bucket Configuration:
     * - Capacity: requestsPerMinute + burst capacity
     * - Refill rate: requestsPerMinute tokens per minute
     * - Refill strategy: Greedy (refill tokens as soon as possible)
     *
     * Example: 60 req/min with 20% burst = 72 token capacity, refills at 60/min
     *
     * @param requestsPerMinute Base rate limit
     * @return Configured bucket
     */
    private Bucket createBucket(int requestsPerMinute) {
        int burstCapacity = requestsPerMinute + (requestsPerMinute * burstCapacityPercentage / 100);

        Bandwidth limit = Bandwidth.builder()
            .capacity(burstCapacity)
            .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
            .build();

        log.debug("Created rate limiter bucket: {} req/min with {} burst capacity",
                requestsPerMinute, burstCapacity);

        return Bucket.builder()
            .addLimit(limit)
            .build();
    }

    /**
     * Try to consume a token from the bucket for the specified API.
     *
     * @param apiName API identifier
     * @param requestsPerMinute Rate limit for this API
     * @param rejectOnLimit Whether to throw exception or return false when limit exceeded
     * @return true if token consumed, false if limit exceeded (only if !rejectOnLimit)
     * @throws RateLimitExceededException if rejectOnLimit and limit exceeded
     */
    private boolean tryConsume(String apiName, int requestsPerMinute, boolean rejectOnLimit) {
        Bucket bucket = getBucket(apiName, requestsPerMinute);

        boolean consumed = bucket.tryConsume(1);

        if (!consumed) {
            long waitTimeNanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();
            long waitTimeMillis = waitTimeNanos / 1_000_000;

            log.warn("Rate limit exceeded for {} API. Wait time: {}ms. Limit: {} req/min",
                    apiName, waitTimeMillis, requestsPerMinute);

            if (rejectOnLimit) {
                throw new RateLimitExceededException(
                    String.format("Rate limit exceeded for %s API. Limit: %d requests/minute. " +
                                "Retry after %d milliseconds.",
                                apiName, requestsPerMinute, waitTimeMillis),
                    apiName,
                    requestsPerMinute,
                    waitTimeMillis
                );
            }
        } else {
            long availableTokens = bucket.getAvailableTokens();
            log.trace("{} API rate limiter: consumed 1 token, {} tokens remaining",
                    apiName, availableTokens);
        }

        return consumed;
    }

    /**
     * Block and wait until a token becomes available.
     *
     * Useful for scenarios where you want to wait rather than fail.
     * Blocks the current thread until a token is available.
     *
     * @param apiName API identifier
     * @param requestsPerMinute Rate limit
     * @throws InterruptedException if thread is interrupted while waiting
     */
    public void consumeBlocking(String apiName, int requestsPerMinute) throws InterruptedException {
        Bucket bucket = getBucket(apiName, requestsPerMinute);

        long waitTimeNanos = bucket.estimateAbilityToConsume(1).getNanosToWaitForRefill();

        if (waitTimeNanos > 0) {
            long waitTimeMillis = waitTimeNanos / 1_000_000;
            log.info("Rate limit reached for {} API. Waiting {}ms for token refill...",
                    apiName, waitTimeMillis);
            Thread.sleep(waitTimeMillis);
        }

        bucket.asBlocking().consume(1);
    }

    /**
     * Get current statistics for a specific API's rate limiter.
     *
     * @param apiName API identifier
     * @param requestsPerMinute Configured rate limit
     * @return Statistics object with current state
     */
    public RateLimiterStats getStats(String apiName, int requestsPerMinute) {
        Bucket bucket = getBucket(apiName, requestsPerMinute);

        long availableTokens = bucket.getAvailableTokens();
        int capacity = calculateCapacity(requestsPerMinute);
        double utilizationPercentage = ((capacity - availableTokens) * 100.0) / capacity;

        return new RateLimiterStats(
            apiName,
            requestsPerMinute,
            capacity,
            availableTokens,
            utilizationPercentage
        );
    }

    /**
     * Get statistics for all configured rate limiters.
     *
     * @return Map of API name to stats
     */
    public Map<String, RateLimiterStats> getAllStats() {
        return Map.of(
            "atmo", getStats("atmo", atmoRateLimit),
            "weather", getStats("weather", weatherRateLimit),
            "insee", getStats("insee", inseeRateLimit)
        );
    }

    /**
     * Reset rate limiter for a specific API (for testing or admin purposes).
     *
     * @param apiName API to reset
     */
    public void reset(String apiName) {
        buckets.remove(apiName);
        log.info("Rate limiter reset for {} API", apiName);
    }

    /**
     * Reset all rate limiters.
     */
    public void resetAll() {
        buckets.clear();
        log.info("All rate limiters reset");
    }

    private int calculateCapacity(int requestsPerMinute) {
        return requestsPerMinute + (requestsPerMinute * burstCapacityPercentage / 100);
    }

    /**
     * Rate limiter statistics record.
     */
    public record RateLimiterStats(
        String apiName,
        int requestsPerMinute,
        int capacity,
        long availableTokens,
        double utilizationPercentage
    ) {}
}
