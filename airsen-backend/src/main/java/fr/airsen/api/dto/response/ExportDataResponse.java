package fr.airsen.api.dto.response;

import java.time.LocalDateTime;

/**
 * Complete export data response for a commune.
 * 
 * Contains all data needed for PDF/CSV export in a single API call.
 * Used by the frontend to generate client-side PDF and CSV exports.
 * 
 * @param commune Complete commune information with geographic and demographic data
 * @param airQuality Latest air quality measurements
 * @param weather Latest weather data
 * @param exportMetadata Metadata about the export (generation time, data freshness)
 */
public record ExportDataResponse(
    CommuneExportDTO commune,
    AirQualityExportDTO airQuality,
    WeatherExportDTO weather,
    ExportMetadata exportMetadata
) {}
