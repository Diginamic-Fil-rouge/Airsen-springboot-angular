package fr.airsen.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Response DTO for weather data update operations.
 * 
 * Indicates the result of weather data fetching and database insertion.
 */
public record WeatherUpdateResponse(
    @NotBlank(message = "Commune INSEE code cannot be blank")
    String communeInseeCode,
    
    @NotNull(message = "Success status cannot be null")
    Boolean success,
    
    String message,
    
    WeatherDataDTO weatherData,
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt,
    
    String source
) {
    
    public static WeatherUpdateResponse success(String communeInseeCode, WeatherDataDTO weatherData) {
        return new WeatherUpdateResponse(
            communeInseeCode,
            true,
            "Weather data successfully updated",
            weatherData,
            LocalDateTime.now(),
            "Open-Meteo API"
        );
    }
    
    public static WeatherUpdateResponse failure(String communeInseeCode, String errorMessage) {
        return new WeatherUpdateResponse(
            communeInseeCode,
            false,
            errorMessage,
            null,
            LocalDateTime.now(),
            "Open-Meteo API"
        );
    }
}