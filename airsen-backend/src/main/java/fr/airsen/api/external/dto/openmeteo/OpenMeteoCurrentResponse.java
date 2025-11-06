package fr.airsen.api.external.dto.openmeteo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Response DTO for Open-Meteo current weather conditions.
 *
 * Maps comprehensive weather data from Open-Meteo API including temperature,
 * humidity, wind conditions, and advanced weather metrics (precipitation,
 * cloud cover, pressure, etc.). Coordinates are provided by the caller from INSEE API data.
 *
 * Maps to Open-Meteo API response structure and supports all current fields
 * including optional advanced weather indicators.
 */
public record OpenMeteoCurrentResponse(
    @JsonProperty("timezone") String timezone,
    @JsonProperty("current") CurrentWeather current
) {

    /**
     * Nested record for current weather data from Open-Meteo API.
     *
     * Contains basic weather metrics (temperature, humidity, wind) and
     * advanced indicators (precipitation, cloud cover, pressure, etc.).
     * All fields are nullable to handle varying API responses.
     *
     * Field Units:
     * - Temperatures: Celsius (°C)
     * - Precipitation/Rain/Showers: millimeters (mm)
     * - Snowfall: centimeters (cm)
     * - Wind speeds: kilometers per hour (km/h)
     * - Wind direction: degrees (0-360)
     * - Pressure: hectopascals (hPa)
     * - Cloud cover: percentage (0-100%)
     */
    public record CurrentWeather(
        @JsonProperty("time") @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime time,

        @JsonProperty("temperature_2m") Double temperature,
        @JsonProperty("relative_humidity_2m") Integer humidity,
        @JsonProperty("apparent_temperature") Double apparentTemperature,
        @JsonProperty("weather_code") Integer weatherCode,

        @JsonProperty("wind_speed_10m") Double windSpeed,
        @JsonProperty("wind_direction_10m") Integer windDirection,
        @JsonProperty("wind_gusts_10m") Double windGusts,

        @JsonProperty("precipitation") Double precipitation,
        @JsonProperty("rain") Double rain,
        @JsonProperty("showers") Double showers,
        @JsonProperty("snowfall") Double snowfall,

        @JsonProperty("cloud_cover") Integer cloudCover,
        @JsonProperty("pressure_msl") Double pressureMsl,

        @JsonProperty("visibility") Double visibility
    ) {}
}
