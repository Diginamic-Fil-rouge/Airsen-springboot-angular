package fr.airsen.api.controller;

import fr.airsen.api.entity.AirQuality;
import fr.airsen.api.service.AtmoDataService;
import fr.airsen.api.service.AtmoIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for testing ATMO API integration.
 * 
 * Provides endpoints to test API connectivity and data fetching
 * without requiring authentication.
 */
@RestController
@RequestMapping("/test/atmo")
public class AtmoTestController {

    private static final Logger log = LoggerFactory.getLogger(AtmoTestController.class);

    private final AtmoDataService atmoDataService;
    private final AtmoIntegrationService atmoIntegrationService;

    public AtmoTestController(AtmoDataService atmoDataService, AtmoIntegrationService atmoIntegrationService) {
        this.atmoDataService = atmoDataService;
        this.atmoIntegrationService = atmoIntegrationService;
    }

    /**
     * GET /test/atmo/connection
     * 
     * Tests the ATMO API connection.
     * 
     * @return ResponseEntity with connection status
     */
    @GetMapping("/connection")
    public Mono<ResponseEntity<Map<String, Object>>> testConnection() {
        log.info("Testing ATMO API connection");
        
        return atmoDataService.testApiConnection()
            .map(success -> {
                if (success) {
                    Map<String, Object> response = Map.of(
                        "status", "success",
                        "message", "ATMO API connection successful",
                        "connected", true
                    );
                    return ResponseEntity.ok(response);
                } else {
                    Map<String, Object> response = Map.of(
                        "status", "error",
                        "message", "ATMO API connection failed",
                        "connected", false
                    );
                    return ResponseEntity.status(503).body(response);
                }
            })
            .doOnError(error -> log.error("Connection test failed", error))
            .onErrorReturn(ResponseEntity.status(500).body(Map.of(
                "status", (Object) "error",
                "message", (Object) "Internal server error during connection test",
                "connected", (Object) false
            )));
    }

    /**
     * POST /test/atmo/fetch-all
     * 
     * Fetches and stores current air quality data for all communes.
     * 
     * @return ResponseEntity with operation results
     */
    @PostMapping("/fetch-all")
    public Mono<ResponseEntity<Map<String, Object>>> fetchAllAirQuality() {
        log.info("Starting fetch all air quality data operation");
        
        return atmoDataService.fetchAndStoreCurrentAirQuality()
            .map(count -> {
                Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "Air quality data fetched and stored successfully",
                    "recordsStored", count,
                    "todayTotal", atmoDataService.getTodayAirQualityCount()
                );
                return ResponseEntity.ok(response);
            })
            .doOnError(error -> log.error("Fetch all operation failed", error))
            .onErrorReturn(ResponseEntity.status(500).body(Map.of(
                "status", (Object) "error",
                "message", (Object) "Failed to fetch air quality data",
                "recordsStored", (Object) 0
            )));
    }

    /**
     * POST /test/atmo/fetch/{communeCode}
     * 
     * Fetches and stores air quality data for a specific commune.
     * 
     * @param communeCode INSEE code of the commune
     * @return ResponseEntity with operation results
     */
    @PostMapping("/fetch/{communeCode}")
    public Mono<ResponseEntity<Map<String, Object>>> fetchAirQualityForCommune(
            @PathVariable String communeCode) {
        log.info("Fetching air quality data for commune: {}", communeCode);
        
        return atmoDataService.fetchAndStoreAirQualityForCommune(communeCode)
            .map(airQuality -> {
                Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "Air quality data fetched and stored for commune " + communeCode,
                    "commune", airQuality.getCommune().getName(),
                    "atmoIndex", airQuality.getAtmoIndex(),
                    "qualifier", airQuality.getQualifier(),
                    "measurementDate", airQuality.getMeasurementDate()
                );
                return ResponseEntity.ok(response);
            })
            .doOnError(error -> log.error("Fetch operation failed for commune: {}", communeCode, error))
            .onErrorReturn(ResponseEntity.status(500).body(Map.of(
                "status", (Object) "error",
                "message", (Object) ("Failed to fetch air quality data for commune " + communeCode),
                "communeCode", (Object) communeCode
            )));
    }

    /**
     * GET /test/atmo/stats
     * 
     * Gets statistics about stored air quality data.
     * 
     * @return ResponseEntity with statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long todayCount = atmoDataService.getTodayAirQualityCount();
        
        return ResponseEntity.ok(Map.of(
            "status", "success",
            "message", "Air quality data statistics",
            "todayRecords", todayCount,
            "hasDataToday", todayCount > 0
        ));
    }

    /**
     * POST /test/atmo/production-sync
     * 
     * Tests the production ATMO integration service.
     * 
     * @return ResponseEntity with sync results
     */
    @PostMapping("/production-sync")
    public Mono<ResponseEntity<Map<String, Object>>> testProductionSync() {
        log.info("Testing production ATMO integration service");
        
        return atmoIntegrationService.syncCurrentAirQualityData()
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Production ATMO sync completed successfully");
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
                "message", "Production ATMO sync failed",
                "timestamp", LocalDateTime.now()
            )));
    }

    /**
     * GET /test/atmo/production-stats
     * 
     * Gets production ATMO integration statistics.
     * 
     * @return ResponseEntity with statistics
     */
    @GetMapping("/production-stats")
    public ResponseEntity<Map<String, Object>> getProductionStats() {
        AtmoIntegrationService.AirQualityStats stats = atmoIntegrationService.getTodayStats();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("service", "Production ATMO Integration");
        response.put("date", stats.date());
        response.put("recordsToday", stats.recordsToday());
        response.put("totalCommunes", stats.totalCommunes());
        response.put("coveragePercentage", stats.coveragePercentage());
        response.put("coverageText", String.format("%.1f%% (%d/%d)", 
            stats.coveragePercentage(), stats.recordsToday(), stats.totalCommunes()));
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
}