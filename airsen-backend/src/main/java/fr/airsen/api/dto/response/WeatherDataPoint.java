package fr.airsen.api.dto.response;

/**
 * Weather data point for historical time-series data.
 * 
 * Contains weather measurements at a specific point in time.
 */
public record WeatherDataPoint(
    Double temperature,
    Integer humidity,
    Double windSpeed,
    Integer windDirection,
    Integer weatherCode,
    Double precipitation,
    Integer cloudCover
) {}
