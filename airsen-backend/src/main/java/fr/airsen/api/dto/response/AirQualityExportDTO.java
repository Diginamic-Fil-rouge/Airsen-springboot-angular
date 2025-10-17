package fr.airsen.api.dto.response;

import java.time.LocalDateTime;

/**
 * Air quality export information.
 * 
 * Contains latest air quality measurements including ATMO index and pollutant concentrations.
 */
public record AirQualityExportDTO(
    LocalDateTime measurementDate,
    Integer atmIndex,
    String atmoQual,
    String atmoColor,
    Double no2,
    Double o3,
    Double pm10,
    Integer pm25,
    Double so2,
    LocalDateTime createdAt
) {}
