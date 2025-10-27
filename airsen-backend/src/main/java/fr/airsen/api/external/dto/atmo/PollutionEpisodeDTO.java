package fr.airsen.api.external.dto.atmo;

import java.time.LocalDate;

/**
 * Represents an active pollution episode detected by ATMO France API,
 * including pollutant type, severity level, and geographic scope.
 */
public record PollutionEpisodeDTO(

    String id,

    String region,

    String pollutant,

    String level,

    LocalDate startDate,

    LocalDate endDate,

    String description
) {
}
