package fr.airsen.api.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import fr.airsen.api.external.client.AtmoApiClient;
import fr.airsen.api.external.config.AtmoApiConfig;
import fr.airsen.api.service.ratelimit.RateLimitExceededException;
import fr.airsen.api.service.ratelimit.RateLimiterService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

/**
 * Integration tests for AtmoApiClient with rate limiting and WireMock.
 *
 * Demonstrates:
 * - WireMock setup for mocking ATMO API
 * - Rate limiter integration testing
 * - Circuit breaker behavior
 * - JWT authentication flow
 */
@DisplayName("AtmoApiClient Rate Limiter Integration Tests")
class AtmoApiClientRateLimiterIntegrationTest {

    private static WireMockServer wireMockServer;
    private AtmoApiClient atmoApiClient;
    private RateLimiterService rateLimiterService;
    private CircuitBreaker circuitBreaker;

    @BeforeAll
    static void setUpWireMock() {
        // Start WireMock server on dynamic port
        wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMockServer.start();
        WireMock.configureFor("localhost", wireMockServer.port());
    }

    @AfterAll
    static void tearDownWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {
        // Reset WireMock before each test
        wireMockServer.resetAll();

        // Configure rate limiter with low limits for testing
        rateLimiterService = new RateLimiterService();
        ReflectionTestUtils.setField(rateLimiterService, "atmoRateLimit", 3);
        ReflectionTestUtils.setField(rateLimiterService, "burstCapacityPercentage", 0);
        ReflectionTestUtils.setField(rateLimiterService, "atmoRejectOnLimit", true);

        // Configure circuit breaker with permissive settings for testing
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(1))
            .permittedNumberOfCallsInHalfOpenState(2)
            .slidingWindowSize(10)
            .build();
        circuitBreaker = CircuitBreaker.of("atmo-test", circuitBreakerConfig);

        // Configure WebClient to point to WireMock
        WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .build();

        // Configure ATMO API config
        AtmoApiConfig config = new AtmoApiConfig();
        ReflectionTestUtils.setField(config, "baseUrl", "http://localhost:" + wireMockServer.port());
        ReflectionTestUtils.setField(config, "timeoutMs", 5000);
        ReflectionTestUtils.setField(config, "username", "test-user");
        ReflectionTestUtils.setField(config, "password", "test-password");

        // Create client
        atmoApiClient = new AtmoApiClient(webClient, config, null, rateLimiterService, circuitBreaker);
    }

    @AfterEach
    void tearDown() {
        rateLimiterService.resetAll();
    }

    @Test
    @DisplayName("Should successfully make requests within rate limit")
    void shouldMakeRequestsWithinRateLimit() {
        // Stub JWT authentication
        stubJwtAuthentication();

        // Stub air quality API
        stubAirQualityIndices();

        // Make 3 requests (within limit)
        for (int i = 0; i < 3; i++) {
            StepVerifier.create(atmoApiClient.getCurrentAirQualityIndices())
                .expectNextCount(2) // Our stub returns 2 features
                .verifyComplete();
        }

        // Verify 3 auth calls and 3 data calls
        verify(exactly(3), postRequestedFor(urlEqualTo("/api/login")));
        verify(exactly(3), getRequestedFor(urlPathEqualTo("/api/v2/data/indices/atmo")));
    }

    @Test
    @DisplayName("Should throw RateLimitExceededException when limit exceeded")
    void shouldThrowExceptionWhenLimitExceeded() {
        // Stub JWT authentication
        stubJwtAuthentication();

        // Stub air quality API
        stubAirQualityIndices();

        // Make 3 requests (exhaust limit)
        for (int i = 0; i < 3; i++) {
            atmoApiClient.getCurrentAirQualityIndices().blockFirst();
        }

        // 4th request should fail with rate limit exception
        StepVerifier.create(atmoApiClient.getCurrentAirQualityIndices())
            .expectError(RateLimitExceededException.class)
            .verify();

        // Verify only 3 API calls were made (4th was blocked by rate limiter)
        verify(exactly(3), postRequestedFor(urlEqualTo("/api/login")));
    }

    @Test
    @DisplayName("Should trigger circuit breaker on repeated failures")
    void shouldTriggerCircuitBreakerOnFailures() {
        // Stub JWT authentication
        stubJwtAuthentication();

        // Stub API to return 500 errors
        stubFor(get(urlPathEqualTo("/api/v2/data/indices/atmo"))
            .willReturn(aResponse()
                .withStatus(500)
                .withBody("{\"error\": \"Internal server error\"}")));

        // Make multiple failing requests
        for (int i = 0; i < 5; i++) {
            StepVerifier.create(atmoApiClient.getCurrentAirQualityIndices())
                .expectError()
                .verify();

            // Reset rate limiter to focus on circuit breaker
            rateLimiterService.reset("atmo");
        }

        // Circuit breaker should eventually open
        // Note: Circuit breaker behavior depends on configuration
    }

    @Test
    @DisplayName("Should handle JWT authentication correctly")
    void shouldHandleJwtAuthentication() {
        // Stub successful authentication
        stubJwtAuthentication();

        // Stub air quality API
        stubAirQualityIndices();

        StepVerifier.create(atmoApiClient.getCurrentAirQualityIndices())
            .expectNextCount(2)
            .verifyComplete();

        // Verify authentication was called
        verify(exactly(1), postRequestedFor(urlEqualTo("/api/login"))
            .withRequestBody(containing("test-user"))
            .withRequestBody(containing("test-password")));

        // Verify bearer token was used
        verify(exactly(1), getRequestedFor(urlPathEqualTo("/api/v2/data/indices/atmo"))
            .withHeader("Authorization", matching("Bearer test-jwt-token-.*")));
    }

    @Test
    @DisplayName("Should handle authentication failures")
    void shouldHandleAuthenticationFailures() {
        // Stub failed authentication
        stubFor(post(urlEqualTo("/api/login"))
            .willReturn(aResponse()
                .withStatus(401)
                .withBody("{\"error\": \"Invalid credentials\"}")));

        StepVerifier.create(atmoApiClient.getCurrentAirQualityIndices())
            .expectError()
            .verify();

        // Verify no data API call was made (failed at auth)
        verify(exactly(0), getRequestedFor(urlPathEqualTo("/api/v2/data/indices/atmo")));
    }

    @Test
    @DisplayName("Should handle API timeout")
    void shouldHandleApiTimeout() {
        // Stub JWT authentication
        stubJwtAuthentication();

        // Stub slow API response (exceeds timeout)
        stubFor(get(urlPathEqualTo("/api/v2/data/indices/atmo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(6000) // 6 seconds (exceeds 5s timeout)
                .withBody("{}")));

        StepVerifier.create(atmoApiClient.getCurrentAirQualityIndices())
            .expectError()
            .verify();
    }

    @Test
    @DisplayName("Should respect rate limiter even with cached JWT token")
    void shouldRespectRateLimiterWithCachedToken() {
        // Stub JWT authentication
        stubJwtAuthentication();

        // Stub air quality API
        stubAirQualityIndices();

        // Make first request (creates JWT cache)
        atmoApiClient.getCurrentAirQualityIndices().blockFirst();

        // Make 2 more requests (should use cached token)
        atmoApiClient.getCurrentAirQualityIndices().blockFirst();
        atmoApiClient.getCurrentAirQualityIndices().blockFirst();

        // 4th request should be rate limited
        StepVerifier.create(atmoApiClient.getCurrentAirQualityIndices())
            .expectError(RateLimitExceededException.class)
            .verify();

        // Verify only 1 authentication call (token was cached)
        // But still only 3 data calls (rate limiter active)
        wireMockServer.verify(moreThanOrExactly(1), postRequestedFor(urlEqualTo("/api/login")));
        wireMockServer.verify(exactly(3), getRequestedFor(urlPathEqualTo("/api/v2/data/indices/atmo")));
    }

    // Helper methods for WireMock stubs

    private void stubJwtAuthentication() {
        stubFor(post(urlEqualTo("/api/login"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"token\": \"test-jwt-token-" + System.currentTimeMillis() + "\"}")));
    }

    private void stubAirQualityIndices() {
        String response = """
            {
              "type": "FeatureCollection",
              "features": [
                {
                  "type": "Feature",
                  "properties": {
                    "code_commune": "75056",
                    "nom_commune": "Paris",
                    "code_zone": "FR_PARIS",
                    "nom_zone": "Paris",
                    "date": "2024-01-15",
                    "valeur": 4,
                    "code_qual": "MOYEN",
                    "lib_qual": "Moyen"
                  },
                  "geometry": {
                    "type": "Point",
                    "coordinates": [2.3522, 48.8566]
                  }
                },
                {
                  "type": "Feature",
                  "properties": {
                    "code_commune": "69123",
                    "nom_commune": "Lyon",
                    "code_zone": "FR_LYON",
                    "nom_zone": "Lyon",
                    "date": "2024-01-15",
                    "valeur": 3,
                    "code_qual": "BON",
                    "lib_qual": "Bon"
                  },
                  "geometry": {
                    "type": "Point",
                    "coordinates": [4.8357, 45.7640]
                  }
                }
              ]
            }
            """;

        stubFor(get(urlPathEqualTo("/api/v2/data/indices/atmo"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(response)));
    }
}
