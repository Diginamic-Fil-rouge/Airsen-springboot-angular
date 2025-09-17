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
 * Provides data access methods for French administrative communes
 * with custom queries for geographic searches and hierarchical relationships
 * according to Airsens data model specifications.
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
    @Query("SELECT c FROM Commune c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Commune> findByNameContainingIgnoreCase(@Param("name") String name, Pageable pageable);

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
     * Finds communes by region (through department relationship).
     * 
     * @param regionId region identifier
     * @param pageable pagination parameters
     * @return page of communes in the region
     */
    @Query("SELECT c FROM Commune c WHERE c.department.region.id = :regionId")
    Page<Commune> findByRegionId(@Param("regionId") Long regionId, Pageable pageable);

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
     * Finds communes within a population range.
     * 
     * @param minPopulation minimum population
     * @param maxPopulation maximum population
     * @param pageable pagination parameters
     * @return page of communes within the population range
     */
    @Query("SELECT c FROM Commune c WHERE c.population BETWEEN :minPopulation AND :maxPopulation")
    Page<Commune> findByPopulationBetween(@Param("minPopulation") Long minPopulation, 
                                          @Param("maxPopulation") Long maxPopulation, 
                                          Pageable pageable);

    /**
     * Finds communes with population greater than specified value.
     * 
     * @param population minimum population threshold
     * @param pageable pagination parameters
     * @return page of communes with population above threshold
     */
    Page<Commune> findByPopulationGreaterThan(Long population, Pageable pageable);

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
     * Finds the largest communes by population.
     * 
     * @param limit maximum number of results
     * @return list of largest communes
     */
    @Query("SELECT c FROM Commune c ORDER BY c.population DESC")
    List<Commune> findTopByPopulation(Pageable pageable);

    /**
     * Finds communes by partial name match (for autocomplete).
     * 
     * @param namePrefix name prefix to match
     * @param limit maximum number of results
     * @return list of communes matching the prefix
     */
    @Query("SELECT c FROM Commune c WHERE LOWER(c.name) LIKE LOWER(CONCAT(:namePrefix, '%')) ORDER BY c.population DESC")
    List<Commune> findByNameStartingWithIgnoreCase(@Param("namePrefix") String namePrefix, Pageable pageable);

    /**
     * Checks if a commune exists by INSEE code.
     * 
     * @param inseeCode INSEE code to check
     * @return true if commune exists
     */
    boolean existsByInseeCode(String inseeCode);

    /**
     * Gets communes with coordinates for mapping.
     * 
     * @return list of communes with valid latitude and longitude
     */
    @Query("SELECT c FROM Commune c WHERE c.latitude IS NOT NULL AND c.longitude IS NOT NULL")
    List<Commune> findCommunesWithCoordinates();
}