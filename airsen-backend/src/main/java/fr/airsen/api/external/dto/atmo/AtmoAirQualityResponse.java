package fr.airsen.api.external.dto.atmo;

import com.fasterxml.jackson.annotation.JsonProperty;


/**
 * Response DTO for ATMO air quality measurements.
 *
 * Maps the ATMO France API GeoJSON response for air quality indices
 * and pollutant concentrations from the features.properties structure.
 *
 * The code_qual field represents the ATMO air quality index (1-6 scale):
 * 1 = Bon (Good), 2 = Moyen (Moderate), 3 = Dégradé (Degraded),
 * 4 = Mauvais (Bad), 5 = Très mauvais (Very Bad), 6 = Extrêmement mauvais (Extremely Bad)
 *
 * EPCI Support:
 * The code_zone can be EITHER:
 * - INSEE code (5 digits, e.g., "31555" for Toulouse)
 * - EPCI SIREN code (9 digits, e.g., "243100518" for Toulouse Métropole)
 * Use type_zone field to distinguish: "commune" vs "EPCI"
 */
public record AtmoAirQualityResponse(
    @JsonProperty("code_zone") String communeInsee,
    @JsonProperty("type_zone") String zoneType,
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
    /**
     * Checks if this response represents commune-level data.
     *
     * @return true if zone type is "commune", false otherwise
     */
    public boolean isCommuneZone() {
        return "commune".equalsIgnoreCase(zoneType);
    }

    /**
     * Checks if this response represents EPCI-level data.
     *
     * @return true if zone type is "EPCI", false otherwise
     */
    public boolean isEpciZone() {
        return "EPCI".equalsIgnoreCase(zoneType);
    }
}
