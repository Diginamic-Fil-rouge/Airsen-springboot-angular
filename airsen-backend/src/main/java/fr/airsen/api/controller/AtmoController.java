package fr.airsen.api.controller;

import fr.airsen.api.dto.AirQualityResponseDTO;
import fr.airsen.api.dto.response.AirQualityResponse;
import fr.airsen.api.dto.response.ErrorResponse;
import fr.airsen.api.exception.AirQualityDataNotFoundException;
import fr.airsen.api.exception.ResourceNotFoundException;
import fr.airsen.api.service.AtmoIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * REST controller for ATMO France air quality data integration.
 *
 * Provides endpoints for accessing current air quality data,
 * triggering data synchronization, and monitoring integration status.
 */
@RestController
@RequestMapping("/atmo")
@Tag(name = "ATMO Air Quality", description = "ATMO France air quality data integration")
@SecurityRequirement(name = "bearerAuth")
public class AtmoController {

    private static final Logger log = LoggerFactory.getLogger(AtmoController.class);

    private final AtmoIntegrationService atmoIntegrationService;

    public AtmoController(AtmoIntegrationService atmoIntegrationService) {
        this.atmoIntegrationService = atmoIntegrationService;
    }

    /**
     * GET /atmo/air-quality/{inseeCode}
     *
     * Retrieves current air quality data for a specific commune.
     *
     * @param inseeCode INSEE code of the commune
     * @return ResponseEntity with air quality data
     */
    @GetMapping("/air-quality/{inseeCode}")
    @Operation(
        summary = "Get air quality for a commune",
        description = """
            Returns current air quality data from the local database.
            If no direct data is available, estimates from the nearest commune within 20km.
            Returns 404 if no data is available within 20km radius (PRD requirement).

            Data Sources:
            - DIRECT: Measured data for the requested commune
            - ESTIMATED: Estimated from nearest commune (includes distance metadata)
            - NOT_AVAILABLE: No data within 20km
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Air quality data found (direct or estimated)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AirQualityResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Direct Data",
                        value = """
                            {
                              \"inseeCode\": \"75056\",
                              \"communeName\": \"Paris\",
                              \"measurementDate\": \"2025-11-04\",
                              \"atmoIndex\": 2,
                              \"qualifier\": \"Moyen\",
                              \"color\": \"#50ccaa\",
                              \"pollutants\": {\"NO2\": 40, \"O3\": 120, \"PM10\": 40, \"PM25\": 20, \"SO2\": 100},
                              \"dataSource\": \"DIRECT\",
                              \"dataQualityNote\": \"Données mesurées pour cette commune\"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Estimated Data",
                        value = """
                            {
                              \"inseeCode\": \"75057\",
                              \"communeName\": \"Versailles\",
                              \"measurementDate\": \"2025-11-04\",
                              \"atmoIndex\": 3,
                              \"qualifier\": \"Dégradé\",
                              \"color\": \"#ffcc00\",
                              \"pollutants\": {\"NO2\": 90, \"O3\": 160, \"PM10\": 50, \"PM25\": 25, \"SO2\": 200},
                              \"dataSource\": \"ESTIMATED\",
                              \"estimatedFromCommune\": \"Paris\",
                              \"distanceKm\": 17.3,
                              \"dataQualityNote\": \"Données estimées depuis Paris (17.3 km)\"
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Commune not found or no air quality data within 20km",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<AirQualityResponse> getAirQuality(
        @PathVariable @Parameter(description = "INSEE code of the commune") String inseeCode
    ) {
        log.info("REST request to get air quality for commune: {}", inseeCode);

        try {
            Optional<AirQualityResponseDTO> optionalData = atmoIntegrationService.getAirQualityForCommuneSync(inseeCode);

            if (optionalData.isEmpty()) {
                throw new AirQualityDataNotFoundException("No air quality data available for commune: " + inseeCode);
            }

            AirQualityResponseDTO dto = optionalData.get();
            AirQualityResponse response = convertToAirQualityResponse(dto);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            log.error("Commune not found: {}", inseeCode);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (AirQualityDataNotFoundException e) {
            log.warn("No air quality data available: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * POST /atmo/sync
     *
     * Triggers manual synchronization of ATMO air quality data.
     *
     * @return ResponseEntity with synchronization results
     */
    @PostMapping("/sync")
    @Operation(summary = "Trigger data synchronization",
               description = "Manually triggers synchronization of air quality data from ATMO API")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Synchronization completed successfully"),
        @ApiResponse(responseCode = "500", description = "Synchronization failed")
    })
    public ResponseEntity<Map<String, Object>> triggerSync() {
        log.info("Manual ATMO data synchronization triggered");

        try {
            // Convert to synchronous operation
            Integer count = atmoIntegrationService.syncCurrentAirQualityData()
                .block(java.time.Duration.ofMinutes(2)); // 2 minute timeout for sync

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "ATMO data synchronization completed");
            response.put("recordsProcessed", count != null ? count : 0);
            response.put("timestamp", LocalDateTime.now());

            AtmoIntegrationService.AirQualityStats stats = atmoIntegrationService.getTodayStats();
            response.put("stats", Map.of(
                "recordsToday", stats.recordsToday(),
                "totalCommunes", stats.totalCommunes(),
                "coverage", String.format("%.1f%%", stats.coveragePercentage())
            ));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("ATMO data synchronization failed", e);
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "ATMO data synchronization failed",
                "timestamp", LocalDateTime.now(),
                "error", e.getMessage()
            ));
        }
    }

    /**
     * GET /atmo/stats
     *
     * Retrieves statistics about ATMO data integration.
     *
     * @return ResponseEntity with integration statistics
     */
    @GetMapping("/stats")
    @Operation(summary = "Get integration statistics",
               description = "Retrieves statistics about ATMO data integration and coverage")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.debug("Retrieving ATMO integration statistics");

        AtmoIntegrationService.AirQualityStats stats = atmoIntegrationService.getTodayStats();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("date", stats.date());
        response.put("recordsToday", stats.recordsToday());
        response.put("totalCommunes", stats.totalCommunes());
        response.put("coveragePercentage", stats.coveragePercentage());
        response.put("coverageText", String.format("%.1f%% (%d/%d)",
            stats.coveragePercentage(), stats.recordsToday(), stats.totalCommunes()));
        response.put("lastUpdated", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /atmo/health
     *
     * Health check endpoint for ATMO integration.
     *
     * @return ResponseEntity with health status
     */
    @GetMapping("/health")
    @Operation(summary = "Health check",
               description = "Checks the health status of ATMO integration")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            AtmoIntegrationService.AirQualityStats stats = atmoIntegrationService.getTodayStats();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "healthy");
            response.put("service", "ATMO Integration");
            response.put("timestamp", LocalDateTime.now());
            response.put("dataAvailable", stats.recordsToday() > 0);
            response.put("coverage", String.format("%.1f%%", stats.coveragePercentage()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Health check failed", e);

            Map<String, Object> response = Map.of(
                "status", "unhealthy",
                "service", "ATMO Integration",
                "timestamp", LocalDateTime.now(),
                "error", e.getMessage()
            );

            return ResponseEntity.status(503).body(response);
        }
    }

    /**
     * Creates a standardized air quality response map from DTO.
     *
     * @param airQuality the air quality DTO
     * @return formatted response map
     */
    private Map<String, Object> createAirQualityResponse(AirQualityResponseDTO airQuality) {
        Map<String, Object> response = new HashMap<>();

        response.put("status", "success");
        response.put("commune", Map.of(
            "inseeCode", airQuality.communeInseeCode(),
            "name", airQuality.communeName(),
            "department", airQuality.departmentName(),
            "region", airQuality.regionName()
        ));

        response.put("airQuality", Map.of(
            "measurementDate", airQuality.measurementDate(),
            "atmoIndex", airQuality.atmoIndex(),
            "qualifier", airQuality.qualifier(),
            "color", airQuality.color(),
            "pollutants", Map.of(
                "NO2", airQuality.no2Concentration(),
                "O3", airQuality.o3Concentration(),
                "PM10", airQuality.pm10Concentration(),
                "PM2_5", airQuality.pm25Concentration(),
                "SO2", airQuality.so2Concentration()
            )
        ));

        response.put("lastUpdated", airQuality.createdAt());

        return response;
    }

    /**
     * Helper method to convert AirQualityResponseDTO to AirQualityResponse.
     * This is a temporary conversion until the service layer is refactored.
     *
     * @param dto The DTO from the service layer
     * @return AirQualityResponse for the controller response
     */
    private AirQualityResponse convertToAirQualityResponse(AirQualityResponseDTO dto) {
        Map<String, Integer> pollutants = new HashMap<>();
        pollutants.put("NO2", dto.no2Concentration());
        pollutants.put("O3", dto.o3Concentration());
        pollutants.put("PM10", dto.pm10Concentration());
        pollutants.put("PM25", dto.pm25Concentration());
        pollutants.put("SO2", dto.so2Concentration());

        return new AirQualityResponse(
            dto.communeInseeCode(),
            dto.communeName(),
            dto.measurementDate(),
            dto.atmoIndex(),
            dto.qualifier(),
            dto.color(),
            pollutants,
            AirQualityResponse.DataSource.DIRECT, // Default to DIRECT for now
            null, // No estimated commune
            null, // No distance
            "Données mesurées pour cette commune" // Default quality note
        );
    }
}
