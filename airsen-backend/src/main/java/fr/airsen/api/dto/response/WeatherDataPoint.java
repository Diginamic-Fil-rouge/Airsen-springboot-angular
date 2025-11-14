package fr.airsen.api.dto.response;

/**
 * Weather data point for historical time-series data.
 *
 * Contains comprehensive weather measurements at a specific point in time,
 * including temperature, humidity, wind, and advanced weather indicators.
 */
public record WeatherDataPoint(
    Double temperature,
    Integer humidity,
    Double windSpeed,
    Integer windDirection,
    Integer weatherCode,
    Double apparentTemperature,
    Double precipitation,
    Double rain,
    Double showers,
    Double snowfall,
    Integer cloudCover,
    Double windGusts,
    Double pressureMsl
) {}
