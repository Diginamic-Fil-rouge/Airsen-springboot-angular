package fr.airsen.api.service.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for RateLimiterService.
 *
 * Tests the Token Bucket rate limiting implementation including:
 * - Basic rate limiting behavior
 * - Burst capacity handling
 * - Reject vs wait behavior
 * - Token refill over time
 * - Multiple API limits
 */
@DisplayName("RateLimiterService Unit Tests")
class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();

        // Configure test rate limits (lower for faster tests)
        ReflectionTestUtils.setField(rateLimiterService, "atmoRateLimit", 5);
        ReflectionTestUtils.setField(rateLimiterService, "weatherRateLimit", 3);
        ReflectionTestUtils.setField(rateLimiterService, "inseeRateLimit", 2);

        // Configure burst capacity
        ReflectionTestUtils.setField(rateLimiterService, "burstCapacityPercentage", 20);

        // Configure reject behavior
        ReflectionTestUtils.setField(rateLimiterService, "atmoRejectOnLimit", true);
        ReflectionTestUtils.setField(rateLimiterService, "weatherRejectOnLimit", true);
        ReflectionTestUtils.setField(rateLimiterService, "inseeRejectOnLimit", false);
    }

    @Test
    @DisplayName("Should allow requests within rate limit")
    void shouldAllowRequestsWithinLimit() {
        // ATMO rate limit is 5 req/min, burst capacity 20% = 6 tokens total
        assertThat(rateLimiterService.tryConsumeAtmo()).isTrue();
        assertThat(rateLimiterService.tryConsumeAtmo()).isTrue();
        assertThat(rateLimiterService.tryConsumeAtmo()).isTrue();
        assertThat(rateLimiterService.tryConsumeAtmo()).isTrue();
        assertThat(rateLimiterService.tryConsumeAtmo()).isTrue();
    }

    @Test
    @DisplayName("Should reject requests exceeding rate limit when reject-on-limit is true")
    void shouldRejectExcessRequestsWhenRejectOnLimit() {
        // ATMO: 5 req/min + 20% burst = 6 tokens
        // Consume all 6 tokens
        for (int i = 0; i < 6; i++) {
            rateLimiterService.tryConsumeAtmo();
        }

        // 7th request should throw exception
        assertThatThrownBy(() -> rateLimiterService.tryConsumeAtmo())
            .isInstanceOf(RateLimitExceededException.class)
            .hasMessageContaining("Rate limit exceeded for atmo API")
            .hasFieldOrPropertyWithValue("apiName", "atmo")
            .hasFieldOrPropertyWithValue("requestsPerMinute", 5);
    }

    @Test
    @DisplayName("Should return false when reject-on-limit is false")
    void shouldReturnFalseWhenNotRejectOnLimit() {
        // INSEE: 2 req/min + 20% burst = 2 tokens (floor of 2.4)
        // Consume all tokens
        assertThat(rateLimiterService.tryConsumeInsee()).isTrue();
        assertThat(rateLimiterService.tryConsumeInsee()).isTrue();

        // 3rd request should return false (not throw exception)
        assertThat(rateLimiterService.tryConsumeInsee()).isFalse();
    }

    @Test
    @DisplayName("Should handle burst capacity correctly")
    void shouldHandleBurstCapacity() {
        // Weather: 3 req/min + 20% burst = 3 tokens (floor of 3.6)
        assertThat(rateLimiterService.tryConsumeWeather()).isTrue();
        assertThat(rateLimiterService.tryConsumeWeather()).isTrue();
        assertThat(rateLimiterService.tryConsumeWeather()).isTrue();

        // 4th request exceeds burst capacity
        assertThatThrownBy(() -> rateLimiterService.tryConsumeWeather())
            .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("Should track separate limits for different APIs")
    void shouldTrackSeparateLimitsForDifferentApis() {
        // Each API has its own bucket
        assertThat(rateLimiterService.tryConsumeAtmo()).isTrue();
        assertThat(rateLimiterService.tryConsumeWeather()).isTrue();
        assertThat(rateLimiterService.tryConsumeInsee()).isTrue();

        // Consuming from one API doesn't affect others
        for (int i = 0; i < 5; i++) {
            rateLimiterService.tryConsumeAtmo();
        }
        // ATMO exhausted, but others still work
        assertThat(rateLimiterService.tryConsumeWeather()).isTrue();
        assertThat(rateLimiterService.tryConsumeInsee()).isTrue();
    }

    @Test
    @DisplayName("Should provide accurate retry-after information")
    void shouldProvideRetryAfterInformation() {
        // Exhaust ATMO tokens (6 total)
        for (int i = 0; i < 6; i++) {
            rateLimiterService.tryConsumeAtmo();
        }

        try {
            rateLimiterService.tryConsumeAtmo();
        } catch (RateLimitExceededException e) {
            assertThat(e.getApiName()).isEqualTo("atmo");
            assertThat(e.getRequestsPerMinute()).isEqualTo(5);
            assertThat(e.getRetryAfterMillis()).isGreaterThan(0);
            assertThat(e.getRetryAfterSeconds()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("Should reset rate limiter for specific API")
    void shouldResetSpecificApi() {
        // Exhaust ATMO tokens
        for (int i = 0; i < 6; i++) {
            rateLimiterService.tryConsumeAtmo();
        }

        // Should be exhausted
        assertThatThrownBy(() -> rateLimiterService.tryConsumeAtmo())
            .isInstanceOf(RateLimitExceededException.class);

        // Reset ATMO
        rateLimiterService.reset("atmo");

        // Should work again
        assertThat(rateLimiterService.tryConsumeAtmo()).isTrue();
    }

    @Test
    @DisplayName("Should reset all rate limiters")
    void shouldResetAllRateLimiters() {
        // Exhaust all APIs
        for (int i = 0; i < 6; i++) rateLimiterService.tryConsumeAtmo();
        for (int i = 0; i < 3; i++) rateLimiterService.tryConsumeWeather();
        for (int i = 0; i < 2; i++) rateLimiterService.tryConsumeInsee();

        // All should be exhausted
        assertThatThrownBy(() -> rateLimiterService.tryConsumeAtmo())
            .isInstanceOf(RateLimitExceededException.class);
        assertThatThrownBy(() -> rateLimiterService.tryConsumeWeather())
            .isInstanceOf(RateLimitExceededException.class);
        assertThat(rateLimiterService.tryConsumeInsee()).isFalse();

        // Reset all
        rateLimiterService.resetAll();

        // All should work again
        assertThat(rateLimiterService.tryConsumeAtmo()).isTrue();
        assertThat(rateLimiterService.tryConsumeWeather()).isTrue();
        assertThat(rateLimiterService.tryConsumeInsee()).isTrue();
    }

    @Test
    @DisplayName("Should provide statistics for rate limiters")
    void shouldProvideStatistics() {
        // Consume some tokens
        rateLimiterService.tryConsumeAtmo();
        rateLimiterService.tryConsumeAtmo();

        RateLimiterService.RateLimiterStats stats = rateLimiterService.getStats("atmo", 5);

        assertThat(stats.apiName()).isEqualTo("atmo");
        assertThat(stats.requestsPerMinute()).isEqualTo(5);
        assertThat(stats.capacity()).isEqualTo(6); // 5 + 20% burst
        assertThat(stats.availableTokens()).isEqualTo(4); // 6 - 2 consumed
        assertThat(stats.utilizationPercentage()).isCloseTo(33.33, within(0.1));
    }

    @Test
    @DisplayName("Should provide statistics for all APIs")
    void shouldProvideAllStatistics() {
        var allStats = rateLimiterService.getAllStats();

        assertThat(allStats).containsKeys("atmo", "weather", "insee");
        assertThat(allStats.get("atmo").requestsPerMinute()).isEqualTo(5);
        assertThat(allStats.get("weather").requestsPerMinute()).isEqualTo(3);
        assertThat(allStats.get("insee").requestsPerMinute()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should handle concurrent requests correctly")
    void shouldHandleConcurrentRequests() throws InterruptedException {
        // This is a basic concurrency test
        // In production, you'd use CountDownLatch and multiple threads

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                try {
                    rateLimiterService.tryConsumeAtmo();
                } catch (RateLimitExceededException ignored) {
                }
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 3; i++) {
                try {
                    rateLimiterService.tryConsumeAtmo();
                } catch (RateLimitExceededException ignored) {
                }
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // Total 6 tokens consumed (or attempted)
        // Should be exhausted
        assertThatThrownBy(() -> rateLimiterService.tryConsumeAtmo())
            .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    @DisplayName("Should allow token refill over time")
    void shouldAllowTokenRefillOverTime() throws InterruptedException {
        // This test is time-sensitive and may be flaky
        // In production tests, you might want to mock the clock

        // Exhaust tokens
        for (int i = 0; i < 6; i++) {
            rateLimiterService.tryConsumeAtmo();
        }

        // Should be exhausted
        assertThatThrownBy(() -> rateLimiterService.tryConsumeAtmo())
            .isInstanceOf(RateLimitExceededException.class);

        // Wait for some refill (5 req/min = 1 token every 12 seconds)
        // Wait 15 seconds to get at least 1 token back
        Thread.sleep(15000);

        // Should have at least 1 token now
        assertThat(rateLimiterService.tryConsumeAtmo()).isTrue();
    }

    private static org.assertj.core.data.Offset<Double> within(double offset) {
        return org.assertj.core.data.Offset.offset(offset);
    }
}
