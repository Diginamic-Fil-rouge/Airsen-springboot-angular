package fr.airsen.api.dto.response;

/**
 * Date range for historical data queries.
 * 
 * @param start Start date in ISO 8601 format (e.g., "2025-09-01")
 * @param end End date in ISO 8601 format (e.g., "2025-10-09")
 */
public record DateRange(
    String start,
    String end
) {}
