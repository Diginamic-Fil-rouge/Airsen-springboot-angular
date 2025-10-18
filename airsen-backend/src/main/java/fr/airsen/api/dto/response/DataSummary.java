package fr.airsen.api.dto.response;

/**
 * Summary statistics for historical data.
 * 
 * @param totalDataPoints Total number of data points returned in the historical data
 * @param completeness Data quality metrics indicating data availability
 */
public record DataSummary(
    Integer totalDataPoints,
    Completeness completeness
) {}
