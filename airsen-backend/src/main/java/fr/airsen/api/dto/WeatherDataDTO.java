package fr.airsen.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Data Transfer Object for weather data.
 * 
 * Represents weather measurements for a specific commune and date.
 */
public record WeatherDataDTO(
    @NotNull(message = "ID cannot be null")
    Long id,
    
    @NotNull(message = "Commune ID cannot be null")
    Long communeId,
    
    String communeName,
    
    String inseeCode,
    
    @NotNull(message = "Measurement date cannot be null")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate measurementDate,
    
    @DecimalMin(value = "-50.0", message = "Temperature must be at least -50°C")
    @DecimalMax(value = "60.0", message = "Temperature must be at most 60°C")
    Double temperature,
    
    @Min(value = 0, message = "Humidity must be at least 0%")
    @Max(value = 100, message = "Humidity must be at most 100%")
    Integer humidity,

    @DecimalMin(value = "0.0", message = "Wind speed cannot be negative")
    Double windSpeed,

    @Min(value = 0, message = "Wind direction must be at least 0°")
    @Max(value = 360, message = "Wind direction must be at most 360°")
    Integer windDirection,
    
    Integer weatherCode,
    
    @DecimalMin(value = "-60.0", message = "Apparent temperature must be at least -60°C")
    @DecimalMax(value = "70.0", message = "Apparent temperature must be at most 70°C")
    Double apparentTemperature,
    
    @DecimalMin(value = "0.0", message = "Precipitation cannot be negative")
    Double precipitation,
    
    @DecimalMin(value = "0.0", message = "Rain amount cannot be negative")
    Double rain,
    
    @DecimalMin(value = "0.0", message = "Shower amount cannot be negative")
    Double showers,
    
    @DecimalMin(value = "0.0", message = "Snowfall amount cannot be negative")
    Double snowfall,
    
    @Min(value = 0, message = "Cloud cover must be at least 0%")
    @Max(value = 100, message = "Cloud cover must be at most 100%")
    Integer cloudCover,
    
    @DecimalMin(value = "0.0", message = "Wind gusts cannot be negative")
    Double windGusts,
    
    @DecimalMin(value = "870.0", message = "Pressure must be at least 870 hPa")
    @DecimalMax(value = "1085.0", message = "Pressure must be at most 1085 hPa")
    Double pressureMsl,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate createdAt
) {}