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
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate createdAt
) {}