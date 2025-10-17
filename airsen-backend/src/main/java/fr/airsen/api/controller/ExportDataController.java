package fr.airsen.api.controller;

import fr.airsen.api.dto.response.ExportDataResponse;
import fr.airsen.api.dto.response.HistoricalDataResponse;
import fr.airsen.api.service.ExportDataService;
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
 * Provides optimized data aggregation endpoints for client-side PDF/CSV export generation.
 * 
 * Two main endpoints:
 * 1. GET /communes/{inseeCode}/export-data - Current snapshot for PDF export
 * 2. GET /communes/{inseeCode}/historical-data - Time-series for CSV export
 * 
 * Both endpoints require authentication with USER or ADMIN role.
 */
@RestController
@RequestMapping("/api/v1/communes")
@Tag(name = "Export Data", description = "Endpoints for data export (client-side generation)")
@SecurityRequirement(name = "Bearer Authentication")
public class ExportDataController {

    private final ExportDataService exportDataService;

    @Autowired
    public ExportDataController(ExportDataService exportDataService) {
        this.exportDataService = exportDataService;
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
     * Performance: < 200ms
     * Response size: ~2-5 KB
     * 
     * @param inseeCode commune INSEE code (5-digit identifier)
     * @return ExportDataResponse with complete current commune state
     */
    @GetMapping("/{inseeCode}/export-data")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(
            summary = "Get export data for PDF generation",
            description = "Retrieves all data needed for PDF export in a single API call. " +
                         "Includes commune info, latest air quality, latest weather, and data quality metadata.",
            parameters = {
                    @Parameter(
                            name = "inseeCode",
                            description = "5-digit INSEE code of the commune",
                            example = "75056",
                            required = true
                    )
            }
    )
    @ApiResponse(
            responseCode = "200",
            description = "Export data retrieved successfully",
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
            @PathVariable String inseeCode
    ) {
        ExportDataResponse response = exportDataService.getExportData(inseeCode);
        return ResponseEntity.ok(response);
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
     * Example requests:
     * - GET /communes/75056/historical-data?startDate=2025-09-01&endDate=2025-10-09
     * - GET /communes/75056/historical-data?startDate=2025-09-01&endDate=2025-09-30&indicators=aqi,pm25,temperature
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
