package fr.airsen.api.dto;

import java.time.LocalDate;

/**
 * DTO for air quality API responses.
 * 
 * Contains all necessary data to avoid Hibernate lazy loading issues
 * when passing data from service to controller layer.
 */
public record AirQualityResponseDTO(
    // Commune information
    String communeInseeCode,
    String communeName,
    String departmentName,
    String regionName,
    
    // Air quality data
    LocalDate measurementDate,
    Integer atmoIndex,
    String qualifier,
    String color,
    Double no2Concentration,
    Double o3Concentration,
    Double pm10Concentration,
    Double pm25Concentration,
    Double so2Concentration,
    LocalDate createdAt
) {
    
    /**
     * Creates a DTO from an AirQuality entity.
     * This should be called within a transactional context to avoid lazy loading issues.
     */
    public static AirQualityResponseDTO fromEntity(fr.airsen.api.entity.AirQuality airQuality) {
        try {
            return new AirQualityResponseDTO(
                airQuality.getCommune().getInseeCode(),
                airQuality.getCommune().getName(),
                airQuality.getCommune().getDepartment().getName(),
                airQuality.getCommune().getDepartment().getRegion().getName(),
                airQuality.getMeasurementDate(),
                airQuality.getAtmoIndex(),
                airQuality.getQualifier(),
                airQuality.getColor(),
                airQuality.getNo2Concentration(),
                airQuality.getO3Concentration(),
                airQuality.getPm10Concentration(),
                airQuality.getPm25Concentration(),
                airQuality.getSo2Concentration(),
                airQuality.getCreatedAt()
            );
        } catch (org.hibernate.LazyInitializationException e) {
            // Fallback with partial data if lazy loading fails
            return new AirQualityResponseDTO(
                airQuality.getCommune().getInseeCode(),
                airQuality.getCommune().getName(),
                "Unknown Department", // fallback
                "Unknown Region", // fallback
                airQuality.getMeasurementDate(),
                airQuality.getAtmoIndex(),
                airQuality.getQualifier(),
                airQuality.getColor(),
                airQuality.getNo2Concentration(),
                airQuality.getO3Concentration(),
                airQuality.getPm10Concentration(),
                airQuality.getPm25Concentration(),
                airQuality.getSo2Concentration(),
                airQuality.getCreatedAt()
            );
        }
    }
}