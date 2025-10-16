package fr.airsen.api.external.dto.atmo;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

/**
 * Response DTO for ATMO air quality measurements.
 * 
 * Maps the ATMO France API GeoJSON response for air quality indices
 * and pollutant concentrations from the features.properties structure.
 */
public record AtmoAirQualityResponse(
    @JsonProperty("code_zone") String communeInsee,
    @JsonProperty("date_ech") String measurementDate,
    @JsonProperty("code_qual") Integer atmoIndex,
    @JsonProperty("lib_qual") String qualifier,
    @JsonProperty("coul_qual") String color,
    @JsonProperty("code_no2") Integer no2Code,
    @JsonProperty("code_o3") Integer o3Code,
    @JsonProperty("code_pm10") Integer pm10Code,
    @JsonProperty("code_pm25") Integer pm25Code,
    @JsonProperty("code_so2") Integer so2Code,
    @JsonProperty("lib_zone") String zoneName,
    @JsonProperty("source") String source,
    @JsonProperty("date_maj") String updateDate
) {
}