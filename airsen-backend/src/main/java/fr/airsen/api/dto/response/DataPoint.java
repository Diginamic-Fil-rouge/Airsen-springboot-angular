package fr.airsen.api.dto.response;

import java.time.LocalDateTime;

/**
 * Single data point in a historical data time-series.
 * 
 * Contains air quality and weather measurements for a specific timestamp.
 * 
 * @param timestamp The measurement timestamp
 * @param airQuality Air quality measurements at this point in time
 * @param weather Weather measurements at this point in time
 */
public record DataPoint(
    LocalDateTime timestamp,
    AirQualityDataPoint airQuality,
    WeatherDataPoint weather
) {}
