package fr.airsen.api.service;

import fr.airsen.api.dto.response.*;
import fr.airsen.api.entity.*;
import fr.airsen.api.repository.*;
import fr.airsen.api.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for export data aggregation.
 * 
 * Provides methods to retrieve and aggregate commune data for client-side export generation.
 * Supports two main use cases:
 * 
 * 1. Export Data (Current Snapshot): Get latest commune state for PDF export
 *    - Returns: Current commune info + latest air quality + latest weather
 *    - Response time: < 200ms
 *    - Size: ~2-5 KB
 * 
 * 2. Historical Data (Time-Series): Get data over a date range for CSV export
 *    - Returns: Array of data points with air quality + weather measurements
 *    - Response time: 500ms - 1s
 *    - Size: ~50-500 KB
 */
@Service
@Transactional(readOnly = true)
public class ExportDataService {

    private final CommuneRepository communeRepository;
    private final AirQualityRepository airQualityRepository;
    private final WeatherDataRepository weatherDataRepository;

    @Autowired
    public ExportDataService(
            CommuneRepository communeRepository,
            AirQualityRepository airQualityRepository,
            WeatherDataRepository weatherDataRepository) {
        this.communeRepository = communeRepository;
        this.airQualityRepository = airQualityRepository;
        this.weatherDataRepository = weatherDataRepository;
    }

    /**
     * Gets export data for a commune (current snapshot).
     * 
     * Retrieves complete commune information with latest air quality and weather data.
     * Optimized for PDF export with a single aggregated API call.
     * 
     * @param inseeCode commune INSEE code
     * @return ExportDataResponse with current commune state
     * @throws EntityNotFoundException if commune is not found
     */
    public ExportDataResponse getExportData(String inseeCode) {
        // Fetch commune with geographic data
        Commune commune = communeRepository.findByInseeCodeWithEagerLoading(inseeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Commune not found with INSEE code: " + inseeCode));

        // Fetch latest air quality data
        AirQuality latestAirQuality = airQualityRepository.findLatestExportDataByInseeCode(inseeCode)
                .orElse(null);

        // Fetch latest weather data
        WeatherData latestWeather = weatherDataRepository.findLatestExportDataByInseeCode(inseeCode)
                .orElse(null);

        // Build export data response
        CommuneExportDTO communeDTO = mapCommuneToExportDTO(commune);
        AirQualityExportDTO airQualityDTO = mapAirQualityToExportDTO(latestAirQuality);
        WeatherExportDTO weatherDTO = mapWeatherToExportDTO(latestWeather);
        ExportMetadata metadata = buildExportMetadata(latestAirQuality, latestWeather);

        return new ExportDataResponse(communeDTO, airQualityDTO, weatherDTO, metadata);
    }

    /**
     * Gets historical data for a commune (time-series for CSV export).
     * 
     * Retrieves time-series data for air quality and weather over a specified date range.
     * Optimized for CSV export with flexible date range support.
     * 
     * @param inseeCode commune INSEE code
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @param indicators optional comma-separated indicator filter
     * @return HistoricalDataResponse with time-series data points
     * @throws EntityNotFoundException if commune is not found
     * @throws IllegalArgumentException if date range is invalid
     */
    public HistoricalDataResponse getHistoricalData(String inseeCode, LocalDate startDate, LocalDate endDate, String indicators) {
        // Validate inputs
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }

        // Check for max 90-day range to prevent excessive data retrieval
        if (ChronoUnit.DAYS.between(startDate, endDate) > 90) {
            throw new IllegalArgumentException("Date range cannot exceed 90 days");
        }

        // Fetch commune basic info
        Commune commune = communeRepository.findByInseeCode(inseeCode)
                .orElseThrow(() -> new ResourceNotFoundException("Commune not found with INSEE code: " + inseeCode));

        // Fetch historical air quality data
        List<AirQuality> airQualityData = airQualityRepository.findHistoricalExportDataByInseeCode(inseeCode, startDate, endDate);

        // Fetch historical weather data
        List<WeatherData> weatherData = weatherDataRepository.findHistoricalExportDataByInseeCode(inseeCode, startDate, endDate);

        // Build data points
        List<DataPoint> dataPoints = buildDataPoints(airQualityData, weatherData);

        // Build response
        CommuneBasicDTO communeDTO = new CommuneBasicDTO(commune.getName(), commune.getInseeCode());
        DateRange dateRange = new DateRange(startDate.toString(), endDate.toString());
        DataSummary summary = calculateDataSummary(dataPoints, startDate, endDate);

        return new HistoricalDataResponse(communeDTO, dateRange, dataPoints, summary);
    }

    /**
     * Maps Commune entity to CommuneExportDTO.
     */
    private CommuneExportDTO mapCommuneToExportDTO(Commune commune) {
        Department department = commune.getDepartment();
        DepartmentExportDTO departmentDTO = null;

        if (department != null) {
            Region region = department.getRegion();
            RegionExportDTO regionDTO = new RegionExportDTO(
                    region.getId(),
                    region.getName(),
                    region.getRegionCode()
            );

            departmentDTO = new DepartmentExportDTO(
                    department.getId(),
                    department.getName(),
                    department.getDepartmentCode(),
                    department.getRegionCode(),
                    regionDTO
            );
        }

        return new CommuneExportDTO(
                commune.getId(),
                commune.getInseeCode(),
                commune.getName(),
                commune.getPopulation(),
                commune.getLatitude() != null ? commune.getLatitude().doubleValue() : null,
                commune.getLongitude() != null ? commune.getLongitude().doubleValue() : null,
                commune.getDepartmentCode(),
                commune.getRegionCode(),
                departmentDTO
        );
    }

    /**
     * Maps AirQuality entity to AirQualityExportDTO.
     */
    private AirQualityExportDTO mapAirQualityToExportDTO(AirQuality airQuality) {
        if (airQuality == null) {
            return null;
        }

        return new AirQualityExportDTO(
                airQuality.getMeasurementDate() != null 
                    ? airQuality.getMeasurementDate().atStartOfDay() 
                    : null,
                airQuality.getAtmIndex(),
                airQuality.getAtmoQual(),
                airQuality.getAtmoColor(),
                airQuality.getNO2(),
                airQuality.getO3(),
                airQuality.getPm10(),
                airQuality.getPm25(),
                airQuality.getSO2(),
                airQuality.getCreatedAt() != null 
                    ? airQuality.getCreatedAt().atStartOfDay() 
                    : null
        );
    }

    /**
     * Maps WeatherData entity to WeatherExportDTO.
     */
    private WeatherExportDTO mapWeatherToExportDTO(WeatherData weather) {
        if (weather == null) {
            return null;
        }

        return new WeatherExportDTO(
                weather.getMeasurementDate() != null 
                    ? weather.getMeasurementDate().atStartOfDay() 
                    : null,
                weather.getTemperature(),
                weather.getHumidity(),
                weather.getWindSpeed(),
                weather.getWindDirection(),
                weather.getWeatherCode(),
                weather.getCreatedAt() != null 
                    ? weather.getCreatedAt().atStartOfDay() 
                    : null
        );
    }

    /**
     * Builds export metadata with data freshness information.
     */
    private ExportMetadata buildExportMetadata(AirQuality latestAirQuality, WeatherData latestWeather) {
        LocalDateTime generatedAt = LocalDateTime.now();

        String airQualityFreshness = latestAirQuality != null 
            ? calculateFreshness(latestAirQuality.getMeasurementDate()) 
            : "No data available";

        String weatherFreshness = latestWeather != null 
            ? calculateFreshness(latestWeather.getMeasurementDate()) 
            : "No data available";

        DataFreshness dataFreshness = new DataFreshness(airQualityFreshness, weatherFreshness);

        return new ExportMetadata(generatedAt, dataFreshness);
    }

    /**
     * Calculates human-readable freshness description.
     */
    private String calculateFreshness(LocalDate measurementDate) {
        if (measurementDate == null) {
            return "Unknown";
        }

        LocalDate today = LocalDate.now();
        long daysDiff = ChronoUnit.DAYS.between(measurementDate, today);

        if (daysDiff == 0) {
            return "Today";
        } else if (daysDiff == 1) {
            return "Yesterday";
        } else if (daysDiff < 7) {
            return daysDiff + " days ago";
        } else if (daysDiff < 30) {
            long weeksDiff = daysDiff / 7;
            return weeksDiff + " week" + (weeksDiff > 1 ? "s" : "") + " ago";
        } else {
            long monthsDiff = daysDiff / 30;
            return monthsDiff + " month" + (monthsDiff > 1 ? "s" : "") + " ago";
        }
    }

    /**
     * Builds data points from air quality and weather data lists.
     */
    private List<DataPoint> buildDataPoints(List<AirQuality> airQualityList, List<WeatherData> weatherList) {
        List<DataPoint> dataPoints = new ArrayList<>();

        // Create a map of weather data by date for easy lookup
        java.util.Map<LocalDate, WeatherData> weatherByDate = new java.util.HashMap<>();
        weatherList.forEach(w -> weatherByDate.putIfAbsent(w.getMeasurementDate(), w));

        // Build data points from air quality data
        for (AirQuality aq : airQualityList) {
            WeatherData weather = weatherByDate.get(aq.getMeasurementDate());

            AirQualityDataPoint aqPoint = new AirQualityDataPoint(
                    aq.getAtmIndex(),
                    aq.getAtmoQual(),
                    aq.getAtmoColor(),
                    aq.getNO2(),
                    aq.getO3(),
                    aq.getPm10(),
                    aq.getPm25(),
                    aq.getSO2()
            );

            WeatherDataPoint weatherPoint = weather != null
                    ? new WeatherDataPoint(
                            weather.getTemperature(),
                            weather.getHumidity(),
                            weather.getWindSpeed(),
                            weather.getWindDirection(),
                            weather.getWeatherCode(),
                            null, // precipitation not available in WeatherData entity
                            null  // cloudCover not available in WeatherData entity
                    )
                    : null;

            DataPoint dataPoint = new DataPoint(
                    aq.getMeasurementDate().atStartOfDay(),
                    aqPoint,
                    weatherPoint
            );

            dataPoints.add(dataPoint);
        }

        return dataPoints;
    }

    /**
     * Calculates data summary with completeness metrics.
     */
    private DataSummary calculateDataSummary(List<DataPoint> dataPoints, LocalDate startDate, LocalDate endDate) {
        int totalDataPoints = dataPoints.size();

        // Calculate expected data points (1 per day in range)
        int expectedDataPoints = (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;

        // Calculate completeness percentages
        double airQualityCompleteness = (totalDataPoints > 0)
                ? (100.0 * totalDataPoints) / expectedDataPoints
                : 0.0;

        double weatherCompleteness = (totalDataPoints > 0)
                ? (100.0 * dataPoints.stream()
                        .filter(dp -> dp.weather() != null)
                        .count()) / expectedDataPoints
                : 0.0;

        // Cap completeness at 100%
        airQualityCompleteness = Math.min(100.0, airQualityCompleteness);
        weatherCompleteness = Math.min(100.0, weatherCompleteness);

        Completeness completeness = new Completeness(airQualityCompleteness, weatherCompleteness);

        return new DataSummary(totalDataPoints, completeness);
    }
}
