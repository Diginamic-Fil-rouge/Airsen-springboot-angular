package fr.airsen.api.dto.response;

/**
 * Department export information.
 * 
 * Contains department data and hierarchy to parent region.
 */
public record DepartmentExportDTO(
    Long id,
    String name,
    String departmentCode,
    String regionCode,
    RegionExportDTO region
) {}
