package fr.airsen.api.service;

import fr.airsen.api.dto.response.NearestAirQualityResult;
import fr.airsen.api.dto.response.NearestWeatherResult;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.repository.AirQualityRepository;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.WeatherDataRepository;
import fr.airsen.api.util.JpqlResultConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Service for calculating geographic distances and finding nearest communes with data.
 * Uses the Haversine formula to calculate distances between coordinates.
 */
@Service
public class GeoDistanceService {

    private static final Logger log = LoggerFactory.getLogger(GeoDistanceService.class);

    private final CommuneRepository communeRepository;
    private final WeatherDataRepository weatherDataRepository;
    private final AirQualityRepository airQualityRepository;

    public GeoDistanceService(CommuneRepository communeRepository,
                              WeatherDataRepository weatherDataRepository,
                              AirQualityRepository airQualityRepository) {
        this.communeRepository = communeRepository;
        this.weatherDataRepository = weatherDataRepository;
        this.airQualityRepository = airQualityRepository;
    }

    /**
     * Earth's radius in kilometers.
     * Used for Haversine formula calculations.
     */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /**
     * PRD-defined maximum distance threshold in kilometers.
     * Data beyond this distance is considered irrelevant.
     */
    private static final double MAX_RELEVANCE_DISTANCE_KM = 20.0;

    /**
     * Calculate the distance between two geographic coordinates using the Haversine formula.
     *
     * @param lat1 Latitude of first point in degrees [-90, 90]
     * @param lon1 Longitude of first point in degrees [-180, 180]
     * @param lat2 Latitude of second point in degrees [-90, 90]
     * @param lon2 Longitude of second point in degrees [-180, 180]
     * @return Distance between points in kilometers
     * @throws IllegalArgumentException if coordinates are out of valid range
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Input validation
        validateCoordinate(lat1, lon1, "point1");
        validateCoordinate(lat2, lon2, "point2");

        // Convert degrees to radians
        double lat1Rad = Math.toRadians(lat1);
        double lon1Rad = Math.toRadians(lon1);
        double lat2Rad = Math.toRadians(lat2);
        double lon2Rad = Math.toRadians(lon2);

        // Haversine formula
        double dLat = lat2Rad - lat1Rad;
        double dLon = lon2Rad - lon1Rad;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        // Clamp 'a' to [0, 1] to handle floating point errors with antipodal points
        a = Math.max(0.0, Math.min(1.0, a));

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS_KM * c;

        log.debug("Calculated distance between ({}, {}) and ({}, {}): {:.2f} km",
                lat1, lon1, lat2, lon2, distance);

        return distance;
    }

    /**
     * Find the nearest commune with weather data within the maximum distance threshold.
     *
     * @param inseeCode INSEE code of the target commune
     * @param maxDistanceKm Maximum distance to search (typically 20km per PRD)
     * @return Optional containing nearest weather result, or empty if none within threshold
     */
    public Optional<NearestWeatherResult> findNearestCommuneWithWeather(
            String inseeCode, double maxDistanceKm) {

        log.info("Searching for nearest commune with weather data for INSEE {}, max distance: {} km",
                inseeCode, maxDistanceKm);

        // Get target commune coordinates
        Optional<Commune> targetCommune = communeRepository.findByInseeCodeWithCoordinates(inseeCode);

        if (targetCommune.isEmpty()) {
            log.warn("Target commune not found: {}", inseeCode);
            return Optional.empty();
        }

        Commune commune = targetCommune.get();

        if (commune.getLatitude() == null || commune.getLongitude() == null) {
            log.warn("Target commune {} has no coordinates", inseeCode);
            return Optional.empty();
        }

        // Query database for nearest commune with weather data
        Optional<Object[]> result = weatherDataRepository.findNearestCommuneWithWeather(
                commune.getLatitude().doubleValue(),
                commune.getLongitude().doubleValue(),
                maxDistanceKm
        );

        if (result.isEmpty()) {
            log.warn("No weather data found within {} km of commune {}", maxDistanceKm, inseeCode);
            return Optional.empty();
        }

        // Parse result array from native query
        Object[] row = result.get();

        // Handle nested Object[] - some JDBC drivers wrap native query results in an extra array
        JpqlResultConverter.UnwrapResult unwrapResult = JpqlResultConverter.unwrapNestedArray(row);
        row = unwrapResult.data;

        // Check if unwrapped array is empty
        if (row.length == 0) {
            log.warn("Unwrapped array is empty, no weather data found within {} km of commune {}",
                     maxDistanceKm, inseeCode);
            return Optional.empty();
        }

        NearestWeatherResult nearestResult = parseWeatherResult(row);

        log.info("Found nearest weather data: commune {} at distance {:.2f} km",
                nearestResult.communeName(), nearestResult.distanceKm());

        return Optional.of(nearestResult);
    }

    /**
     * Find the nearest commune with air quality data within the maximum distance threshold.
     *
     * @param inseeCode INSEE code of the target commune
     * @param maxDistanceKm Maximum distance to search (typically 20km per PRD)
     * @return Optional containing nearest air quality result, or empty if none within threshold
     */
    public Optional<NearestAirQualityResult> findNearestCommuneWithAirQuality(
            String inseeCode, double maxDistanceKm) {

        log.info("Searching for nearest commune with air quality data for INSEE {}, max distance: {} km",
                inseeCode, maxDistanceKm);

        // Get target commune coordinates
        Optional<Commune> targetCommune = communeRepository.findByInseeCodeWithCoordinates(inseeCode);

        if (targetCommune.isEmpty()) {
            log.warn("Target commune not found: {}", inseeCode);
            return Optional.empty();
        }

        Commune commune = targetCommune.get();

        if (commune.getLatitude() == null || commune.getLongitude() == null) {
            log.warn("Target commune {} has no coordinates", inseeCode);
            return Optional.empty();
        }

        // Query database for nearest commune with air quality data
        Optional<Object[]> result = airQualityRepository.findNearestCommuneWithAirQuality(
                commune.getLatitude().doubleValue(),
                commune.getLongitude().doubleValue(),
                maxDistanceKm
        );

        if (result.isEmpty()) {
            log.warn("No air quality data found within {} km of commune {}", maxDistanceKm, inseeCode);
            return Optional.empty();
        }

        // Parse result array from native query
        Object[] row = result.get();

        // Handle nested Object[] - some JDBC drivers wrap native query results in an extra array
        JpqlResultConverter.UnwrapResult unwrapResult = JpqlResultConverter.unwrapNestedArray(row);
        row = unwrapResult.data;

        // Check if unwrapped array is empty
        if (row.length == 0) {
            log.warn("Unwrapped array is empty, no air quality data found within {} km of commune {}",
                     maxDistanceKm, inseeCode);
            return Optional.empty();
        }

        NearestAirQualityResult nearestResult = parseAirQualityResult(row);

        log.info("Found nearest air quality data: commune {} at distance {:.2f} km",
                nearestResult.communeName(), nearestResult.distanceKm());

        return Optional.of(nearestResult);
    }

        /**
     * Validate that coordinates are within valid ranges.
     *
     * @param lat Latitude in degrees
     * @param lon Longitude in degrees
     * @param pointName Name of the point for error messages
     * @throws IllegalArgumentException if coordinates are invalid
     */
    private void validateCoordinate(double lat, double lon, String pointName) {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException(
                    String.format("Invalid latitude for %s: %.6f (must be between -90 and 90)",
                            pointName, lat));
        }
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException(
                    String.format("Invalid longitude for %s: %.6f (must be between -180 and 180)",
                            pointName, lon));
        }
    }

    /**
     * Parse native query result array into NearestWeatherResult.
     *
     * @param row Result array from native query
     * @return Parsed NearestWeatherResult
     */
    private NearestWeatherResult parseWeatherResult(Object[] row) {
        return new NearestWeatherResult(
            JpqlResultConverter.toStringNullable(row[0]),          // inseeCode (VARCHAR → String)
            JpqlResultConverter.toStringNullable(row[1]),          // communeName (VARCHAR → String)
            JpqlResultConverter.toDoubleNullable(row[2]),          // latitude (BigDecimal → Double)
            JpqlResultConverter.toDoubleNullable(row[3]),          // longitude (BigDecimal → Double)
            JpqlResultConverter.toLocalDateNullable(row[4]),       // measurementDate
            // Basic weather measurements
            JpqlResultConverter.toDoubleNullable(row[5]),          // temperature (BigDecimal → Double)
            JpqlResultConverter.toIntegerNullable(row[6]),         // humidity (INT → Integer)
            JpqlResultConverter.toDoubleNullable(row[7]),          // windSpeed (BigDecimal → Double)
            JpqlResultConverter.toIntegerNullable(row[8]),         // windDirection (INT → Integer)
            JpqlResultConverter.toIntegerNullable(row[9]),         // weatherCode (INT → Integer)
            // Advanced weather measurements
            JpqlResultConverter.toDoubleNullable(row[10]),         // apparentTemperature (BigDecimal → Double)
            JpqlResultConverter.toDoubleNullable(row[11]),         // precipitation (BigDecimal → Double)
            JpqlResultConverter.toDoubleNullable(row[12]),         // rain (BigDecimal → Double)
            JpqlResultConverter.toDoubleNullable(row[13]),         // showers (BigDecimal → Double)
            JpqlResultConverter.toDoubleNullable(row[14]),         // snowfall (BigDecimal → Double)
            JpqlResultConverter.toIntegerNullable(row[15]),        // cloudCover (INT → Integer)
            JpqlResultConverter.toDoubleNullable(row[16]),         // windGusts (BigDecimal → Double)
            JpqlResultConverter.toDoubleNullable(row[17]),         // pressureMsl (BigDecimal → Double)
            // Distance calculation result
            JpqlResultConverter.toDoubleNullable(row[18])          // distanceKm (BigDecimal → Double)
        );
    }

    /**
     * Parse native query result array into NearestAirQualityResult.
     *
     * @param row Result array from native query
     * @return Parsed NearestAirQualityResult
     */
    private NearestAirQualityResult parseAirQualityResult(Object[] row) {
        return new NearestAirQualityResult(
            JpqlResultConverter.toStringNullable(row[0]),          // inseeCode (VARCHAR → String)
            JpqlResultConverter.toStringNullable(row[1]),          // communeName (VARCHAR → String)
            JpqlResultConverter.toDoubleNullable(row[2]),          // latitude (BigDecimal → Double)
            JpqlResultConverter.toDoubleNullable(row[3]),          // longitude (BigDecimal → Double)
            JpqlResultConverter.toLocalDateNullable(row[4]),       // measurementDate
            JpqlResultConverter.toIntegerNullable(row[5]),         // atmoIndex (BigDecimal/Integer → Integer)
            JpqlResultConverter.toStringNullable(row[6]),          // qualifier (VARCHAR → String)
            JpqlResultConverter.toStringNullable(row[7]),          // color (VARCHAR → String)
            JpqlResultConverter.toIntegerNullable(row[8]),         // no2 (BigDecimal/Integer → Integer)
            JpqlResultConverter.toIntegerNullable(row[9]),         // o3 (BigDecimal/Integer → Integer)
            JpqlResultConverter.toIntegerNullable(row[10]),        // pm10 (BigDecimal/Integer → Integer)
            JpqlResultConverter.toIntegerNullable(row[11]),        // pm25 (BigDecimal/Integer → Integer)
            JpqlResultConverter.toIntegerNullable(row[12]),        // so2 (BigDecimal/Integer → Integer)
            JpqlResultConverter.toDoubleNullable(row[13])          // distanceKm (BigDecimal → Double)
        );
    }
}
