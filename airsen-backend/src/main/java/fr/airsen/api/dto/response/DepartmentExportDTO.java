package fr.airsen.api.dto.response;

/**
 * Department export information for API responses.
 * 
 * Contains department name and official INSEE code for identification.
 * Region hierarchy information is provided through the nested region DTO.
 * 
 * Note: Intentionally excludes the internal database ID field since:
 * - IDs are implementation details that shouldn't be exposed in public APIs
 * - The departmentCode is the stable, official identifier
 * - Excludes regionCode to avoid duplication (already in nested region)
 */
public record DepartmentExportDTO(
    String name,
    String departmentCode,
    RegionExportDTO region
) {}
