package fr.airsen.api.dto.response;

import java.time.LocalDateTime;

/**
 * Commune export information.
 * 
 * Contains commune demographics, geographic data, and hierarchy information.
 */
public record CommuneExportDTO(
    Long id,
    String inseeCode,
    String name,
    Long population,
    Double latitude,
    Double longitude,
    String departmentCode,
    String regionCode,
    DepartmentExportDTO department
) {}
