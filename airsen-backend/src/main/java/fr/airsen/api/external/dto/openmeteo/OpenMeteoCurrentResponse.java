package fr.airsen.api.external.dto.openmeteo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Response DTO for Open-Meteo current weather conditions.
 * 
 * Maps weather data including temperature, humidity, and wind conditions.
 * Coordinates are provided by the caller from INSEE API data.
 */
public record OpenMeteoCurrentResponse(
    @JsonProperty("timezone") String timezone,
    @JsonProperty("current") CurrentWeather current
) {
    
    /**
     * Nested record for current weather data.
     */
    public record CurrentWeather(
        @JsonProperty("time") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime time,
        @JsonProperty("temperature_2m") Double temperature,
        @JsonProperty("relative_humidity_2m") Integer humidity,
        @JsonProperty("wind_speed_10m") Double windSpeed,
        @JsonProperty("wind_direction_10m") Integer windDirection,
        @JsonProperty("weather_code") Integer weatherCode,
        @JsonProperty("precipitation") Double precipitation,
        @JsonProperty("pressure_msl") Double pressure,
        @JsonProperty("visibility") Double visibility
    ) {}
}