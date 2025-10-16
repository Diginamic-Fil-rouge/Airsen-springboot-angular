package fr.airsen.api.external.dto.insee;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response DTO for INSEE commune demographic data.
 * 
 * Maps French administrative and population information including coordinates.
 */
public record InseeCommuneResponse(
    @JsonProperty("code") String inseeCode,
    @JsonProperty("nom") String name,
    @JsonProperty("codeDepartement") String departmentCode,
    @JsonProperty("codeRegion") String regionCode,
    @JsonProperty("codesPostaux") List<String> postalCodes,
    @JsonProperty("population") Integer population,
    @JsonProperty("surface") Double surface,
    @JsonProperty("centre") GeographicCenter centre,
    @JsonProperty("departement") DepartmentInfo departement,
    @JsonProperty("region") RegionInfo region
) {
    
    /**
     * Nested record for geographic coordinates.
     */
    public record GeographicCenter(
        @JsonProperty("coordinates") List<Double> coordinates
    ) {
        /**
         * Gets longitude from coordinates array.
         * 
         * @return longitude (first coordinate)
         */
        public Double getLongitude() {
            return coordinates != null && coordinates.size() >= 2 ? coordinates.get(0) : null;
        }
        
        /**
         * Gets latitude from coordinates array.
         * 
         * @return latitude (second coordinate)
         */
        public Double getLatitude() {
            return coordinates != null && coordinates.size() >= 2 ? coordinates.get(1) : null;
        }
    }
    
    /**
     * Nested record for department information.
     */
    public record DepartmentInfo(
        @JsonProperty("code") String code,
        @JsonProperty("nom") String nom
    ) {}
    
    /**
     * Nested record for region information.
     */
    public record RegionInfo(
        @JsonProperty("code") String code,
        @JsonProperty("nom") String nom
    ) {}
}