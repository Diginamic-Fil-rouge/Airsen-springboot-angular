package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entity representing the weather data for a given commune.
 */
@Entity
public class WeatherData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "commune_id")
    private Commune commune;

    @Column(name = "measurement_date")
    private LocalDate measurementDate;

    @Column(name = "temperature")
    @Size(min = -50, max = 60, message = "The temperature must be between -50 and 60")
    private double temperature;

    @Column(name = "humidity")
    @Size(min = 0, max = 100, message = "The humidity must be between 0 and 100")
    private double humidity;

    @Column(name = "wind_speed")
    private double windSpeed;

    @Column(name = "wind_direction")
    @Size(min = 0, max = 360, message = "The wind direction must be between 0 and 360")
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

    public Integer getId() {
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



    @Override
    public String toString() {
        return "AirQuality{" +
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
