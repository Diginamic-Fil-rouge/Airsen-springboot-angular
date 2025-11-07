package fr.airsen.api.controller;

import fr.airsen.api.dto.WeatherDataDTO;
import fr.airsen.api.dto.WeatherUpdateResponse;
import fr.airsen.api.dto.response.WeatherResponse;
import fr.airsen.api.entity.WeatherData;
import fr.airsen.api.exception.ResourceNotFoundException;
import fr.airsen.api.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for weather data management.
 *
 * Provides endpoints for fetching weather data from Open-Meteo API
 * and managing weather information in the database.
 */
@RestController
@RequestMapping("/weather")
@Tag(name = "Weather Data", description = "Weather data management with Open-Meteo API integration")
@SecurityRequirement(name = "bearerAuth")
@Validated
public class WeatherController {

    private static final Logger log = LoggerFactory.getLogger(WeatherController.class);

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * Get current weather data for a commune.
     *
     * @param inseeCode INSEE code of the commune
     * @return ResponseEntity with current weather data
     */
    @GetMapping("/current/{inseeCode}")
    @Operation(
        summary = "Get current weather for a commune",
        description = """
            Returns current weather data from the local database.
            If no direct data is available, estimates from the nearest commune within 20km.
            Returns 404 if no data is available within 20km radius (PRD requirement).

            Data Sources:
            - DIRECT: Measured data for the requested commune
            - ESTIMATED: Estimated from nearest commune (includes distance metadata)
            - NOT_AVAILABLE: No data within 20km
            """,
        tags = {"Weather Data"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Weather data found (direct or estimated)",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = WeatherResponse.class),
                examples = {
                    @ExampleObject(
                        name = "Direct Data",
                        value = """
                            {
                              \"inseeCode\": \"75056\",
                              \"communeName\": \"Paris\",
                              \"measurementDate\": \"2025-11-04\",
                              \"temperature\": 15.5,
                              \"humidity\": 62,
                              \"windSpeed\": 12.3,
                              \"windDirection\": 210,
                              \"weatherCode\": 3,
                              \"weatherDescription\": \"Overcast\",
                              \"apparentTemperature\": 14.2,
                              \"precipitation\": 0.2,
                              \"rain\": 0.1,
                              \"showers\": 0.0,
                              \"snowfall\": 0.0,
                              \"cloudCover\": 85,
                              \"windGusts\": 22.5,
                              \"pressureMsl\": 1014.3,
                              \"dataSource\": \"DIRECT\",
                              \"dataQualityNote\": \"Données mesurées pour cette commune\"
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Estimated Data",
                        value = """
                            {
                              \"inseeCode\": \"78646\",
                              \"communeName\": \"Vélizy-Villacoublay\",
                              \"measurementDate\": \"2025-11-04\",
                              \"temperature\": 15.2,
                              \"humidity\": 60,
                              \"windSpeed\": 13.1,
                              \"windDirection\": 205,
                              \"weatherCode\": 2,
                              \"weatherDescription\": \"Partly cloudy\",
                              \"apparentTemperature\": 14.8,
                              \"precipitation\": 0.0,
                              \"rain\": 0.0,
                              \"showers\": 0.0,
                              \"snowfall\": 0.0,
                              \"cloudCover\": 50,
                              \"windGusts\": 20.0,
                              \"pressureMsl\": 1013.8,
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
            description = "Commune not found or no weather data within 20km",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = fr.airsen.api.dto.response.ErrorResponse.class)
            )
        )
    })
    public ResponseEntity<WeatherResponse> getCurrentWeather(
        @PathVariable @Parameter(description = "INSEE code of the commune") String inseeCode
    ) {
        log.info("REST request to get current weather for commune: {}", inseeCode);

        try {
            // Service now returns WeatherResponse directly with smart caching and geodistance fallback
            WeatherResponse response = weatherService.getCurrentWeatherForCommune(inseeCode);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            log.error("Commune not found: {}", inseeCode);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    /**
     * Manually triggers synchronization of weather data from Open-Meteo API for all communes.
     *
     * This endpoint performs a full synchronization of weather data, similar to the scheduled daily sync.
     * It updates weather information for all communes in the database by fetching fresh data
     * from the Open-Meteo API.
     *
     * @return ResponseEntity with synchronization summary
     */
    @PostMapping("/update")
    @Operation(
        summary = "Manually trigger full weather data synchronization",
        description = """
            Triggers manual synchronization of weather data from Open-Meteo API for all communes.

            This operation:
            - Fetches fresh weather data for all communes from Open-Meteo API
            - Updates the database with current weather information
            - Publishes cache eviction events after successful transaction commit
            - Returns summary of successful and failed updates

            Note: This is a long-running operation that may take several minutes depending on
            the number of communes in the database.
            """
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Weather data synchronization completed (check response for success/failure details)",
            content = @Content(
                mediaType = "application/json",
                examples = {
                    @ExampleObject(
                        name = "Successful Sync",
                        value = """
                            {
                              "success": true,
                              "message": "Weather data synchronization completed successfully",
                              "communesUpdated": 35228,
                              "startTime": "2025-11-07T14:30:00",
                              "endTime": "2025-11-07T14:45:32",
                              "durationSeconds": 932
                            }
                            """
                    ),
                    @ExampleObject(
                        name = "Partial Sync",
                        value = """
                            {
                              "success": true,
                              "message": "Weather data synchronization completed with some failures",
                              "communesUpdated": 34500,
                              "communesFailed": 728,
                              "startTime": "2025-11-07T14:30:00",
                              "endTime": "2025-11-07T14:46:15",
                              "durationSeconds": 975
                            }
                            """
                    )
                }
            )
        ),
        @ApiResponse(responseCode = "500", description = "Synchronization failed due to system error")
    })
    public ResponseEntity<java.util.Map<String, Object>> synchronizeAllWeatherData() {
        java.time.LocalDateTime startTime = java.time.LocalDateTime.now();
        log.info("========================================");
        log.info("MANUAL WEATHER SYNC TRIGGERED at {}", startTime);
        log.info("========================================");

        try {
            int communesUpdated = weatherService.updateWeatherDataInTransaction();

            java.time.LocalDateTime endTime = java.time.LocalDateTime.now();
            java.time.Duration duration = java.time.Duration.between(startTime, endTime);

            log.info("========================================");
            log.info("MANUAL WEATHER SYNC COMPLETED at {}", endTime);
            log.info("Updated {} communes in {} seconds", communesUpdated, duration.getSeconds());
            log.info("========================================");

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", true);
            response.put("message", "Weather data synchronization completed successfully");
            response.put("communesUpdated", communesUpdated);
            response.put("startTime", startTime.toString());
            response.put("endTime", endTime.toString());
            response.put("durationSeconds", duration.getSeconds());

            return ResponseEntity.ok(response);

        } catch (Exception error) {
            java.time.LocalDateTime endTime = java.time.LocalDateTime.now();
            java.time.Duration duration = java.time.Duration.between(startTime, endTime);

            log.error("========================================");
            log.error("MANUAL WEATHER SYNC FAILED at {}", endTime);
            log.error("Duration before failure: {} seconds", duration.getSeconds());
            log.error("Error: {}", error.getMessage(), error);
            log.error("========================================");

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("success", false);
            response.put("message", "Weather data synchronization failed: " + error.getMessage());
            response.put("startTime", startTime.toString());
            response.put("endTime", endTime.toString());
            response.put("durationSeconds", duration.getSeconds());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Get weather forecast for a commune.
     *
     * @param communeInseeCode INSEE code of the commune
     * @param days number of forecast days (1-16)
     * @return ResponseEntity with forecast data
     */
    @GetMapping("/forecast/{communeInseeCode}")
    @Operation(
        summary = "Get weather forecast",
        description = "Retrieves weather forecast from Open-Meteo API for a commune"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Forecast data retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Commune not found"),
        @ApiResponse(responseCode = "500", description = "External API error or server error")
    })
    public ResponseEntity<List<WeatherDataDTO>> getWeatherForecast(
            @Parameter(description = "INSEE code of the commune", example = "75101")
            @PathVariable
            @NotBlank(message = "Commune INSEE code cannot be blank")
            @Pattern(regexp = "\\d{5}", message = "INSEE code must be 5 digits")
            String communeInseeCode,

            @Parameter(description = "Number of forecast days", example = "7")
            @RequestParam(defaultValue = "7")
            @Min(value = 1, message = "Forecast days must be at least 1")
            @Max(value = 16, message = "Forecast days must be at most 16")
            int days) {

        log.info("Fetching weather forecast for commune: {} for {} days", communeInseeCode, days);

        try {
            // Convert reactive to synchronous to fix authentication context issue
            List<WeatherDataDTO> forecastData = weatherService.getWeatherForecastForCommune(communeInseeCode, days)
                .map(this::mapToDTO)
                .collectList()
                .block(java.time.Duration.ofSeconds(15)); // 15 second timeout for forecast

            log.info("Successfully retrieved forecast for commune: {}", communeInseeCode);
            return ResponseEntity.ok(forecastData != null ? forecastData : List.of());

        } catch (Exception error) {
            log.error("Error in forecast for commune: {}", communeInseeCode, error);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get historical weather data for a commune.
     *
     * @param communeInseeCode INSEE code of the commune
     * @param startDate start date for historical data
     * @param endDate end date for historical data
     * @param page page number (0-based)
     * @param size page size
     * @return ResponseEntity with historical weather data
     */
    @GetMapping("/historical/{communeInseeCode}")
    @Operation(
        summary = "Get historical weather data",
        description = "Retrieves historical weather data from database for a commune within a date range"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Historical data retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid date range"),
        @ApiResponse(responseCode = "404", description = "Commune not found")
    })
    public ResponseEntity<List<WeatherDataDTO>> getHistoricalWeather(
            @Parameter(description = "INSEE code of the commune", example = "75101")
            @PathVariable
            @NotBlank(message = "Commune INSEE code cannot be blank")
            @Pattern(regexp = "\\d{5}", message = "INSEE code must be 5 digits")
            String communeInseeCode,

            @Parameter(description = "Start date", example = "2024-01-01")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @Parameter(description = "End date", example = "2024-01-31")
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {

        log.info("Fetching historical weather for commune: {} from {} to {}",
                communeInseeCode, startDate, endDate);

        try {
            // Convert reactive to synchronous to fix authentication context issue
            List<WeatherDataDTO> historicalData = weatherService.getHistoricalWeather(communeInseeCode, startDate, endDate)
                .map(this::mapToDTO)
                .collectList()
                .block(java.time.Duration.ofSeconds(20)); // 20 second timeout for historical data

            log.info("Successfully retrieved historical data for commune: {}", communeInseeCode);
            return ResponseEntity.ok(historicalData != null ? historicalData : List.of());

        } catch (Exception error) {
            log.error("Error in historical data for commune: {}", communeInseeCode, error);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Bulk update weather data for multiple communes.
     *
     * @param communeCodes list of INSEE codes
     * @return Flux of update responses
     */
    @PostMapping("/bulk-update")
    @Operation(
        summary = "Bulk update weather data",
        description = "Updates weather data for multiple communes from Open-Meteo API"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Bulk update initiated"),
        @ApiResponse(responseCode = "400", description = "Invalid commune codes")
    })
    public Flux<WeatherUpdateResponse> bulkUpdateWeatherData(
            @Parameter(description = "List of INSEE codes")
            @RequestBody List<String> communeCodes) {

        log.info("Starting bulk weather update for {} communes", communeCodes.size());

        return Flux.fromIterable(communeCodes)
            .filter(code -> code != null && code.matches("\\d{5}"))
            .flatMap(communeCode ->
                weatherService.forceUpdateWeatherForCommune(communeCode)
                    .map(weatherData -> {
                        WeatherDataDTO dto = mapToDTO(weatherData);
                        return WeatherUpdateResponse.success(communeCode, dto);
                    })
                    .onErrorResume(error -> {
                        log.warn("Failed to update weather for commune: {}", communeCode, error);
                        return Mono.just(WeatherUpdateResponse.failure(
                            communeCode,
                            "Update failed: " + error.getMessage()
                        ));
                    })
            )
            .doOnComplete(() -> log.info("Completed bulk weather update"));
    }

    /**
     * Maps WeatherData entity to WeatherDataDTO.
     *
     * @param weatherData the weather data entity
     * @return mapped DTO
     */
    private WeatherDataDTO mapToDTO(WeatherData weatherData) {
        return new WeatherDataDTO(
            weatherData.getId(),
            weatherData.getCommune().getId(),
            weatherData.getCommune().getName(),
            weatherData.getCommune().getInseeCode(),
            weatherData.getMeasurementDate(),
            weatherData.getTemperature(),
            weatherData.getHumidity(),
            weatherData.getWindSpeed(),
            weatherData.getWindDirection(),
            weatherData.getWeatherCode(),
            weatherData.getApparentTemperature(),
            weatherData.getPrecipitation(),
            weatherData.getRain(),
            weatherData.getShowers(),
            weatherData.getSnowfall(),
            weatherData.getCloudCover(),
            weatherData.getWindGusts(),
            weatherData.getPressureMsl(),
            weatherData.getCreatedAt()
        );
    }

    private WeatherResponse mapToWeatherResponse(WeatherData weatherData) {
        String description = weatherCodeToDescription(weatherData.getWeatherCode());
        return WeatherResponse.direct(
            weatherData.getCommune().getInseeCode(),
            weatherData.getCommune().getName(),
            weatherData.getMeasurementDate(),
            weatherData.getTemperature(),
            weatherData.getHumidity(),
            weatherData.getWindSpeed(),
            weatherData.getWindDirection(),
            weatherData.getWeatherCode(),
            description,
            weatherData.getApparentTemperature(),
            weatherData.getPrecipitation(),
            weatherData.getRain(),
            weatherData.getShowers(),
            weatherData.getSnowfall(),
            weatherData.getCloudCover(),
            weatherData.getWindGusts(),
            weatherData.getPressureMsl()
        );
    }

    private String weatherCodeToDescription(Integer weatherCode) {
        if (weatherCode == null) {
            return "Unknown";
        }
        return switch (weatherCode) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Foggy";
            case 51, 53, 55 -> "Drizzle";
            case 61, 63, 65 -> "Rain";
            case 71, 73, 75 -> "Snow";
            case 77 -> "Snow grains";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> "Weather code " + weatherCode;
        };
    }
}
