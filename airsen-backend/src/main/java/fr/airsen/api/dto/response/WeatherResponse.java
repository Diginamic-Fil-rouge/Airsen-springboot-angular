package fr.airsen.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;

/**
 * Enhanced weather response DTO with data source transparency.
 *
 * Indicates whether weather data comes from:
 * - DIRECT: Measured at the requested commune
 * - ESTIMATED: Estimated from nearest commune within 20km (geodistance fallback)
 * - NOT_AVAILABLE: No data available within threshold
 *
 * @param inseeCode INSEE code of the requested commune
 * @param communeName Name of the requested commune
 * @param measurementDate Date of the weather measurement
 * @param temperature Temperature in Celsius
 * @param humidity Relative humidity percentage (0-100)
 * @param windSpeed Wind speed in km/h
 * @param windDirection Wind direction in degrees (0-360)
 * @param weatherCode WMO weather code
 * @param weatherDescription Human-readable weather description
 * @param apparentTemperature Feels-like temperature in Celsius
 * @param precipitation Total precipitation in mm
 * @param rain Rain amount in mm
 * @param showers Shower intensity in mm
 * @param snowfall Snowfall amount in cm
 * @param cloudCover Cloud coverage percentage (0-100%)
 * @param windGusts Wind gust speed in km/h
 * @param pressureMsl Mean sea level pressure in hPa
 * @param dataSource Data source indicator (DIRECT, ESTIMATED, NOT_AVAILABLE)
 * @param estimatedFromCommune Name of nearest commune if data is estimated
 * @param distanceKm Distance in kilometers if data is estimated
 * @param dataQualityNote French user-facing message about data quality
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record WeatherResponse(
    String inseeCode,
    String communeName,
    LocalDate measurementDate,
    Double temperature,
    Integer humidity,
    Double windSpeed,
    Integer windDirection,
    Integer weatherCode,
    String weatherDescription,
    Double apparentTemperature,
    Double precipitation,
    Double rain,
    Double showers,
    Double snowfall,
    Integer cloudCover,
    Double windGusts,
    Double pressureMsl,
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
     * Create a weather response with direct data from the requested commune.
     */
    public static WeatherResponse direct(
            String inseeCode,
            String communeName,
            LocalDate measurementDate,
            Double temperature,
            Integer humidity,
            Double windSpeed,
            Integer windDirection,
            Integer weatherCode,
            String weatherDescription,
            Double apparentTemperature,
            Double precipitation,
            Double rain,
            Double showers,
            Double snowfall,
            Integer cloudCover,
            Double windGusts,
            Double pressureMsl) {
        return new WeatherResponse(
            inseeCode,
            communeName,
            measurementDate,
            temperature,
            humidity,
            windSpeed,
            windDirection,
            weatherCode,
            weatherDescription,
            apparentTemperature,
            precipitation,
            rain,
            showers,
            snowfall,
            cloudCover,
            windGusts,
            pressureMsl,
            DataSource.DIRECT,
            null,
            null,
            "Données mesurées pour cette commune"
        );
    }

    /**
     * Create a weather response with estimated data from nearest commune.
     *
     * @param inseeCode INSEE code of requested commune
     * @param communeName Name of requested commune
     * @param measurementDate Date of measurement
     * @param temperature Temperature from nearest commune
     * @param humidity Humidity from nearest commune
     * @param windSpeed Wind speed from nearest commune
     * @param windDirection Wind direction from nearest commune
     * @param weatherCode WMO weather code from nearest commune
     * @param weatherDescription Weather description from nearest commune
     * @param apparentTemperature Apparent temperature from nearest commune
     * @param precipitation Precipitation from nearest commune
     * @param rain Rain from nearest commune
     * @param showers Showers from nearest commune
     * @param snowfall Snowfall from nearest commune
     * @param cloudCover Cloud cover from nearest commune
     * @param windGusts Wind gusts from nearest commune
     * @param pressureMsl Pressure from nearest commune
     * @param nearestCommuneName Name of commune where data was actually measured
     * @param distanceKm Distance between requested and nearest commune
     */
    public static WeatherResponse estimated(
            String inseeCode,
            String communeName,
            LocalDate measurementDate,
            Double temperature,
            Integer humidity,
            Double windSpeed,
            Integer windDirection,
            Integer weatherCode,
            String weatherDescription,
            Double apparentTemperature,
            Double precipitation,
            Double rain,
            Double showers,
            Double snowfall,
            Integer cloudCover,
            Double windGusts,
            Double pressureMsl,
            String nearestCommuneName,
            Double distanceKm) {
        String qualityNote = String.format(
            "Données estimées depuis %s (%.1f km)",
            nearestCommuneName,
            distanceKm);
        return new WeatherResponse(
            inseeCode,
            communeName,
            measurementDate,
            temperature,
            humidity,
            windSpeed,
            windDirection,
            weatherCode,
            weatherDescription,
            apparentTemperature,
            precipitation,
            rain,
            showers,
            snowfall,
            cloudCover,
            windGusts,
            pressureMsl,
            DataSource.ESTIMATED,
            nearestCommuneName,
            distanceKm,
            qualityNote
        );
    }

    /**
     * Create a not-available response when no data exists within threshold.
     */
    public static WeatherResponse notAvailable(
            String inseeCode,
            String communeName) {
        return new WeatherResponse(
            inseeCode,
            communeName,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
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
