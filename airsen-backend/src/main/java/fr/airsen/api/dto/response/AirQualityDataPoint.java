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
    Double no2,
    Double o3,
    Double pm10,
    Integer pm25,
    Double so2
) {}
