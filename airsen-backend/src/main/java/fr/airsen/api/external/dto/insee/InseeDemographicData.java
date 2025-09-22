package fr.airsen.api.external.dto.insee;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for INSEE demographic statistics.
 * 
 * Maps detailed demographic data for French communes.
 */
public record InseeDemographicData(
    @JsonProperty("code") String inseeCode,
    @JsonProperty("population") Integer population,
    @JsonProperty("superficie") Double area,
    @JsonProperty("densite") Double density,
    @JsonProperty("nbMenages") Integer householdCount,
    @JsonProperty("nbLogements") Integer housingCount
) {}