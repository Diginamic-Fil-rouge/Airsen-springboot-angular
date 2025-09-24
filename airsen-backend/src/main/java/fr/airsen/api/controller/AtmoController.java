package fr.airsen.api.controller;

import fr.airsen.api.dto.AirQualityResponseDTO;
import fr.airsen.api.service.AtmoIntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

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
    @Transactional(readOnly = true)
    @Operation(summary = "Get air quality for commune", 
               description = "Retrieves current air quality data for a commune by INSEE code")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Air quality data retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Commune not found or no data available"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public Mono<ResponseEntity<Map<String, Object>>> getAirQuality(
            @Parameter(description = "INSEE code of the commune", example = "75056")
            @PathVariable String inseeCode) {
        
        log.error(">>> CLAUDE DEBUG: AtmoController.getAirQuality called for: {}", inseeCode);
        
        return atmoIntegrationService.getAirQualityForCommune(inseeCode)
            .map(airQualityOpt -> {
                if (airQualityOpt.isPresent()) {
                    AirQualityResponseDTO airQuality = airQualityOpt.get();
                    Map<String, Object> response = createAirQualityResponse(airQuality);
                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> response = Map.of(
                        "status", "not_found",
                        "message", "No air quality data available for commune " + inseeCode,
                        "inseeCode", inseeCode
                    );
                    return ResponseEntity.status(404).body(response);
                }
            })
            .onErrorResume(error -> {
                log.error("Error retrieving air quality data for commune: {}", inseeCode, error);
                Map<String, Object> errorResponse = Map.of(
                    "status", "error",
                    "message", "Failed to retrieve air quality data",
                    "inseeCode", inseeCode,
                    "error", error.getMessage()
                );
                return Mono.just(ResponseEntity.status(500).body(errorResponse));
            });
    }

    /**
     * GET /atmo/air-quality/{inseeCode}/latest
     * 
     * Retrieves the latest stored air quality data for a commune.
     * 
     * @param inseeCode INSEE code of the commune
     * @return ResponseEntity with latest stored air quality data
     */
    @GetMapping("/air-quality/{inseeCode}/latest")
    @Transactional(readOnly = true)
    @Operation(summary = "Get latest stored air quality", 
               description = "Retrieves the most recent stored air quality data for a commune")
    public ResponseEntity<Map<String, Object>> getLatestAirQuality(
            @Parameter(description = "INSEE code of the commune")
            @PathVariable String inseeCode) {
        
        log.debug("Retrieving latest stored air quality for commune: {}", inseeCode);
        
        Optional<AirQualityResponseDTO> airQualityOpt = atmoIntegrationService.getLatestStoredAirQuality(inseeCode);
        
        if (airQualityOpt.isPresent()) {
            AirQualityResponseDTO airQuality = airQualityOpt.get();
            Map<String, Object> response = createAirQualityResponse(airQuality);
            response.put("source", "database");
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = Map.of(
                "status", "not_found",
                "message", "No stored air quality data found for commune " + inseeCode,
                "inseeCode", inseeCode
            );
            return ResponseEntity.status(404).body(response);
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
    public Mono<ResponseEntity<Map<String, Object>>> triggerSync() {
        log.info("Manual ATMO data synchronization triggered");
        
        return atmoIntegrationService.syncCurrentAirQualityData()
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "ATMO data synchronization completed");
                response.put("recordsProcessed", count);
                response.put("timestamp", LocalDateTime.now());
                
                AtmoIntegrationService.AirQualityStats stats = atmoIntegrationService.getTodayStats();
                response.put("stats", Map.of(
                    "recordsToday", stats.recordsToday(),
                    "totalCommunes", stats.totalCommunes(),
                    "coverage", String.format("%.1f%%", stats.coveragePercentage())
                ));
                
                return ResponseEntity.ok(response);
            })
            .onErrorReturn(ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "ATMO data synchronization failed",
                "timestamp", LocalDateTime.now()
            )));
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
                "PM2.5", airQuality.pm25Concentration(),
                "SO2", airQuality.so2Concentration()
            )
        ));
        
        response.put("lastUpdated", airQuality.createdAt());
        
        return response;
    }
}