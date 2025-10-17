package fr.airsen.api.dto.response;

/**
 * Data completeness metrics for historical data.
 * 
 * Indicates the percentage of expected data points that were successfully retrieved.
 * Useful for understanding data quality and sensor availability.
 * 
 * @param airQuality Percentage of air quality data points available (0-100)
 * @param weather Percentage of weather data points available (0-100)
 */
public record Completeness(
    Double airQuality,
    Double weather
) {}
