package fr.airsen.api.dto.response;

import java.time.LocalDate;

/**
 * DTO representing air quality data from the nearest commune within geodistance threshold.
 * Used when target commune has no direct air quality data.
 *
 * @param inseeCode INSEE code of the nearest commune (not the target commune)
 * @param communeName Name of the nearest commune
 * @param latitude Latitude of the nearest commune
 * @param longitude Longitude of the nearest commune
 * @param measurementDate Date of the air quality measurement
 * @param atmoIndex ATMO air quality index (1-6)
 * @param qualifier ATMO qualifier (Bon, Moyen, Dégradé, etc.)
 * @param color Hex color code for visualization
 * @param no2 NO2 concentration (μg/m³)
 * @param o3 O3 concentration (μg/m³)
 * @param pm10 PM10 concentration (μg/m³)
 * @param pm25 PM2.5 concentration (μg/m³)
 * @param so2 SO2 concentration (μg/m³)
 * @param distanceKm Distance from target commune to this nearest commune
 */
public record NearestAirQualityResult(
    String inseeCode,
    String communeName,
    Double latitude,
    Double longitude,
    LocalDate measurementDate,
    Integer atmoIndex,
    String qualifier,
    String color,
    Integer no2,
    Integer o3,
    Integer pm10,
    Integer pm25,
    Integer so2,
    Double distanceKm
) {
    /**
     * Validate that distance is within PRD-defined threshold and ATMO index is valid.
     */
    public NearestAirQualityResult {
        if (distanceKm == null || distanceKm < 0) {
            throw new IllegalArgumentException("Distance must be non-negative");
        }
        if (distanceKm > 20.0) {
            throw new IllegalArgumentException(
                String.format("Distance %.2f km exceeds PRD threshold of 20km", distanceKm));
        }
        if (atmoIndex != null && (atmoIndex < 1 || atmoIndex > 6)) {
            throw new IllegalArgumentException(
                String.format("ATMO index %d must be between 1 and 6", atmoIndex));
        }
    }
}
