package fr.airsen.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entity representing the weather data for a given commune.
 */
@Entity
@Table(name = "weather_data")
public class WeatherData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "commune_id")
    @JsonIgnoreProperties({"weatherData", "airQualities", "airQuality", "hibernateLazyInitializer", "handler"})
    private Commune commune;

    @Column(name = "measurement_date")
    private LocalDate measurementDate;

    @Column(name = "temperature")
    @DecimalMin(value = "-50.0", message = "The temperature must be at least -50°C")
    @DecimalMax(value = "60.0", message = "The temperature must be at most 60°C")
    private Double temperature;

    @Column(name = "humidity")
    @Min(value = 0, message = "The humidity must be at least 0%")
    @Max(value = 100, message = "The humidity must be at most 100%")
    private Integer humidity;

    @Column(name = "wind_speed")
    @DecimalMin(value = "0.0", message = "Wind speed cannot be negative")
    private Double windSpeed;

    @Column(name = "wind_direction")
    @Min(value = 0, message = "Wind direction must be at least 0°")
    @Max(value = 360, message = "Wind direction must be at most 360°")
    private Integer windDirection;

    @Column(name = "weather_code")
    private Integer weatherCode;

    @Column(name = "apparent_temperature")
    @DecimalMin(value = "-60.0", message = "Apparent temperature must be at least -60°C")
    @DecimalMax(value = "70.0", message = "Apparent temperature must be at most 70°C")
    private Double apparentTemperature;

    @Column(name = "precipitation")
    @DecimalMin(value = "0.0", message = "Precipitation cannot be negative")
    private Double precipitation;

    @Column(name = "rain")
    @DecimalMin(value = "0.0", message = "Rain amount cannot be negative")
    private Double rain;

    @Column(name = "showers")
    @DecimalMin(value = "0.0", message = "Shower amount cannot be negative")
    private Double showers;

    @Column(name = "snowfall")
    @DecimalMin(value = "0.0", message = "Snowfall amount cannot be negative")
    private Double snowfall;

    @Column(name = "cloud_cover")
    @Min(value = 0, message = "Cloud cover must be at least 0%")
    @Max(value = 100, message = "Cloud cover must be at most 100%")
    private Integer cloudCover;

    @Column(name = "wind_gusts")
    @DecimalMin(value = "0.0", message = "Wind gusts cannot be negative")
    private Double windGusts;

    @Column(name = "pressure_msl")
    @DecimalMin(value = "870.0", message = "Pressure must be at least 870 hPa")
    @DecimalMax(value = "1085.0", message = "Pressure must be at most 1085 hPa")
    private Double pressureMsl;

    @Column(name = "created_at")
    private LocalDate createdAt;

    public WeatherData() {
    }

    public WeatherData(Commune commune, LocalDate measurementDate, Double temperature, Integer humidity, Double windSpeed, Integer windDirection, Integer weatherCode, LocalDate createdAt) {
        this.commune = commune;
        this.measurementDate = measurementDate;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.weatherCode = weatherCode;
        this.createdAt = createdAt;
    }

    public WeatherData(Commune commune, LocalDate measurementDate, Double temperature, Integer humidity,
                       Double windSpeed, Integer windDirection, Integer weatherCode,
                       Double apparentTemperature, Double precipitation, Double rain, Double showers,
                       Double snowfall, Integer cloudCover, Double windGusts, Double pressureMsl,
                       LocalDate createdAt) {
        this.commune = commune;
        this.measurementDate = measurementDate;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.weatherCode = weatherCode;
        this.apparentTemperature = apparentTemperature;
        this.precipitation = precipitation;
        this.rain = rain;
        this.showers = showers;
        this.snowfall = snowfall;
        this.cloudCover = cloudCover;
        this.windGusts = windGusts;
        this.pressureMsl = pressureMsl;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Commune getCommune() {
        return commune;
    }

    public void setCommune(Commune commune) {
        this.commune = commune;
    }

    public LocalDate getMeasurementDate() {
        return measurementDate;
    }

    public void setMeasurementDate(LocalDate measurementDate) {
        this.measurementDate = measurementDate;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getHumidity() {
        return humidity;
    }

    public void setHumidity(Integer humidity) {
        this.humidity = humidity;
    }

    public Double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(Double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public Integer getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(Integer windDirection) {
        this.windDirection = windDirection;
    }

    public Integer getWeatherCode() {
        return weatherCode;
    }

    public void setWeatherCode(Integer weatherCode) {
        this.weatherCode = weatherCode;
    }

    public LocalDate getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDate createdAt) {
        this.createdAt = createdAt;
    }

    public Double getApparentTemperature() {
        return apparentTemperature;
    }

    public void setApparentTemperature(Double apparentTemperature) {
        this.apparentTemperature = apparentTemperature;
    }

    public Double getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(Double precipitation) {
        this.precipitation = precipitation;
    }

    public Double getRain() {
        return rain;
    }

    public void setRain(Double rain) {
        this.rain = rain;
    }

    public Double getShowers() {
        return showers;
    }

    public void setShowers(Double showers) {
        this.showers = showers;
    }

    public Double getSnowfall() {
        return snowfall;
    }

    public void setSnowfall(Double snowfall) {
        this.snowfall = snowfall;
    }

    public Integer getCloudCover() {
        return cloudCover;
    }

    public void setCloudCover(Integer cloudCover) {
        this.cloudCover = cloudCover;
    }

    public Double getWindGusts() {
        return windGusts;
    }

    public void setWindGusts(Double windGusts) {
        this.windGusts = windGusts;
    }

    public Double getPressureMsl() {
        return pressureMsl;
    }

    public void setPressureMsl(Double pressureMsl) {
        this.pressureMsl = pressureMsl;
    }



    // External API integration methods (aliases for compatibility)
    public Double getCurrentTemperature() {
        return temperature;
    }

    public void setCurrentTemperature(Double temp) {
        this.temperature = temp != null ? temp : 0.0;
    }

    public Integer getRelativeHumidity() {
        return humidity;
    }

    public void setRelativeHumidity(Integer humidity) {
        this.humidity = humidity != null ? humidity : 0;
    }

    public Double getWindSpeed10m() {
        return windSpeed;
    }

    public void setWindSpeed10m(Double windSpeed) {
        this.windSpeed = windSpeed != null ? windSpeed : 0.0;
    }

    public Integer getWindDirection10m() {
        return windDirection;
    }

    public void setWindDirection10m(Integer windDirection) {
        this.windDirection = windDirection != null ? windDirection : 0;
    }

    public Integer getWeatherCodeValue() {
        return weatherCode;
    }

    public void setWeatherCodeValue(Integer weatherCode) {
        this.weatherCode = weatherCode;
    }

    @Override
    public String toString() {
        return "WeatherData{" +
                "id=" + id +
                ", commune=" + commune +
                ", measurementDate=" + measurementDate +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", windSpeed=" + windSpeed +
                ", windDirection=" + windDirection +
                ", weatherCode=" + weatherCode +
                ", apparentTemperature=" + apparentTemperature +
                ", precipitation=" + precipitation +
                ", rain=" + rain +
                ", showers=" + showers +
                ", snowfall=" + snowfall +
                ", cloudCover=" + cloudCover +
                ", windGusts=" + windGusts +
                ", pressureMsl=" + pressureMsl +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WeatherData that)) return false;
        return Objects.equals(temperature, that.temperature) &&
               Objects.equals(humidity, that.humidity) &&
               Objects.equals(windSpeed, that.windSpeed) &&
               Objects.equals(windDirection, that.windDirection) &&
               Objects.equals(commune, that.commune) &&
               Objects.equals(measurementDate, that.measurementDate) &&
               Objects.equals(weatherCode, that.weatherCode) &&
               Objects.equals(apparentTemperature, that.apparentTemperature) &&
               Objects.equals(precipitation, that.precipitation) &&
               Objects.equals(rain, that.rain) &&
               Objects.equals(showers, that.showers) &&
               Objects.equals(snowfall, that.snowfall) &&
               Objects.equals(cloudCover, that.cloudCover) &&
               Objects.equals(windGusts, that.windGusts) &&
               Objects.equals(pressureMsl, that.pressureMsl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commune, measurementDate, temperature, humidity, windSpeed, windDirection,
                           weatherCode, apparentTemperature, precipitation, rain, showers, snowfall,
                           cloudCover, windGusts, pressureMsl);
    }
}
