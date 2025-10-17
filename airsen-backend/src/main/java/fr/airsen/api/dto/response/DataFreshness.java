package fr.airsen.api.dto.response;

/**
 * Data freshness information.
 * 
 * Indicates how recent the measurements are using human-readable descriptions.
 * 
 * @param airQuality Freshness description for air quality data (e.g., "15 minutes ago")
 * @param weather Freshness description for weather data (e.g., "30 minutes ago")
 */
public record DataFreshness(
    String airQuality,
    String weather
) {}
