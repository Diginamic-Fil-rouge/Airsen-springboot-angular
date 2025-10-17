package fr.airsen.api.dto.response;

import java.util.List;

/**
 * Historical data response for CSV export.
 * 
 * Contains time-series data for a commune over a specified date range.
 * Used for trend analysis and CSV export generation.
 * 
 * @param commune Basic commune information (name, inseeCode)
 * @param dateRange The requested date range (start and end dates)
 * @param dataPoints Array of data points with air quality and weather measurements
 * @param summary Summary statistics about the data (total points, completeness)
 */
public record HistoricalDataResponse(
    CommuneBasicDTO commune,
    DateRange dateRange,
    List<DataPoint> dataPoints,
    DataSummary summary
) {}
