package fr.airsen.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

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
    private double temperature;

    @Column(name = "humidity")
    @DecimalMin(value = "0.0", message = "The humidity must be at least 0%")
    @DecimalMax(value = "100.0", message = "The humidity must be at most 100%")
    private double humidity;

    @Column(name = "wind_speed")
    @DecimalMin(value = "0.0", message = "Wind speed cannot be negative")
    private double windSpeed;

    @Column(name = "wind_direction")
    @DecimalMin(value = "0.0", message = "Wind direction must be at least 0°")
    @DecimalMax(value = "360.0", message = "Wind direction must be at most 360°")
    private double windDirection;

    @Column(name = "weather_code")
    private Integer weatherCode;

    @Column(name = "created_at")
    private LocalDate createdAt;

    public WeatherData() {
    }

    public WeatherData(Commune commune, LocalDate measurementDate, double temperature, double humidity, double windSpeed, double windDirection, Integer weatherCode, LocalDate createdAt) {
        this.commune = commune;
        this.measurementDate = measurementDate;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.windDirection = windDirection;
        this.weatherCode = weatherCode;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(Integer id) {
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

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getHumidity() {
        return humidity;
    }

    public void setHumidity(double humidity) {
        this.humidity = humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public double getWindDirection() {
        return windDirection;
    }

    public void setWindDirection(double windDirection) {
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



    // External API integration methods (aliases for compatibility)
    public Double getCurrentTemperature() {
        return temperature;
    }

    public void setCurrentTemperature(Double temp) {
        this.temperature = temp != null ? temp : 0.0;
    }

    public Double getRelativeHumidity() {
        return humidity;
    }

    public void setRelativeHumidity(Double humidity) {
        this.humidity = humidity != null ? humidity : 0.0;
    }

    public Double getWindSpeed10m() {
        return windSpeed;
    }

    public void setWindSpeed10m(Double windSpeed) {
        this.windSpeed = windSpeed != null ? windSpeed : 0.0;
    }

    public Double getWindDirection10m() {
        return windDirection;
    }

    public void setWindDirection10m(Double windDirection) {
        this.windDirection = windDirection != null ? windDirection : 0.0;
    }

    public Integer getWeatherCodeValue() {
        return weatherCode;
    }

    public void setWeatherCodeValue(Integer weatherCode) {
        this.weatherCode = weatherCode;
    }

    public Double getApparentTemperature() {
        return temperature;
    }

    public void setApparentTemperature(Double temp) {
        this.temperature = temp != null ? temp : 0.0;
    }


    public void setMaxTemperature(Double maxTemperature) {
        this.temperature = maxTemperature != null ? maxTemperature : 0.0;
    }

    public Double getMaxTemperature() {
        return temperature;
    }

    public void setMinTemperature(Double minTemperature) {
        this.temperature = minTemperature != null ? minTemperature : 0.0;
    }

    public Double getMinTemperature() {
        return temperature;
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
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof WeatherData that)) return false;
        return Double.compare(temperature, that.temperature) == 0 && Double.compare(humidity, that.humidity) == 0 && Double.compare(windSpeed, that.windSpeed) == 0 && Double.compare(windDirection, that.windDirection) == 0 && Objects.equals(commune, that.commune) && Objects.equals(measurementDate, that.measurementDate) && Objects.equals(weatherCode, that.weatherCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commune, measurementDate, temperature, humidity, windSpeed, windDirection, weatherCode);
    }
}
