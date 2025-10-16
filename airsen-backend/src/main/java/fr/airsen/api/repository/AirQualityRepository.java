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
}
