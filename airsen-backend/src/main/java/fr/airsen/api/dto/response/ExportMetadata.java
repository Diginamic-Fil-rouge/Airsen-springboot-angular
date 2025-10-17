package fr.airsen.api.dto.response;

import java.time.LocalDateTime;

/**
 * Export metadata containing information about data quality and freshness.
 * 
 * @param generatedAt Timestamp when the API response was created
 * @param dataFreshness Information about how recent the measurements are
 */
public record ExportMetadata(
    LocalDateTime generatedAt,
    DataFreshness dataFreshness
) {}
