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
    Double humidity,
    Double windSpeed,
    Double windDirection,
    Integer weatherCode,
    LocalDateTime createdAt
) {}
