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
    Integer no2,
    Integer o3,
    Integer pm10,
    Integer pm25,
    Integer so2,
    LocalDateTime createdAt
) {}
