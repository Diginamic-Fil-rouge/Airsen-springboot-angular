package fr.airsen.api.external.dto.atmo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for ATMO air quality GeoJSON structure.
 * 
 * Maps the complete GeoJSON response from ATMO France API.
 */
public record AtmoGeoJsonResponse(
    @JsonProperty("type") String type,
    @JsonProperty("features") List<Feature> features
) {
    
    /**
     * GeoJSON Feature containing air quality data.
     */
    public record Feature(
        @JsonProperty("type") String type,
        @JsonProperty("properties") AtmoAirQualityResponse properties,
        @JsonProperty("geometry") Geometry geometry
    ) {}
    
    /**
     * GeoJSON Geometry (not used for air quality processing).
     */
    public record Geometry(
        @JsonProperty("type") String type,
        @JsonProperty("coordinates") List<Double> coordinates
    ) {}
}