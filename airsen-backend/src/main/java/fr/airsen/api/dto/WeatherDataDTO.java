package fr.airsen.api.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
    
    @DecimalMin(value = "0.0", message = "Humidity must be at least 0%")
    @DecimalMax(value = "100.0", message = "Humidity must be at most 100%")
    Double humidity,
    
    @DecimalMin(value = "0.0", message = "Wind speed cannot be negative")
    Double windSpeed,
    
    @DecimalMin(value = "0.0", message = "Wind direction must be at least 0°")
    @DecimalMax(value = "360.0", message = "Wind direction must be at most 360°")
    Double windDirection,
    
    Integer weatherCode,
    
    @DecimalMin(value = "0.0", message = "Precipitation cannot be negative")
    Double precipitation,
    
    @DecimalMin(value = "0.0", message = "Pressure cannot be negative")
    Double pressure,
    
    @DecimalMin(value = "0.0", message = "Visibility cannot be negative")
    Double visibility,
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate createdAt
) {}