package fr.airsen.api.controller;

import fr.airsen.api.dto.WeatherDataDTO;
import fr.airsen.api.dto.WeatherUpdateResponse;
import fr.airsen.api.entity.WeatherData;
import fr.airsen.api.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
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
     * @param communeInseeCode INSEE code of the commune
     * @return ResponseEntity with current weather data
     */
    @GetMapping("/current/{communeInseeCode}")
    @Operation(
        summary = "Get current weather data",
        description = "Retrieves current weather data for a commune from database or fetches from Open-Meteo API if needed"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Weather data retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Commune not found"),
        @ApiResponse(responseCode = "500", description = "External API error or server error")
    })
    public ResponseEntity<WeatherDataDTO> getCurrentWeather(
            @Parameter(description = "INSEE code of the commune", example = "75101")
            @PathVariable
            @NotBlank(message = "Commune INSEE code cannot be blank")
            @Pattern(regexp = "\\d{5}", message = "INSEE code must be 5 digits")
            String communeInseeCode) {

        log.info("Fetching current weather for commune: {}", communeInseeCode);

        try {
            // Add timeout to prevent blocking indefinitely
            WeatherData weatherData = weatherService.getCurrentWeatherForCommune(communeInseeCode)
                .block(java.time.Duration.ofSeconds(15)); // 15 second timeout

            if (weatherData != null) {
                // Check if commune relationship is loaded to avoid LazyInitializationException
                if (weatherData.getCommune() == null) {
                    log.error("Weather data commune relationship not loaded for: {}", communeInseeCode);
                    return ResponseEntity.internalServerError().build();
                }

                WeatherDataDTO dto = mapToDTO(weatherData);
                log.info("Successfully fetched current weather for commune: {} - Temperature: {}°C",
                        communeInseeCode, weatherData.getTemperature());
                return ResponseEntity.ok(dto);
            } else {
                log.warn("No weather data found for commune: {} - Check if commune exists and has coordinates",
                        communeInseeCode);
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalStateException error) {
            // Handle specific error when commune has no coordinates
            log.error("Commune {} configuration error: {}", communeInseeCode, error.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception error) {
            log.error("Failed to fetch current weather for commune: {} - Error: {}",
                    communeInseeCode, error.getMessage(), error);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Update weather data for a commune by fetching from Open-Meteo API.
     * 
     * @param communeInseeCode INSEE code of the commune
     * @return ResponseEntity with update response
     */
    @PostMapping("/update/{communeInseeCode}")
    @Operation(
        summary = "Update weather data",
        description = "Fetches fresh weather data from Open-Meteo API and saves to database"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Weather data updated successfully"),
        @ApiResponse(responseCode = "404", description = "Commune not found"),
        @ApiResponse(responseCode = "500", description = "External API error or server error")
    })
    public ResponseEntity<WeatherUpdateResponse> updateWeatherData(
            @Parameter(description = "INSEE code of the commune", example = "75101")
            @PathVariable 
            @NotBlank(message = "Commune INSEE code cannot be blank")
            @Pattern(regexp = "\\d{5}", message = "INSEE code must be 5 digits")
            String communeInseeCode) {
        
        log.info("Updating weather data for commune: {}", communeInseeCode);
        
        try {
            // Convert reactive to synchronous to fix authentication context issue
            WeatherData weatherData = weatherService.forceUpdateWeatherForCommune(communeInseeCode)
                .block(java.time.Duration.ofSeconds(10)); // 10 second timeout
            
            if (weatherData != null) {
                WeatherDataDTO dto = mapToDTO(weatherData);
                WeatherUpdateResponse response = WeatherUpdateResponse.success(communeInseeCode, dto);
                log.info("Successfully updated weather data for commune: {}", communeInseeCode);
                return ResponseEntity.ok(response);
            } else {
                log.warn("No weather data returned for commune: {}", communeInseeCode);
                WeatherUpdateResponse response = WeatherUpdateResponse.failure(
                    communeInseeCode, 
                    "No weather data returned from service"
                );
                return ResponseEntity.ok(response);
            }
            
        } catch (Exception error) {
            log.error("Failed to update weather data for commune: {}", communeInseeCode, error);
            WeatherUpdateResponse response = WeatherUpdateResponse.failure(
                communeInseeCode, 
                "Failed to update weather data: " + error.getMessage()
            );
            return ResponseEntity.ok(response);
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
     * Get weather data for all communes in a region.
     * 
     * @param regionCode INSEE region code
     * @return Flux of weather data for the region
     */
    @GetMapping("/region/{regionCode}")
    @Operation(
        summary = "Get weather data for region",
        description = "Retrieves current weather data for all communes in a region"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Region weather data retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Region not found")
    })
    public ResponseEntity<List<WeatherDataDTO>> getRegionWeatherData(
            @Parameter(description = "INSEE region code", example = "11")
            @PathVariable 
            @NotBlank(message = "Region code cannot be blank")
            @Pattern(regexp = "\\d{1,2}", message = "Region code must be 1-2 digits")
            String regionCode) {
        
        log.info("Fetching weather data for region: {}", regionCode);
        
        try {
            // Convert reactive to synchronous to fix authentication context issue
            List<WeatherDataDTO> weatherData = weatherService.getRegionWeatherData(regionCode)
                .map(this::mapToDTO)
                .collectList()
                .block(java.time.Duration.ofSeconds(10)); // 10 second timeout
            
            log.info("Successfully retrieved weather data for region: {}", regionCode);
            return ResponseEntity.ok(weatherData != null ? weatherData : List.of());
            
        } catch (Exception error) {
            log.error("Error in region weather data for region: {}", regionCode, error);
            return ResponseEntity.internalServerError().build();
        }
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
            weatherData.getCreatedAt()
        );
    }
}