package fr.airsen.api.dto.response;

/**
 * Region export information.
 * 
 * Highest level in the French geographic hierarchy.
 */
public record RegionExportDTO(
    Long id,
    String name,
    String regionCode
) {}
