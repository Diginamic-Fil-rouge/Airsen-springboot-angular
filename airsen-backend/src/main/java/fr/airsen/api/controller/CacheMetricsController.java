package fr.airsen.api.controller;

import fr.airsen.api.dto.metrics.CacheStatisticsSummary;
import fr.airsen.api.service.metrics.CacheMetricsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for exposing cache metrics and statistics.
 *
 * This controller provides endpoints to view real-time cache performance metrics,
 * useful for monitoring dashboard integration and performance analysis.
 *
 * Endpoints:
 * - GET /api/v1/metrics/cache - Get comprehensive cache statistics summary
 *
 * All endpoints require ADMIN role for security (prevents unauthorized metrics exposure).
 *
 * Example response from /metrics/cache:
 * ```json
 * {
 *   "cacheHits": 15234,
 *   "cacheMisses": 2156,
 *   "stalenessDetections": 34,
 *   "externalApiCalls": 2190,
 *   "cachedApiCalls": 15200,
 *   "cacheHitRate": 87.6,
 *   "apiCallReduction": 87.4,
 *   "averageCacheHitTimeMs": 45.2,
 *   "averageCacheMissTimeMs": 312.5,
 *   "estimatedDailyApiCalls": 46600,
 *   "estimatedCostReductionPercent": 98.2
 * }
 * ```
 *
 * @author AIRSEN Development Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/v1/metrics")
public class CacheMetricsController {

    private final CacheMetricsService cacheMetricsService;

    public CacheMetricsController(CacheMetricsService cacheMetricsService) {
        this.cacheMetricsService = cacheMetricsService;
    }

    /**
     * Get comprehensive cache statistics summary.
     *
     * Returns all cache metrics including hit rate, API call reduction, response times,
     * and estimated performance improvements. Requires ADMIN role to prevent
     * unauthorized performance data exposure.
     *
     * @return ResponseEntity containing CacheStatisticsSummary with all metrics
     *
     * Example response (HTTP 200 OK):
     * ```json
     * {
     *   "cacheHits": 15234,
     *   "cacheMisses": 2156,
     *   "stalenessDetections": 34,
     *   "externalApiCalls": 2190,
     *   "cachedApiCalls": 15200,
     *   "cacheHitRate": 87.6,
     *   "apiCallReduction": 87.4,
     *   "averageCacheHitTimeMs": 45.2,
     *   "averageCacheMissTimeMs": 312.5,
     *   "estimatedDailyApiCalls": 46600,
     *   "estimatedCostReductionPercent": 98.2
     * }
     * ```
     *
     * Unauthorized response (HTTP 403 FORBIDDEN):
     * When user lacks ADMIN role or is not authenticated.
     */
    @GetMapping("/cache")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CacheStatisticsSummary> getCacheStatistics() {
        CacheStatisticsSummary statistics = cacheMetricsService.getStatisticsSummary();
        return ResponseEntity.ok(statistics);
    }
}
