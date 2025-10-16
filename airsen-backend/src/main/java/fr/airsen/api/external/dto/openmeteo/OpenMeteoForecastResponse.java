package fr.airsen.api.external.dto.openmeteo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for Open-Meteo weather forecast data.
 * 
 * Maps daily forecast data for weather predictions.
 */
public record OpenMeteoForecastResponse(
    @JsonProperty("timezone") String timezone,
    @JsonProperty("daily") DailyForecast daily
) {
    
    /**
     * Nested record for daily forecast data.
     */
    public record DailyForecast(
        @JsonProperty("time") List<LocalDate> dates,
        @JsonProperty("temperature_2m_max") List<Double> maxTemperatures,
        @JsonProperty("temperature_2m_min") List<Double> minTemperatures,
        @JsonProperty("precipitation_sum") List<Double> precipitationSum,
        @JsonProperty("wind_speed_10m_max") List<Double> maxWindSpeed,
        @JsonProperty("weather_code") List<Integer> weatherCodes
    ) {}
}