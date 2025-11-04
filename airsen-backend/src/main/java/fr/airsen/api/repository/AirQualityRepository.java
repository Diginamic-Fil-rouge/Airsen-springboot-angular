package fr.airsen.api.repository;

import fr.airsen.api.entity.AirQuality;
import fr.airsen.api.entity.Commune;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing AirQuality entities.
 *
 * Provides data access methods for air quality measurements
 * with custom queries for temporal and geographic searches.
 */
@Repository
public interface AirQualityRepository extends JpaRepository<AirQuality, Long> {

    /**
     * Finds air quality data by commune and measurement date.
     *
     * @param commune the commune
     * @param measurementDate the measurement date
     * @return optional air quality data
     */
    Optional<AirQuality> findByCommuneAndMeasurementDate(Commune commune, LocalDate measurementDate);

    /**
     * Finds air quality data by commune and measurement date with eager loading.
     *
     * @param commune the commune
     * @param measurementDate the measurement date
     * @return optional air quality data with relationships loaded
     */
    @Query("SELECT aq FROM AirQuality aq " +
           "LEFT JOIN FETCH aq.commune c " +
           "LEFT JOIN FETCH c.department d " +
           "LEFT JOIN FETCH d.region r " +
           "WHERE c = :commune AND aq.measurementDate = :measurementDate")
    Optional<AirQuality> findByCommuneAndMeasurementDateWithEagerLoading(@Param("commune") Commune commune, @Param("measurementDate") LocalDate measurementDate);

    /**
     * Finds air quality data by commune ID.
     *
     * @param communeId commune identifier
     * @param pageable pagination parameters
     * @return page of air quality data for the commune
     */
    @Query("SELECT aq FROM AirQuality aq WHERE aq.commune.id = :communeId ORDER BY aq.measurementDate DESC")
    Page<AirQuality> findByCommuneId(@Param("communeId") Long communeId, Pageable pageable);

    /**
     * Finds latest air quality data for a commune.
     *
     * @param communeId commune identifier
     * @return optional latest air quality data
     */
    @Query("SELECT aq FROM AirQuality aq WHERE aq.commune.id = :communeId ORDER BY aq.measurementDate DESC")
    Optional<AirQuality> findLatestByCommuneId(@Param("communeId") Long communeId);

    /**
     * Finds latest air quality data by commune INSEE code with eager loading.
     *
     * @param inseeCode commune INSEE code
     * @return optional latest air quality data with commune, department, and region loaded
     */
    @Query("SELECT aq FROM AirQuality aq " +
           "LEFT JOIN FETCH aq.commune c " +
           "LEFT JOIN FETCH c.department d " +
           "LEFT JOIN FETCH d.region r " +
           "WHERE c.inseeCode = :inseeCode " +
           "ORDER BY aq.measurementDate DESC")
    List<AirQuality> findByCommune_InseeCodeWithEagerLoading(@Param("inseeCode") String inseeCode, Pageable pageable);

    default Optional<AirQuality> findLatestByCommune_InseeCode(String inseeCode) {
        List<AirQuality> results = findByCommune_InseeCodeWithEagerLoading(inseeCode, Pageable.ofSize(1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Finds air quality data by commune ID and date range.
     *
     * @param communeId commune identifier
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @param pageable pagination parameters
     * @return page of air quality data within date range
     */
    @Query("SELECT aq FROM AirQuality aq WHERE aq.commune.id = :communeId AND aq.measurementDate BETWEEN :startDate AND :endDate ORDER BY aq.measurementDate DESC")
    Page<AirQuality> findByCommuneIdAndDateRange(
            @Param("communeId") Long communeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable);

    /**
     * Finds air quality data for multiple communes on a specific date.
     *
     * @param communeIds list of commune identifiers
     * @param measurementDate measurement date
     * @return list of air quality data
     */
    @Query("SELECT aq FROM AirQuality aq WHERE aq.commune.id IN :communeIds AND aq.measurementDate = :measurementDate")
    List<AirQuality> findByCommuneIdsAndDate(
            @Param("communeIds") List<Long> communeIds,
            @Param("measurementDate") LocalDate measurementDate);

    /**
     * Counts air quality data entries for a commune.
     *
     * @param communeId commune identifier
     * @return number of air quality data entries
     */
    @Query("SELECT COUNT(aq) FROM AirQuality aq WHERE aq.commune.id = :communeId")
    long countByCommuneId(@Param("communeId") Long communeId);

    /**
     * Finds air quality data entries for a specific measurement date.
     *
     * @param measurementDate the measurement date
     * @return list of air quality data entries for the date
     */
    List<AirQuality> findByMeasurementDate(LocalDate measurementDate);

    /**
     * Counts air quality data entries for a specific measurement date.
     *
     * @param measurementDate the measurement date
     * @return number of air quality data entries for the date
     */
    long countByMeasurementDate(LocalDate measurementDate);

    /**
     * Finds air quality data by commune for API integration.
     *
     * @param commune the commune
     * @return list of air quality data ordered by date
     */
    @Query("SELECT aq FROM AirQuality aq WHERE aq.commune = :commune ORDER BY aq.measurementDate DESC")
    List<AirQuality> findByCommune(@Param("commune") Commune commune);

    /**
     * Deletes old air quality data for cleanup.
     *
     * @param cutoffDate date before which data will be deleted
     */
    @Query("DELETE FROM AirQuality aq WHERE aq.createdAt < :cutoffDate")
    void deleteOldAirQualityData(@Param("cutoffDate") LocalDate cutoffDate);

    /**
     * Finds latest air quality data for a commune for export endpoint.
     *
     * Optimized query for export/export-data endpoint that retrieves
     * the most recent air quality measurement in a single query.
     *
     * @param inseeCode commune INSEE code
     * @return optional air quality data
     */
    @Query(value = "SELECT aq.* FROM air_quality aq " +
                   "JOIN communes c ON aq.commune_id = c.id " +
                   "WHERE c.insee_code = :inseeCode " +
                   "ORDER BY aq.measurement_date DESC " +
                   "LIMIT 1",
           nativeQuery = true)
    Optional<AirQuality> findLatestExportDataByInseeCode(@Param("inseeCode") String inseeCode);

    /**
     * Finds air quality data for a commune within a date range for export endpoint.
     *
     * Optimized query for export/historical-data endpoint that retrieves
     * all air quality measurements within a date range in chronological order.
     *
     * @param inseeCode commune INSEE code
     * @param startDate start date (inclusive)
     * @param endDate end date (inclusive)
     * @return list of air quality data ordered by date
     */
    @Query("SELECT aq FROM AirQuality aq " +
           "WHERE aq.commune.inseeCode = :inseeCode " +
           "AND aq.measurementDate BETWEEN :startDate AND :endDate " +
           "ORDER BY aq.measurementDate ASC")
    List<AirQuality> findHistoricalExportDataByInseeCode(
            @Param("inseeCode") String inseeCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find nearest commune with air quality data within specified distance using Haversine formula.
     * Used for geodistance fallback when target commune has no direct air quality data.
     *
     * @param lat Target latitude in decimal degrees
     * @param lon Target longitude in decimal degrees
     * @param maxDistanceKm Maximum search radius in kilometers (typically 20km per PRD)
     * @return Optional containing result array with commune info, air quality data, and distance
     */
    @Query(value = """
        SELECT
            c.insee_code,
            c.name,
            c.latitude,
            c.longitude,
            aq.measurement_date,
            aq.atm_index,
            aq.atmo_qual,
            aq.atmo_color,
            aq.no2,
            aq.o3,
            aq.pm10,
            aq.pm25,
            aq.so2,
            (6371 * acos(
                cos(radians(:lat)) * cos(radians(c.latitude))
                * cos(radians(c.longitude) - radians(:lon))
                + sin(radians(:lat)) * sin(radians(c.latitude))
            )) AS distance_km
        FROM communes c
        INNER JOIN air_quality aq ON aq.commune_id = c.id
        WHERE aq.measurement_date = (
            SELECT MAX(aq2.measurement_date)
            FROM air_quality aq2
            WHERE aq2.commune_id = c.id
        )
        HAVING distance_km <= :maxDistanceKm
        ORDER BY distance_km ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<Object[]> findNearestCommuneWithAirQuality(
        @Param("lat") double lat,
        @Param("lon") double lon,
        @Param("maxDistanceKm") double maxDistanceKm
    );
}
