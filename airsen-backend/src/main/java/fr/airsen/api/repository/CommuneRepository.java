package fr.airsen.api.repository;

import fr.airsen.api.entity.Commune;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Commune entities.
 *
 * Provides data access methods for administrative communes
 * with custom queries for geographic searches and hierarchical relationships
 */

@Repository
public interface CommuneRepository extends JpaRepository<Commune, Long> {

    /**
     * Finds commune by INSEE code.
     *
     * @param inseeCode INSEE code of the commune
     * @return optional commune
     */
    Optional<Commune> findByInseeCode(String inseeCode);

    /**
     * Finds commune by INSEE code with eager loading of department and region.
     *
     * @param inseeCode INSEE code of the commune
     * @return optional commune with department and region loaded
     */
    @Query("SELECT c FROM Commune c " +
        "LEFT JOIN FETCH c.department d " +
        "LEFT JOIN FETCH d.region r " +
        "WHERE c.inseeCode = :inseeCode")
    Optional<Commune> findByInseeCodeWithEagerLoading(@Param("inseeCode") String inseeCode);

    /**
     * Finds communes by name (case-insensitive).
     *
     * @param name commune name
     * @param pageable pagination parameters
     * @return page of communes matching the name
     */
    Page<Commune> findByNameContainingIgnoreCase(String name, Pageable pageable);

    /**
     * Finds communes by department ID.
     *
     * @param departmentId department identifier
     * @param pageable pagination parameters
     * @return page of communes in the department
     */
    @Query("SELECT c FROM Commune c WHERE c.department.id = :departmentId")
    Page<Commune> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

    /**
     *  List communes by department ID (returning List instead of Page).
     *
     * @param departmentId department identifier
     * @param pageable pagination parameters
     * @return list of communes in the department
     */
    @Query("SELECT c FROM Commune c WHERE c.department.id = :departmentId")
    List<Commune> findByDepartmentIdAsList(@Param("departmentId") Long departmentId, Pageable pageable);

    /**
     * Finds communes by department with latest air quality data.
     *
     * @param departmentId department identifier
     * @param pageable pagination parameters
     * @return list of object arrays: [Commune, atmoIndex, qualifier, color]
     */
    @Query("SELECT c, aq.atmIndex, aq.atmoQual, aq.atmoColor " +
        "FROM Commune c " +
        "LEFT JOIN c.airQuality aq " +
        "WHERE c.department.id = :departmentId " +
        "AND (aq.measurementDate = (" +
        "    SELECT MAX(aq2.measurementDate) " +
        "    FROM AirQuality aq2 " +
        "    WHERE aq2.commune.id = c.id" +
        ") OR aq IS NULL) " +
        w"ORDER BY c.name ASC")
    List<Object[]> findByDepartmentIdWithLatestAirQuality(
        @Param("departmentId") Long departmentId,
        Pageable pageable
    );

    /**
     * Searches communes by name with latest air quality data.
     *
     * @param departmentId department identifier
     * @param search search term for commune name (case-insensitive)
     * @param pageable pagination parameters
     * @return list of object arrays: [Commune, atmoIndex, qualifier, color]
     */
    @Query("SELECT c, aq.atmIndex, aq.atmoQual, aq.atmoColor " +
           "FROM Commune c " +
           "LEFT JOIN c.airQuality aq " +
           "WHERE c.department.id = :departmentId " +
           "AND LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "AND (aq.measurementDate = (" +
           "    SELECT MAX(aq2.measurementDate) " +
           "    FROM AirQuality aq2 " +
           "    WHERE aq2.commune.id = c.id" +
           ") OR aq IS NULL) " +
           "ORDER BY c.name ASC")
    List<Object[]> findByDepartmentIdAndNameWithLatestAirQuality(
        @Param("departmentId") Long departmentId,
        @Param("search") String search,
        Pageable pageable
    );

    /**
     * Searches communes by name or INSEE code with relevance-based ordering.
     *
     * Ordering priority:
     * 1. Exact matches (case-insensitive)
     * 2. Starts with search term
     * 3. Contains search term
     * 4. Alphabetically by name
     *
     * @param searchTerm search term to match against name or INSEE code
     * @param pageable pagination parameters
     * @return page of matching communes ordered by relevance
     */
    @Query("SELECT c FROM Commune c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR c.inseeCode LIKE CONCAT('%', :searchTerm, '%') " +
           "ORDER BY " +
           "CASE " +
           "  WHEN LOWER(c.name) = LOWER(:searchTerm) THEN 1 " +
           "  WHEN LOWER(c.name) LIKE LOWER(CONCAT(:searchTerm, '%')) THEN 2 " +
           "  WHEN c.inseeCode = :searchTerm THEN 3 " +
           "  ELSE 4 " +
           "END, " +
           "c.name ASC")
    Page<Commune> searchByNameOrInseeCode(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Finds communes by department with name filter.
     *
     * @param departmentId department identifier
     * @param nameFilter name filter (case-insensitive)
     * @param pageable pagination parameters
     * @return page of matching communes in the department
     */
    @Query("SELECT c FROM Commune c WHERE c.department.id = :departmentId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :nameFilter, '%'))")
    Page<Commune> findByDepartmentIdAndNameContaining(
            @Param("departmentId") Long departmentId,
            @Param("nameFilter") String nameFilter,
            Pageable pageable);

    List<Commune> findByDepartmentIdAndNameContainingIgnoreCase(Long departmentId, String name, Pageable pageable);

    /**
     * Finds communes within a bounding box.
     *
     * @param minLatitude minimum latitude
     * @param maxLatitude maximum latitude
     * @param minLongitude minimum longitude
     * @param maxLongitude maximum longitude
     * @param pageable pagination parameters
     * @return page of communes within the geographic bounds
     */
    @Query("SELECT c FROM Commune c WHERE c.latitude BETWEEN :minLat AND :maxLat AND c.longitude BETWEEN :minLng AND :maxLng")
    Page<Commune> findCommunesInBoundingBox(
            @Param("minLat") Double minLatitude,
            @Param("maxLat") Double maxLatitude,
            @Param("minLng") Double minLongitude,
            @Param("maxLng") Double maxLongitude,
            Pageable pageable);

    /**
     * Counts communes by department.
     *
     * @param departmentId department identifier
     * @return number of communes in the department
     */
    @Query("SELECT COUNT(c) FROM Commune c WHERE c.department.id = :departmentId")
    long countByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * Finds communes that have air quality data.
     *
     * @param pageable pagination parameters
     * @return page of communes with air quality measurements
     */
    @Query("SELECT DISTINCT c FROM Commune c WHERE EXISTS (SELECT 1 FROM AirQuality aq WHERE aq.commune = c)")
    Page<Commune> findCommunesWithAirQualityData(Pageable pageable);

    /**
     * Finds communes that have weather data.
     *
     * @param pageable pagination parameters
     * @return page of communes with weather measurements
     */
    @Query("SELECT DISTINCT c FROM Commune c WHERE EXISTS (SELECT 1 FROM WeatherData wd WHERE wd.commune = c)")
    Page<Commune> findCommunesWithWeatherData(Pageable pageable);

    /**
     * Finds communes by region.
     *
     * @param regionId region identifier
     * @param pageable pagination parameters
     * @return page of communes in the region
     */
    @Query("SELECT c FROM Commune c WHERE c.department.region.id = :regionId")
    Page<Commune> findByRegionId(@Param("regionId") Long regionId, Pageable pageable);

    /**
     * Finds most populated communes.
     *
     * @param pageable pagination parameters for limiting results
     * @return page of communes ordered by population descending
     */
    @Query("SELECT c FROM Commune c WHERE c.population IS NOT NULL ORDER BY c.population DESC")
    Page<Commune> findMostPopulatedCommunes(Pageable pageable);

    /**
     * Gets communes with coordinates for mapping.
     * Filters out invalid coordinates (NULL, 0, or out of valid France bounds).
     * France approximate bounds: latitude 41-51°N, longitude -5-10°E
     *
     * @return list of communes with valid latitude and longitude
     */
    @Query("SELECT c FROM Commune c WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL " +
           "AND c.latitude <> 0 AND c.longitude <> 0 " +
           "AND c.latitude BETWEEN 41 AND 51 AND c.longitude BETWEEN -5 AND 10")
    List<Commune> findCommunesWithCoordinates();

    /**
     * Gets communes with valid coordinates and latest air quality data for map display.
     *
     * @return list of object arrays containing [Commune entity, atmoIndex, qualifier, color]
     */
    @Query("SELECT c, aq.atmIndex, aq.atmoQual, aq.atmoColor " +
           "FROM Commune c " +
           "LEFT JOIN c.airQuality aq " +
           "WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL " +
           "AND c.latitude <> 0 AND c.longitude <> 0 " +
           "AND c.latitude BETWEEN 41 AND 51 AND c.longitude BETWEEN -5 AND 10 " +
           "AND (aq.measurementDate = (" +
           "    SELECT MAX(aq2.measurementDate) " +
           "    FROM AirQuality aq2 " +
           "    WHERE aq2.commune.id = c.id" +
           ") OR aq IS NULL)")
    List<Object[]> findCommunesWithCoordinatesAndAirQuality();

    /**
     * Gets communes with valid coordinates and latest air quality data, filtered by minimum population.
     * Used for progressive map loading (show major cities first).
     *
     * @param minPopulation Minimum population threshold for filtering
     * @return list of object arrays containing [Commune entity, atmoIndex, qualifier, color]
     */
    @Query("SELECT c, aq.atmIndex, aq.atmoQual, aq.atmoColor " +
           "FROM Commune c " +
           "LEFT JOIN c.airQuality aq " +
           "WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL " +
           "AND c.latitude <> 0 AND c.longitude <> 0 " +
           "AND c.latitude BETWEEN 41 AND 51 AND c.longitude BETWEEN -5 AND 10 " +
           "AND c.population >= :minPopulation " +
           "AND (aq.measurementDate = (" +
           "    SELECT MAX(aq2.measurementDate) " +
           "    FROM AirQuality aq2 " +
           "    WHERE aq2.commune.id = c.id" +
           ") OR aq IS NULL) " +
           "ORDER BY c.population DESC")
    List<Object[]> findCommunesWithCoordinatesAndAirQualityByMinPopulation(@Param("minPopulation") Integer minPopulation);

    /**
     * Finds communes by department ID (for external API integration).
     *
     * @param departmentId department identifier
     * @return list of communes in the department
     */
    @Query("SELECT c FROM Commune c WHERE c.department.id = :departmentId")
    List<Commune> findByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * Finds commune by name for INSEE API integration.
     *
     * @param name exact commune name
     * @return optional commune
     */
    Optional<Commune> findByName(String name);

    /**
     * Gets coordinates for a commune by INSEE code.
     *
     * @param inseeCode INSEE code of the commune
     * @return optional commune with coordinates
     */
    @Query("SELECT c FROM Commune c WHERE c.inseeCode = :inseeCode AND c.latitude IS NOT NULL AND c.longitude IS NOT NULL")
    Optional<Commune> findCommuneCoordinatesByInseeCode(@Param("inseeCode") String inseeCode);

    /**
     * Finds all communes for data synchronization.
     *
     * @return list of all communes
     */
    @Query("SELECT c FROM Commune c ORDER BY c.name")
    List<Commune> findAllOrderByName();

    /**
     * Finds communes by department code for INSEE integration.
     *
     * Uses Commune.departmentCode field directly for better performance.
     *
     * @param departmentCode department code
     * @return list of communes in the department
     */
    List<Commune> findByDepartmentCode(String departmentCode);

    /**
     * Finds communes by department using department entity relationship.
     *
     * @param departmentCode department code as integer
     * @return list of communes in the department
     */
    @Query("SELECT c FROM Commune c WHERE c.department.departmentCode = :departmentCode")
    List<Commune> findByDepartmentEntityCode(@Param("departmentCode") String departmentCode);

    /**
     * Finds communes by region using region code property access.
     *
     * Uses Commune.regionCode field directly for better performance.
     *
     * @param regionCode region code
     * @return list of communes in the region
     */
    List<Commune> findByRegionCode(String regionCode);

    /**
     * Finds all communes belonging to a specific EPCI by SIREN code.
     *
     * Multiple communes can share the same EPCI (intercommunal grouping).
     * Returns empty list if EPCI code not found or no communes associated.
     * Uses database index on epci_code for optimized lookups.
     *
     * @param epciCode 9-digit SIREN code for EPCI
     * @return list of communes in the EPCI grouping, ordered by name
     */
    List<Commune> findByEpciCode(String epciCode);

    /**
     * Finds communes matching EITHER INSEE code (exact match) OR EPCI code (grouping).
     *
     * Use case: External API returns zone code without type indicator.
     * Returns single commune if INSEE match, multiple if EPCI match.
     *
     * Pattern: Dual OR clause leverages both indexes (insee_code, epci_code).
     *
     * @param code 5-digit INSEE code OR 9-digit EPCI SIREN code
     * @return list of matching communes (1 for INSEE, 0-50 for EPCI)
     */
    @Query("""
        SELECT c FROM Commune c
        WHERE c.inseeCode = :code OR c.epciCode = :code
        ORDER BY c.name ASC
        """)
    List<Commune> findByInseeCodeOrEpciCode(@Param("code") String code);

    /**
     * Finds communes by EPCI code with non-null coordinates for geodistance calculations.
     *
     * Used when target commune has no direct air quality data and belongs to an EPCI.
     * Filters out invalid coordinates (NULL, 0, out of France bounds).
     * Orders by population DESC: prefer larger communes for EPCI-level data.
     *
     * @param epciCode 9-digit EPCI SIREN code
     * @return list of communes with valid coordinates, or empty if none found
     */
    @Query("""
        SELECT c FROM Commune c
        WHERE c.epciCode = :epciCode
          AND c.latitude IS NOT NULL
          AND c.longitude IS NOT NULL
          AND c.latitude <> 0 AND c.longitude <> 0
          AND c.latitude BETWEEN 41 AND 51
          AND c.longitude BETWEEN -5 AND 10
        ORDER BY c.population DESC
        """)
    List<Commune> findByEpciCodeWithCoordinates(@Param("epciCode") String epciCode);

    /**
     * Finds communes by EPCI code with eager loading of department and region relationships.
     *
     * Prevents LazyInitializationException when accessing commune.department.region
     * outside transactional context (e.g., DTOs, cache serialization).
     * Single query with 2 JOINs (better than N+1 lazy loading).
     *
     * @param epciCode 9-digit EPCI SIREN code
     * @return list of communes with fully initialized relationships
     */
    @Query("""
        SELECT c FROM Commune c
        LEFT JOIN FETCH c.department d
        LEFT JOIN FETCH d.region r
        WHERE c.epciCode = :epciCode
        ORDER BY c.name ASC
        """)
    List<Commune> findByEpciCodeWithEagerLoading(@Param("epciCode") String epciCode);

    /**
     * Finds Tier 1 communes (population >= 100,000).
     *
     * Tier 1 communes are high-priority targets for frequent cache refresh
     * due to their importance and resource availability.
     *
     * @return list of all Tier 1 communes ordered by population descending
     */
    @Query("SELECT c FROM Commune c WHERE c.population >= 100000 ORDER BY c.population DESC")
    List<Commune> findTier1Communes();

    /**
     * Finds Tier 2 communes (population between 10,000 and 99,999).
     *
     * Tier 2 communes are medium-priority targets with moderate
     * cache refresh frequency.
     *
     * @return list of all Tier 2 communes ordered by population descending
     */
    @Query("SELECT c FROM Commune c WHERE c.population >= 10000 AND c.population < 100000 ORDER BY c.population DESC")
    List<Commune> findTier2Communes();

    /**
     * Finds Tier 3 communes (population < 10,000).
     *
     * Tier 3 communes are low-priority targets with infrequent
     * cache refresh to reduce API load.
     *
     * @return list of all Tier 3 communes ordered by population descending
     */
    @Query("SELECT c FROM Commune c WHERE c.population < 10000 ORDER BY c.population DESC")
    List<Commune> findTier3Communes();

    /**
     * Counts Tier 1 communes.
     *
     * @return number of communes with population >= 100,000
     */
    @Query("SELECT COUNT(c) FROM Commune c WHERE c.population >= 100000")
    long countTier1Communes();

    /**
     * Counts Tier 2 communes.
     *
     * @return number of communes with population between 10,000-99,999
     */
    @Query("SELECT COUNT(c) FROM Commune c WHERE c.population >= 10000 AND c.population < 100000")
    long countTier2Communes();

    /**
     * Counts Tier 3 communes.
     *
     * @return number of communes with population < 10,000
     */
    @Query("SELECT COUNT(c) FROM Commune c WHERE c.population < 10000")
    long countTier3Communes();

    /**
     * Find commune by INSEE code with coordinates for geodistance calculations.
     * Used when target commune has no direct data and needs fallback estimation.
     *
     * @param inseeCode INSEE code of the commune
     * @return Optional containing commune with non-null coordinates, or empty if not found
     */
    @Query("""
        SELECT c
        FROM Commune c
        WHERE c.inseeCode = :inseeCode
          AND c.latitude IS NOT NULL
          AND c.longitude IS NOT NULL
        """)
    Optional<Commune> findByInseeCodeWithCoordinates(@Param("inseeCode") String inseeCode);
}
