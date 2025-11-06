package fr.airsen.api.service;

import fr.airsen.api.dto.response.*;
import fr.airsen.api.entity.*;
import fr.airsen.api.exception.ResourceNotFoundException;
import fr.airsen.api.repository.AirQualityRepository;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.WeatherDataRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for export data aggregation.
 * 
 * Provides methods to retrieve and aggregate commune data for client-side export generation.
 * Supports two main use cases:
 * 
 * 1. Export Data (Current Snapshot): Get latest commune state for PDF export
 *    - Returns: Current commune info + latest air quality + latest weather
 * 
 * 2. Historical Data (Time-Series): Get data over a date range for CSV export
 *    - Returns: Array of data points with air quality + weather measurements
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

        // Build data points (with optional indicator filtering)
        List<DataPoint> dataPoints = buildDataPoints(airQualityData, weatherData, indicators);

        // Build response
        CommuneBasicDTO communeDTO = new CommuneBasicDTO(commune.getName(), commune.getInseeCode());
        DateRange dateRange = new DateRange(startDate.toString(), endDate.toString());
        DataSummary summary = calculateDataSummary(dataPoints, startDate, endDate);

        return new HistoricalDataResponse(communeDTO, dateRange, dataPoints, summary);
    }

    /**
     * Maps Commune entity to CommuneExportDTO.
     * 
     * Builds a clean DTO hierarchy without redundant fields:
     * - Removes departmentCode and regionCode (available in nested department.region)
     * - Removes internal database IDs (use official INSEE codes instead)
     * - Maintains geographic hierarchy through nested DTOs
     */
    private CommuneExportDTO mapCommuneToExportDTO(Commune commune) {
        Department department = commune.getDepartment();
        DepartmentExportDTO departmentDTO = null;

        if (department != null) {
            Region region = department.getRegion();
            // Create region DTO with official code only (no internal ID)
            RegionExportDTO regionDTO = new RegionExportDTO(
                    region.getName(),
                    region.getRegionCode()
            );

            // Create department DTO without ID and without redundant regionCode
            departmentDTO = new DepartmentExportDTO(
                    department.getName(),
                    department.getDepartmentCode(),
                    regionDTO
            );
        }

        // Create commune DTO with geographic data but no redundant codes
        return new CommuneExportDTO(
                commune.getInseeCode(),
                commune.getName(),
                commune.getPopulation(),
                commune.getLatitude() != null ? commune.getLatitude().doubleValue() : null,
                commune.getLongitude() != null ? commune.getLongitude().doubleValue() : null,
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
                weather.getApparentTemperature(),
                weather.getPrecipitation(),
                weather.getRain(),
                weather.getShowers(),
                weather.getSnowfall(),
                weather.getCloudCover(),
                weather.getWindGusts(),
                weather.getPressureMsl(),
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
     * Builds data points from air quality and weather data lists with optional indicator filtering.
     * 
     * Filters data points to include only the requested indicators.
     * Valid air quality indicators: aqi, pm25, pm10, no2, o3, so2
     * Valid weather indicators: temperature, humidity, windSpeed, windDirection, pressure
     * 
     * Merges air quality and weather data chronologically, ensuring all dates from both
     * datasets are represented in the output (even if one has data and the other doesn't).
     * 
     * @param airQualityList list of air quality data
     * @param weatherList list of weather data
     * @param indicatorsFilter optional comma-separated indicator filter (null or empty returns all)
     * @return filtered list of data points sorted by timestamp
     */
    private List<DataPoint> buildDataPoints(List<AirQuality> airQualityList, List<WeatherData> weatherList, String indicatorsFilter) {
        List<DataPoint> dataPoints = new ArrayList<>();

        // Parse and validate indicators filter
        Set<String> requestedIndicators = parseIndicators(indicatorsFilter);
        boolean hasAirQualityIndicators = requestedIndicators.stream().anyMatch(ind -> 
            ind.matches("aqi|pm25|pm10|no2|o3|so2"));
        boolean hasWeatherIndicators = requestedIndicators.stream().anyMatch(ind -> 
            ind.matches("temperature|humidity|windspeed|winddirection|weathercode"));

        // If no indicators specified, include all
        if (requestedIndicators.isEmpty()) {
            hasAirQualityIndicators = true;
            hasWeatherIndicators = true;
        }

        // Create maps for easy lookup by date
        java.util.Map<LocalDate, AirQuality> airQualityByDate = new java.util.HashMap<>();
        airQualityList.forEach(aq -> airQualityByDate.putIfAbsent(aq.getMeasurementDate(), aq));

        java.util.Map<LocalDate, WeatherData> weatherByDate = new java.util.HashMap<>();
        weatherList.forEach(w -> weatherByDate.putIfAbsent(w.getMeasurementDate(), w));

        // Collect all unique dates from both datasets
        Set<LocalDate> allDates = new java.util.TreeSet<>();
        allDates.addAll(airQualityByDate.keySet());
        allDates.addAll(weatherByDate.keySet());

        // Build data points for all dates, merging both datasets
        for (LocalDate date : allDates) {
            AirQuality aq = airQualityByDate.get(date);
            WeatherData weather = weatherByDate.get(date);

            // Build air quality data point (with filtering if needed)
            AirQualityDataPoint aqPoint = null;
            if (hasAirQualityIndicators && aq != null) {
                aqPoint = filterAirQualityDataPoint(aq, requestedIndicators);
            }

            // Build weather data point (with filtering if needed)
            WeatherDataPoint weatherPoint = null;
            if (hasWeatherIndicators && weather != null) {
                weatherPoint = filterWeatherDataPoint(weather, requestedIndicators);
            }

            // Only add data point if it has at least one requested indicator
            if (aqPoint != null || weatherPoint != null) {
                DataPoint dataPoint = new DataPoint(
                        date.atStartOfDay(),
                        aqPoint,
                        weatherPoint
                );
                dataPoints.add(dataPoint);
            }
        }

        return dataPoints;
    }

    /**
     * Parses indicator filter string into a set of individual indicators.
     * 
     * @param indicatorsFilter comma-separated indicator string (e.g., "aqi,pm25,temperature")
     * @return set of parsed indicators (empty if filter is null or empty)
     */
    private Set<String> parseIndicators(String indicatorsFilter) {
        if (indicatorsFilter == null || indicatorsFilter.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        return Arrays.stream(indicatorsFilter.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    /**
     * Filters air quality data point to include only requested indicators.
     * 
     * @param aq air quality entity
     * @param requestedIndicators set of requested indicators
     * @return filtered AirQualityDataPoint with null values for non-requested fields
     */
    private AirQualityDataPoint filterAirQualityDataPoint(AirQuality aq, Set<String> requestedIndicators) {
        // If no indicators specified or "aqi" is requested, include AQI fields; otherwise null
        Integer aqi = (requestedIndicators.isEmpty() || requestedIndicators.contains("aqi")) ? aq.getAtmIndex() : null;
        String qualifier = (requestedIndicators.isEmpty() || requestedIndicators.contains("aqi")) ? aq.getAtmoQual() : null;
        String color = (requestedIndicators.isEmpty() || requestedIndicators.contains("aqi")) ? aq.getAtmoColor() : null;

        // Include individual pollutants if requested
        Integer no2 = (requestedIndicators.isEmpty() || requestedIndicators.contains("no2")) ? aq.getNO2() : null;
        Integer o3 = (requestedIndicators.isEmpty() || requestedIndicators.contains("o3")) ? aq.getO3() : null;
        Integer pm10 = (requestedIndicators.isEmpty() || requestedIndicators.contains("pm10")) ? aq.getPm10() : null;
        Integer pm25 = (requestedIndicators.isEmpty() || requestedIndicators.contains("pm25")) ? aq.getPm25() : null;
        Integer so2 = (requestedIndicators.isEmpty() || requestedIndicators.contains("so2")) ? aq.getSO2() : null;

        return new AirQualityDataPoint(aqi, qualifier, color, no2, o3, pm10, pm25, so2);
    }

    /**
     * Filters weather data point to include only requested indicators.
     * 
     * @param weather weather data entity
     * @param requestedIndicators set of requested indicators
     * @return filtered WeatherDataPoint with null values for non-requested fields
     */
    private WeatherDataPoint filterWeatherDataPoint(WeatherData weather, Set<String> requestedIndicators) {
        // Include weather indicators if requested or if no filter specified
        Double temperature = (requestedIndicators.isEmpty() || requestedIndicators.contains("temperature")) ? weather.getTemperature() : null;
        Integer humidity = (requestedIndicators.isEmpty() || requestedIndicators.contains("humidity")) ? weather.getHumidity() : null;
        Double windSpeed = (requestedIndicators.isEmpty() || requestedIndicators.contains("windspeed")) ? weather.getWindSpeed() : null;
        Integer windDirection = (requestedIndicators.isEmpty() || requestedIndicators.contains("winddirection")) ? weather.getWindDirection() : null;
        Integer weatherCode = (requestedIndicators.isEmpty() || requestedIndicators.contains("weathercode")) ? weather.getWeatherCode() : null;
        Double apparentTemperature = (requestedIndicators.isEmpty() || requestedIndicators.contains("apparenttemperature")) ? weather.getApparentTemperature() : null;
        Double precipitation = (requestedIndicators.isEmpty() || requestedIndicators.contains("precipitation")) ? weather.getPrecipitation() : null;
        Double rain = (requestedIndicators.isEmpty() || requestedIndicators.contains("rain")) ? weather.getRain() : null;
        Double showers = (requestedIndicators.isEmpty() || requestedIndicators.contains("showers")) ? weather.getShowers() : null;
        Double snowfall = (requestedIndicators.isEmpty() || requestedIndicators.contains("snowfall")) ? weather.getSnowfall() : null;
        Integer cloudCover = (requestedIndicators.isEmpty() || requestedIndicators.contains("cloudcover")) ? weather.getCloudCover() : null;
        Double windGusts = (requestedIndicators.isEmpty() || requestedIndicators.contains("windgusts")) ? weather.getWindGusts() : null;
        Double pressureMsl = (requestedIndicators.isEmpty() || requestedIndicators.contains("pressure")) ? weather.getPressureMsl() : null;

        return new WeatherDataPoint(
                temperature,
                humidity,
                windSpeed,
                windDirection,
                weatherCode,
                apparentTemperature,
                precipitation,
                rain,
                showers,
                snowfall,
                cloudCover,
                windGusts,
                pressureMsl
        );
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
