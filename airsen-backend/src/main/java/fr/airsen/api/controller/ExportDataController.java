package fr.airsen.api.controller;

import fr.airsen.api.dto.cache.CachedEntry;
import fr.airsen.api.dto.response.ExportDataResponse;
import fr.airsen.api.dto.response.HistoricalDataResponse;
import fr.airsen.api.entity.cache.CacheMetadata;
import fr.airsen.api.service.ExportDataService;
import fr.airsen.api.service.cache.SmartCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST Controller for export data endpoints.
 * 
 * Provides optimized data aggregation endpoints for client-side PDF/CSV export generation
 * with integrated intelligent caching for performance optimization.
 * 
 * Cache Strategy:
 * - Export data is cached with 6-hour TTL (ON_DEMAND_FETCH source)
 * - Scheduled refresh every 2-24 hours depending on commune tier
 * - Response headers include cache freshness indicators
 */
@RestController
@RequestMapping("communes")
@Tag(name = "Export Data", description = "Endpoints for data export (client-side generation)")
@SecurityRequirement(name = "bearerAuth")
public class ExportDataController {

    private final ExportDataService exportDataService;
    private final SmartCacheService cacheService;

    @Autowired
    public ExportDataController(ExportDataService exportDataService, SmartCacheService cacheService) {
        this.exportDataService = exportDataService;
        this.cacheService = cacheService;
    }

    /**
     * Gets export data for a commune (current snapshot).
     * 
     * Retrieves all data needed for PDF export in a single API call, including:
     * - Commune information (name, INSEE code, population, geographic data)
     * - Latest air quality measurements (ATMO index, pollutants)
     * - Latest weather data (temperature, humidity, wind)
     * - Data quality metadata (freshness indicators)
     * 
     * Caching Strategy:
     * - Data is cached with 6-hour TTL (ON_DEMAND_FETCH source)
     * - Scheduled refresh every 2-24 hours depending on commune population
     * - Stale data returned if API is temporarily unavailable
     * - Response headers indicate cache freshness
     * 
     * @param inseeCode commune INSEE code (5-digit identifier)
     * @param forceRefresh optional parameter to bypass cache and fetch fresh data (admin only)
     * @return ExportDataResponse with complete current commune state + cache metadata headers
     */
    @GetMapping("/{inseeCode}/export-data")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Get export data for PDF generation",
            description = "Retrieves all data needed for PDF export in a single API call. " +
                         "Data is intelligently cached with 6-hour TTL and scheduled refresh. " +
                         "Response headers indicate cache freshness and data source.",
            parameters = {
                    @Parameter(
                            name = "inseeCode",
                            description = "5-digit INSEE code of the commune",
                            example = "75056",
                            required = true
                    ),
                    @Parameter(
                            name = "forceRefresh",
                            description = "Admin only: Force bypass cache and fetch fresh data from API",
                            example = "false",
                            required = false
                    )
            }
    )
    @ApiResponse(
            responseCode = "200",
            description = "Export data retrieved successfully (from cache or API)",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ExportDataResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Commune not found"
    )
    public ResponseEntity<ExportDataResponse> getExportData(
            @PathVariable String inseeCode,
            @RequestParam(required = false, defaultValue = "false") boolean forceRefresh
    ) {
        // Build consistent cache key
        String cacheKey = "export:" + inseeCode;

        // Get data from cache or API
        CachedEntry<ExportDataResponse> cachedData = cacheService.getOrFetch(
            cacheKey,
            ExportDataResponse.class,
            CacheMetadata.DataSource.ON_DEMAND_FETCH,
            forceRefresh,
            () -> exportDataService.getExportData(inseeCode)
        );

        ExportDataResponse data = cachedData.getData();
        CacheMetadata metadata = cachedData.getMetadata();

        // Build response with cache metadata headers
        return ResponseEntity.ok()
            .header("X-Data-Source", metadata.getSource().toString())
            .header("X-Cache-Age", metadata.getAgeDescription())
            .header("X-Cache-Freshness", String.format("%.0f%%", (1 - metadata.getStaleness()) * 100))
            .header("X-Cache-TTL", metadata.getTtlSeconds() + " seconds")
            .header("X-Cache-Fresh", String.valueOf(cachedData.isFresh()))
            .body(data);
    }

    /**
     * Gets historical data for a commune (time-series for CSV export).
     * 
     * Retrieves time-series data for air quality and weather measurements over a specified
     * date range, with optional indicator filtering.
     * 
     * Features:
     * - Flexible date range (up to 90 days maximum)
     * - Optional indicator filtering (air quality or weather specific)
     * - Data completeness metrics (% of expected data points)
     * - Chronologically ordered data points
     * 
     * Performance: 500ms - 1s (depending on date range)
     * Response size: ~50-500 KB (depending on date range)
     *
     * @param inseeCode commune INSEE code (5-digit identifier)
     * @param startDate start date (ISO 8601 format: YYYY-MM-DD)
     * @param endDate end date (ISO 8601 format: YYYY-MM-DD)
     * @param indicators optional comma-separated indicator filter (e.g., "aqi,pm25,temperature")
     * @return HistoricalDataResponse with time-series data points
     */
    @GetMapping("/{inseeCode}/historical-data")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Get historical data for CSV generation",
            description = "Retrieves time-series data for air quality and weather over a date range. " +
                         "Supports flexible date ranges (up to 90 days) and optional indicator filtering.",
            parameters = {
                    @Parameter(
                            name = "inseeCode",
                            description = "5-digit INSEE code of the commune",
                            example = "75056",
                            required = true
                    ),
                    @Parameter(
                            name = "startDate",
                            description = "Start date in ISO 8601 format (YYYY-MM-DD)",
                            example = "2025-09-01",
                            required = true
                    ),
                    @Parameter(
                            name = "endDate",
                            description = "End date in ISO 8601 format (YYYY-MM-DD)",
                            example = "2025-10-09",
                            required = true
                    ),
                    @Parameter(
                            name = "indicators",
                            description = "Optional comma-separated indicator filter. Valid values: " +
                                         "aqi,pm25,pm10,no2,o3,so2,temperature,humidity,windSpeed,pressure. " +
                                         "If not specified, all indicators are returned.",
                            example = "aqi,pm25,temperature,humidity",
                            required = false
                    )
            }
    )
    @ApiResponse(
            responseCode = "200",
            description = "Historical data retrieved successfully",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = HistoricalDataResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Bad request - Invalid date range or parameters"
    )
    @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Authentication required"
    )
    @ApiResponse(
            responseCode = "404",
            description = "Commune not found"
    )
    public ResponseEntity<HistoricalDataResponse> getHistoricalData(
            @PathVariable String inseeCode,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,
            @RequestParam(required = false) String indicators
    ) {
        HistoricalDataResponse response = exportDataService.getHistoricalData(inseeCode, startDate, endDate, indicators);
        return ResponseEntity.ok(response);
    }
}
