package fr.airsen.api.service.ratelimit;

/**
 * Exception thrown when API rate limit is exceeded.
 *
 * Provides detailed information about the rate limit violation including:
 * - Which API was rate limited
 * - The configured limit
 * - How long to wait before retrying
 *
 * This exception can be caught by:
 * - Circuit breakers to trigger open state
 * - Retry mechanisms to implement exponential backoff
 * - Global exception handlers to return 429 Too Many Requests responses
 */
public class RateLimitExceededException extends RuntimeException {

    private final String apiName;
    private final int requestsPerMinute;
    private final long retryAfterMillis;

    public RateLimitExceededException(String message, String apiName, int requestsPerMinute, long retryAfterMillis) {
        super(message);
        this.apiName = apiName;
        this.requestsPerMinute = requestsPerMinute;
        this.retryAfterMillis = retryAfterMillis;
    }

    public String getApiName() {
        return apiName;
    }

    public int getRequestsPerMinute() {
        return requestsPerMinute;
    }

    public long getRetryAfterMillis() {
        return retryAfterMillis;
    }

    public long getRetryAfterSeconds() {
        return retryAfterMillis / 1000;
    }
}
