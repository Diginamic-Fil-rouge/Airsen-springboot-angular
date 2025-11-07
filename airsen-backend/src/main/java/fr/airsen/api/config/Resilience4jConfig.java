package fr.airsen.api.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for Resilience4j Circuit Breakers.
 *
 * Circuit breakers protect external API calls from cascading failures:
 * - ATMO API circuit breaker
 * - OpenMeteo (Weather) API circuit breaker
 * - INSEE API circuit breaker
 *
 * Circuit Breaker States:
 * 1. CLOSED: Normal operation, all requests pass through
 * 2. OPEN: Failure threshold exceeded, requests fail immediately
 * 3. HALF_OPEN: Testing if service recovered, limited requests allowed
 *
 * Configuration:
 * - Failure rate threshold: Percentage of failures to open circuit
 * - Wait duration: Time to wait in OPEN state before trying HALF_OPEN
 * - Permitted calls in half-open: Number of test requests in HALF_OPEN state
 * - Sliding window size: Number of calls to track for failure rate
 *
 * @see <a href="https://resilience4j.readme.io/docs/circuitbreaker">Resilience4j Circuit Breaker</a>
 */
@Configuration
public class Resilience4jConfig {

    private static final Logger log = LoggerFactory.getLogger(Resilience4jConfig.class);

    @Value("${rate-limiter.atmo.circuit-breaker.failure-threshold:0.5}")
    private float atmoFailureThreshold;

    @Value("${rate-limiter.atmo.circuit-breaker.wait-duration-seconds:60}")
    private int atmoWaitDuration;

    @Value("${rate-limiter.atmo.circuit-breaker.permitted-calls-half-open:10}")
    private int atmoPermittedCalls;

    @Value("${rate-limiter.weather.circuit-breaker.failure-threshold:0.5}")
    private float weatherFailureThreshold;

    @Value("${rate-limiter.weather.circuit-breaker.wait-duration-seconds:60}")
    private int weatherWaitDuration;

    @Value("${rate-limiter.weather.circuit-breaker.permitted-calls-half-open:5}")
    private int weatherPermittedCalls;

    @Value("${rate-limiter.insee.circuit-breaker.failure-threshold:0.5}")
    private float inseeFailureThreshold;

    @Value("${rate-limiter.insee.circuit-breaker.wait-duration-seconds:120}")
    private int inseeWaitDuration;

    @Value("${rate-limiter.insee.circuit-breaker.permitted-calls-half-open:5}")
    private int inseePermittedCalls;

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.ofDefaults();

        // Register ATMO circuit breaker
        registerAtmoCircuitBreaker(registry);

        // Register Weather circuit breaker
        registerWeatherCircuitBreaker(registry);

        // Register INSEE circuit breaker
        registerInseeCircuitBreaker(registry);

        return registry;
    }

    private void registerAtmoCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(atmoFailureThreshold * 100) // Convert to percentage
            .waitDurationInOpenState(Duration.ofSeconds(atmoWaitDuration))
            .permittedNumberOfCallsInHalfOpenState(atmoPermittedCalls)
            .slidingWindowSize(100)
            .minimumNumberOfCalls(10)
            .recordExceptions(
                Exception.class // Record all exceptions
            )
            .ignoreExceptions(
                IllegalArgumentException.class // Don't count validation errors
            )
            .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker("atmo", config);

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.warn("ATMO API Circuit Breaker state changed: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onFailureRateExceeded(event ->
                log.error("ATMO API Circuit Breaker failure rate exceeded: {}%",
                    event.getFailureRate()))
            .onCallNotPermitted(event ->
                log.warn("ATMO API call rejected by circuit breaker (circuit is OPEN)"));

        log.info("Registered ATMO circuit breaker: failure threshold={}%, wait={}s, half-open calls={}",
            atmoFailureThreshold * 100, atmoWaitDuration, atmoPermittedCalls);
    }

    private void registerWeatherCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(weatherFailureThreshold * 100)
            .waitDurationInOpenState(Duration.ofSeconds(weatherWaitDuration))
            .permittedNumberOfCallsInHalfOpenState(weatherPermittedCalls)
            .slidingWindowSize(100)
            .minimumNumberOfCalls(10)
            .recordExceptions(Exception.class)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker("weather", config);

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.warn("Weather API Circuit Breaker state changed: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onFailureRateExceeded(event ->
                log.error("Weather API Circuit Breaker failure rate exceeded: {}%",
                    event.getFailureRate()))
            .onCallNotPermitted(event ->
                log.warn("Weather API call rejected by circuit breaker (circuit is OPEN)"));

        log.info("Registered Weather circuit breaker: failure threshold={}%, wait={}s, half-open calls={}",
            weatherFailureThreshold * 100, weatherWaitDuration, weatherPermittedCalls);
    }

    private void registerInseeCircuitBreaker(CircuitBreakerRegistry registry) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(inseeFailureThreshold * 100)
            .waitDurationInOpenState(Duration.ofSeconds(inseeWaitDuration))
            .permittedNumberOfCallsInHalfOpenState(inseePermittedCalls)
            .slidingWindowSize(100)
            .minimumNumberOfCalls(10)
            .recordExceptions(Exception.class)
            .ignoreExceptions(IllegalArgumentException.class)
            .build();

        CircuitBreaker circuitBreaker = registry.circuitBreaker("insee", config);

        circuitBreaker.getEventPublisher()
            .onStateTransition(event ->
                log.warn("INSEE API Circuit Breaker state changed: {} -> {}",
                    event.getStateTransition().getFromState(),
                    event.getStateTransition().getToState()))
            .onFailureRateExceeded(event ->
                log.error("INSEE API Circuit Breaker failure rate exceeded: {}%",
                    event.getFailureRate()))
            .onCallNotPermitted(event ->
                log.warn("INSEE API call rejected by circuit breaker (circuit is OPEN)"));

        log.info("Registered INSEE circuit breaker: failure threshold={}%, wait={}s, half-open calls={}",
            inseeFailureThreshold * 100, inseeWaitDuration, inseePermittedCalls);
    }

    /**
     * Get circuit breaker for ATMO API.
     *
     * @param registry Circuit breaker registry
     * @return ATMO circuit breaker
     */
    @Bean
    public CircuitBreaker atmoCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("atmo");
    }

    /**
     * Get circuit breaker for Weather API.
     *
     * @param registry Circuit breaker registry
     * @return Weather circuit breaker
     */
    @Bean
    public CircuitBreaker weatherCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("weather");
    }

    /**
     * Get circuit breaker for INSEE API.
     *
     * @param registry Circuit breaker registry
     * @return INSEE circuit breaker
     */
    @Bean
    public CircuitBreaker inseeCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker("insee");
    }
}
