package fr.airsen.api.dto.response;

/**
 * Air quality data point for historical time-series data.
 * 
 * Contains air quality measurements at a specific point in time.
 */
public record AirQualityDataPoint(
    Integer aqi,
    String qualifier,
    String color,
    Integer no2,
    Integer o3,
    Integer pm10,
    Integer pm25,
    Integer so2
) {}
