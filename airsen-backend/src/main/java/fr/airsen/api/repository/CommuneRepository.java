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
     * Finds communes by name (case insensitive).
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
     * Robincassan's method: List communes by department ID (returning List instead of Page).
     * 
     * @param departmentId department identifier
     * @param pageable pagination parameters
     * @return list of communes in the department
     */
    @Query("SELECT c FROM Commune c WHERE c.department.id = :departmentId")
    List<Commune> findByDepartmentIdAsList(@Param("departmentId") Long departmentId, Pageable pageable);

    /**
     * Searches communes by name or INSEE code.
     * 
     * @param searchTerm search term to match against name or INSEE code
     * @param pageable pagination parameters
     * @return page of matching communes
     */
    @Query("SELECT c FROM Commune c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR c.inseeCode LIKE CONCAT('%', :searchTerm, '%')")
    Page<Commune> searchByNameOrInseeCode(@Param("searchTerm") String searchTerm, Pageable pageable);

    /**
     * Finds communes by department with name filter.
     * 
     * @param departmentId department identifier
     * @param nameFilter name filter (case insensitive)
     * @param pageable pagination parameters
     * @return page of matching communes in the department
     */
    @Query("SELECT c FROM Commune c WHERE c.department.id = :departmentId AND LOWER(c.name) LIKE LOWER(CONCAT('%', :nameFilter, '%'))")
    Page<Commune> findByDepartmentIdAndNameContaining(
            @Param("departmentId") Long departmentId, 
            @Param("nameFilter") String nameFilter, 
            Pageable pageable);

    /**
     * @param departmentId department identifier
     * @param name name filter (case insensitive)
     * @param pageable pagination parameters
     * @return list of matching communes in the department
     */
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
     * 
     * @return list of communes with valid latitude and longitude
     */
    @Query("SELECT c FROM Commune c WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL")
    List<Commune> findCommunesWithCoordinates();
}