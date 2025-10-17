package fr.airsen.api.dto.response;

import java.time.LocalDateTime;

/**
 * Commune export information for API responses.
 * 
 * Contains commune demographics and geographic data for export operations.
 * Department hierarchy information is provided through the nested department DTO.
 * 
 * Note: Intentionally excludes redundant departmentCode and regionCode fields
 * to avoid data duplication. These codes are already available in the nested
 * department.region hierarchy and are the only identifiers needed for exports.
 */
public record CommuneExportDTO(
    Long id,
    String inseeCode,
    String name,
    Long population,
    Double latitude,
    Double longitude,
    DepartmentExportDTO department
) {}
