package fr.airsen.api.dto.response;

import java.time.LocalDateTime;

/**
 * Weather export information.
 *
 * Contains comprehensive weather measurements including temperature, humidity, wind,
 * and advanced weather indicators (precipitation, cloud cover, pressure, etc.)
 */
public record WeatherExportDTO(
    LocalDateTime observationDate,
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
    Double pressureMsl,
    LocalDateTime createdAt
) {}
