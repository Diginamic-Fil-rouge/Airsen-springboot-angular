package fr.airsen.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.util.Map;

/**
 * Enhanced air quality response DTO with data source transparency.
 *
 * Indicates whether air quality data comes from:
 * - DIRECT: Measured at the requested commune
 * - ESTIMATED: Estimated from nearest commune within 20km (geodistance fallback)
 * - NOT_AVAILABLE: No data available within threshold
 *
 * @param inseeCode INSEE code of the requested commune
 * @param communeName Name of the requested commune
 * @param measurementDate Date of the air quality measurement
 * @param atmoIndex ATMO air quality index (1-6: Bon, Moyen, Dégradé, Mauvais, Très Mauvais, Extrêmement Mauvais)
 * @param qualifier ATMO qualifier text (e.g., "Bon", "Moyen")
 * @param color Hex color code for visualization (#00FF00 for good, #FF0000 for bad)
 * @param pollutants Map of pollutant concentrations (NO2, O3, PM10, PM25, SO2) in μg/m³
 * @param dataSource Data source indicator (DIRECT, ESTIMATED, NOT_AVAILABLE)
 * @param estimatedFromCommune Name of nearest commune if data is estimated
 * @param distanceKm Distance in kilometers if data is estimated
 * @param dataQualityNote French user-facing message about data quality
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AirQualityResponse(
    String inseeCode,
    String communeName,
    LocalDate measurementDate,
    Integer atmoIndex,
    String qualifier,
    String color,
    Map<String, Integer> pollutants,
    DataSource dataSource,
    String estimatedFromCommune,
    Double distanceKm,
    String dataQualityNote
) {
    /**
     * Data source indicator for transparency.
     *
     * DIRECT: Data measured at the requested commune
     * ESTIMATED: Data estimated from nearest commune within 20km threshold
     * NOT_AVAILABLE: No data available within geographic threshold
     */
    public enum DataSource {
        DIRECT,
        ESTIMATED,
        NOT_AVAILABLE
    }

    /**
     * Create an air quality response with direct data from the requested commune.
     */
    public static AirQualityResponse direct(
            String inseeCode,
            String communeName,
            LocalDate measurementDate,
            Integer atmoIndex,
            String qualifier,
            String color,
            Map<String, Integer> pollutants) {
        return new AirQualityResponse(
            inseeCode,
            communeName,
            measurementDate,
            atmoIndex,
            qualifier,
            color,
            pollutants,
            DataSource.DIRECT,
            null,
            null,
            "Données mesurées pour cette commune"
        );
    }

    /**
     * Create an air quality response with estimated data from nearest commune.
     *
     * @param inseeCode INSEE code of requested commune
     * @param communeName Name of requested commune
     * @param measurementDate Date of measurement
     * @param atmoIndex ATMO index from nearest commune
     * @param qualifier ATMO qualifier from nearest commune
     * @param color Hex color code from nearest commune
     * @param pollutants Pollutant map from nearest commune
     * @param nearestCommuneName Name of commune where data was actually measured
     * @param distanceKm Distance between requested and nearest commune
     */
    public static AirQualityResponse estimated(
            String inseeCode,
            String communeName,
            LocalDate measurementDate,
            Integer atmoIndex,
            String qualifier,
            String color,
            Map<String, Integer> pollutants,
            String nearestCommuneName,
            Double distanceKm) {
        String qualityNote = String.format(
            "Données estimées depuis %s (%.1f km)",
            nearestCommuneName,
            distanceKm);
        return new AirQualityResponse(
            inseeCode,
            communeName,
            measurementDate,
            atmoIndex,
            qualifier,
            color,
            pollutants,
            DataSource.ESTIMATED,
            nearestCommuneName,
            distanceKm,
            qualityNote
        );
    }

    /**
     * Create a not-available response when no data exists within threshold.
     */
    public static AirQualityResponse notAvailable(
            String inseeCode,
            String communeName) {
        return new AirQualityResponse(
            inseeCode,
            communeName,
            null,
            null,
            null,
            null,
            null,
            DataSource.NOT_AVAILABLE,
            null,
            null,
            "Aucune donnée disponible dans un rayon de 20 km"
        );
    }
}
