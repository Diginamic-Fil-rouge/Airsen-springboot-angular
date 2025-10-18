package fr.airsen.api.repository;

import fr.airsen.api.entity.WeatherData;
import fr.airsen.api.entity.Commune;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing WeatherData entities.
 * 
 * Provides data access methods for weather measurements
 * with custom queries for temporal and geographic searches.
 */
@Repository
public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {

    /**
     * Finds weather data by commune and measurement date.
     * 
     * @param commune the commune
     * @param measurementDate the measurement date
     * @return optional weather data
     */
    Optional<WeatherData> findByCommuneAndMeasurementDate(Commune commune, LocalDate measurementDate);

    /**
     * Finds weather data by commune ID.
     * 
     * @param communeId commune identifier
     * @param pageable pagination parameters
     * @return page of weather data for the commune
     */
    @Query("SELECT w FROM WeatherData w WHERE w.commune.id = :communeId ORDER BY w.measurementDate DESC")
    Page<WeatherData> findByCommuneId(@Param("communeId") Long communeId, Pageable pageable);

    /**
     * Finds latest weather data for a commune.
     * 
     * @param communeId commune identifier
     * @return optional latest weather data
     */
    @Query("SELECT w FROM WeatherData w WHERE w.commune.id = :communeId ORDER BY w.measurementDate DESC")
    Optional<WeatherData> findLatestByCommuneId(@Param("communeId") Long communeId);

    /**
     * Finds weather data by commune ID and date range.
     * 
     * @param communeId commune identifier
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @param pageable pagination parameters
     * @return page of weather data within date range
     */
    @Query("SELECT w FROM WeatherData w WHERE w.commune.id = :communeId AND w.measurementDate BETWEEN :startDate AND :endDate ORDER BY w.measurementDate DESC")
    Page<WeatherData> findByCommuneIdAndDateRange(
            @Param("communeId") Long communeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Finds weather data for multiple communes on a specific date.
     * 
     * @param communeIds list of commune identifiers
     * @param measurementDate measurement date
     * @return list of weather data
     */
    @Query("SELECT w FROM WeatherData w WHERE w.commune.id IN :communeIds AND w.measurementDate = :measurementDate")
    List<WeatherData> findByCommuneIdsAndDate(
            @Param("communeIds") List<Long> communeIds,
            @Param("measurementDate") LocalDate measurementDate);

    /**
     * Finds weather data by temperature range.
     * 
     * @param minTemperature minimum temperature
     * @param maxTemperature maximum temperature
     * @param pageable pagination parameters
     * @return page of weather data within temperature range
     */
    @Query("SELECT w FROM WeatherData w WHERE w.temperature BETWEEN :minTemp AND :maxTemp ORDER BY w.measurementDate DESC")
    Page<WeatherData> findByTemperatureRange(
            @Param("minTemp") Double minTemperature,
            @Param("maxTemp") Double maxTemperature,
            Pageable pageable);

    /**
     * Counts weather data entries for a commune.
     * 
     * @param communeId commune identifier
     * @return number of weather data entries
     */
    @Query("SELECT COUNT(w) FROM WeatherData w WHERE w.commune.id = :communeId")
    long countByCommuneId(@Param("communeId") Long communeId);

    /**
     * Finds weather data by commune for API integration.
     * 
     * @param commune the commune
     * @return list of weather data ordered by date
     */
    @Query("SELECT w FROM WeatherData w WHERE w.commune = :commune ORDER BY w.measurementDate DESC")
    List<WeatherData> findByCommune(@Param("commune") Commune commune);

    /**
     * Finds current weather for external API integration.
     * 
     * @param commune the commune
     * @return optional current weather data
     */
    @Query("SELECT w FROM WeatherData w WHERE w.commune = :commune ORDER BY w.measurementDate DESC")
    Optional<WeatherData> findCurrentWeatherByCommune(@Param("commune") Commune commune);

    /**
     * Gets weather forecast for external API integration.
     * 
     * @param commune the commune
     * @param startDate start date for forecast
     * @return list of weather forecast data
     */
    @Query("SELECT w FROM WeatherData w WHERE w.commune = :commune AND w.measurementDate >= :startDate ORDER BY w.measurementDate ASC")
    List<WeatherData> findWeatherForecastByCommune(
            @Param("commune") Commune commune,
            @Param("startDate") LocalDate startDate);

    /**
     * Deletes old weather data for cleanup.
     * 
     * @param cutoffDate date before which data will be deleted
     */
    @Query("DELETE FROM WeatherData w WHERE w.createdAt < :cutoffDate")
    void deleteOldWeatherData(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Finds the most recent weather data entry for a commune by INSEE code.
     * 
     * @param inseeCode commune INSEE code
     * @return optional latest weather data
     */
    @Query(value = "SELECT w.id, w.commune_id, w.measurement_date, w.temperature, w.humidity, " +
           "w.wind_speed, w.wind_direction, w.weather_code, w.created_at " +
           "FROM weather_data w JOIN communes c ON w.commune_id = c.id " +
           "WHERE c.insee_code = :inseeCode ORDER BY w.measurement_date DESC, w.created_at DESC LIMIT 1", 
           nativeQuery = true)
    Optional<WeatherData> getMostRecentWeatherByInseeCode(@Param("inseeCode") String inseeCode);

    /**
     * Finds weather data by commune INSEE code and date range property access.
     * 
     * @param inseeCode commune INSEE code
     * @param startDateTime start date time
     * @param endDateTime end date time
     * @return list of weather data within date range
     */
    List<WeatherData> findByCommune_InseeCodeAndMeasurementDateBetween(String inseeCode, java.time.LocalDateTime startDateTime, java.time.LocalDateTime endDateTime);

    /**
     * Finds latest weather data for a commune for export endpoint.
     * 
     * Optimized query for export/export-data endpoint that retrieves
     * the most recent weather measurement in a single query.
     * 
     * @param inseeCode commune INSEE code
     * @return optional weather data
     */
    @Query(value = "SELECT w.* FROM weather_data w " +
                   "JOIN communes c ON w.commune_id = c.id " +
                   "WHERE c.insee_code = :inseeCode " +
                   "ORDER BY w.measurement_date DESC " +
                   "LIMIT 1", 
           nativeQuery = true)
    Optional<WeatherData> findLatestExportDataByInseeCode(@Param("inseeCode") String inseeCode);

    /**
     * Finds weather data for a commune within a date range for export endpoint.
     * 
     * Optimized query for export/historical-data endpoint that retrieves
     * all weather measurements within a date range in chronological order.
     * 
     * @param inseeCode commune INSEE code
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of weather data ordered by date
     */
    @Query("SELECT w FROM WeatherData w " +
           "WHERE w.commune.inseeCode = :inseeCode " +
           "AND w.measurementDate BETWEEN :startDate AND :endDate " +
           "ORDER BY w.measurementDate ASC")
    List<WeatherData> findHistoricalExportDataByInseeCode(
            @Param("inseeCode") String inseeCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
