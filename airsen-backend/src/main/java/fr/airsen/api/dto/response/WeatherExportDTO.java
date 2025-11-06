package fr.airsen.api.dto.response;

import java.time.LocalDateTime;

/**
 * Weather export information.
 * 
 * Contains latest weather measurements including temperature, humidity, wind speed, and conditions.
 */
public record WeatherExportDTO(
    LocalDateTime observationDate,
    Double temperature,
    Integer humidity,
    Double windSpeed,
    Integer windDirection,
    Integer weatherCode,
    LocalDateTime createdAt
) {}
