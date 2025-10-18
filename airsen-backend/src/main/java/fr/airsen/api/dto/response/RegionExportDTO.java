package fr.airsen.api.dto.response;

/**
 * Region export information for API responses.
 * 
 * Contains region name and official INSEE code for identification.
 * Represents the top level of the French administrative hierarchy.
 * 
 * Note: Intentionally excludes the internal database ID field since:
 * - IDs are implementation details that shouldn't be exposed in public APIs
 * - The regionCode is the stable, official identifier defined by INSEE
 * - This reduces payload size and couples the API to official standards, not DB schema
 */
public record RegionExportDTO(
    String name,
    String regionCode
) {}
