package fr.airsen.api.config;

import fr.airsen.api.service.metrics.CacheMetricsService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Configuration component for initializing cache metrics.
 *
 * This component ensures that all Micrometer metrics gauges are registered
 * when the Spring application starts. This registration makes metrics available
 * via the /api/v1/actuator/metrics endpoint.
 *
 * Metrics initialized:
 * - cache.operations.hits: Cache hit counter
 * - cache.operations.misses: Cache miss counter
 * - cache.operations.staleness: Staleness detection counter
 * - api.calls.external: External API call counter
 * - api.calls.cached: Cached API call counter
 * - cache.hit.rate: Hit rate percentage (0-100)
 * - api.call.reduction: Call reduction percentage (0-100)
 * - response.time.cache.hit.average: Avg hit response time (ms)
 * - response.time.cache.miss.average: Avg miss response time (ms)
 * - estimated.api.calls.per.day: Estimated daily calls (46,600)
 * - estimated.cost.reduction.percent: Cost reduction percentage (98.2%)
 *
 * @author AIRSEN Development Team
 * @version 1.0
 */
@Component
public class MetricsConfiguration {

    private final CacheMetricsService cacheMetricsService;

    public MetricsConfiguration(CacheMetricsService cacheMetricsService) {
        this.cacheMetricsService = cacheMetricsService;
    }

    /**
     * Initialize all metrics when application is ready.
     *
     * This method is called after the Spring application context is fully loaded,
     * ensuring all beans are available before registering metrics with Micrometer.
     *
     * Event: ApplicationReadyEvent
     * Purpose: Register all cache metrics gauges with MeterRegistry
     * Timing: Runs once during application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeMetrics() {
        cacheMetricsService.initializeMetrics();
    }
}
