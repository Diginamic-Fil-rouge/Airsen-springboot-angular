package fr.airsen.api.dto.response;

import java.time.LocalDate;

/**
 * DTO representing weather data from the nearest commune within geodistance threshold.
 * Used when target commune has no direct weather data.
 *
 * @param inseeCode INSEE code of the nearest commune (not the target commune)
 * @param communeName Name of the nearest commune
 * @param latitude Latitude of the nearest commune
 * @param longitude Longitude of the nearest commune
 * @param measurementDate Date of the weather measurement
 * @param temperature Temperature in Celsius
 * @param humidity Relative humidity percentage
 * @param windSpeed Wind speed in km/h
 * @param windDirection Wind direction in degrees (0-360)
 * @param weatherCode WMO weather code
 * @param apparentTemperature Apparent temperature in Celsius
 * @param precipitation Total precipitation in mm
 * @param rain Rain amount in mm
 * @param showers Shower intensity in mm
 * @param snowfall Snowfall amount in cm
 * @param cloudCover Cloud coverage percentage
 * @param windGusts Wind gust speed in km/h
 * @param pressureMsl Mean sea level pressure in hPa
 * @param distanceKm Distance from target commune to this nearest commune
 */
public record NearestWeatherResult(
    String inseeCode,
    String communeName,
    Double latitude,
    Double longitude,
    LocalDate measurementDate,
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
    Double distanceKm
) {
    /**
     * Validate that distance is within PRD-defined threshold.
     */
    public NearestWeatherResult {
        if (distanceKm == null || distanceKm < 0) {
            throw new IllegalArgumentException("Distance must be non-negative");
        }
        if (distanceKm > 20.0) {
            throw new IllegalArgumentException(
                String.format("Distance %.2f km exceeds PRD threshold of 20km", distanceKm));
        }
    }
}
