package fr.airsen.api.dto;

import java.math.BigDecimal;

/**
 * @param atmoIndex ATMO air quality index (1-10, null if no data available)
 * @param qualifier Air quality descriptor (e.g., "Bon", "Moyen", "Dégradé", null if no data)
 * @param color Hex color code for visualization (e.g., "#50F0E6", null if no data)
 */
public record CommuneDTO(
    Long id,
    String inseeCode,
    String name,
    String departmentCode,
    String regionCode,
    Long population,
    BigDecimal latitude,
    BigDecimal longitude,
    Integer atmoIndex,
    String qualifier,
    String color
) {}
