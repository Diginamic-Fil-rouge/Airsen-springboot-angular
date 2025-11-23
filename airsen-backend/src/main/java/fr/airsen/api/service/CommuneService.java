package fr.airsen.api.service;

import fr.airsen.api.dto.CommuneDTO;
import fr.airsen.api.dto.response.CommuneDetailResponse;
import fr.airsen.api.entity.*;
import fr.airsen.api.entity.cacheData.CacheMetadata;
import fr.airsen.api.exception.ResourceNotFoundException;
import fr.airsen.api.external.client.AtmoApiClient;
import fr.airsen.api.external.client.InseeApiClient;
import fr.airsen.api.external.client.OpenMeteoApiClient;
import fr.airsen.api.external.dto.insee.InseeCommuneResponse;
import fr.airsen.api.mapper.CommuneDetailMapper;
import fr.airsen.api.repository.*;
import fr.airsen.api.service.cacheData.SmartCacheService;
import fr.airsen.api.util.JpqlResultConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CommuneService {

    private static final Logger log = LoggerFactory.getLogger(CommuneService.class);

    private final CommuneRepository communeRepository;
    private final DepartmentRepository departmentRepository;
    private final RegionRepository regionRepository;
    private final AirQualityRepository airQualityRepository;
    private final WeatherDataRepository weatherDataRepository;
    private final InseeApiClient inseeApiClient;
    private final AtmoApiClient atmoApiClient;
    private final OpenMeteoApiClient openMeteoApiClient;
    private final CommuneDetailMapper communeDetailMapper;
    private final SmartCacheService smartCacheService;

    public CommuneService(CommuneRepository communeRepository,
                         DepartmentRepository departmentRepository,
                         RegionRepository regionRepository,
                         AirQualityRepository airQualityRepository,
                         WeatherDataRepository weatherDataRepository,
                         InseeApiClient inseeApiClient,
                         AtmoApiClient atmoApiClient,
                         OpenMeteoApiClient openMeteoApiClient,
                         CommuneDetailMapper communeDetailMapper,
                         SmartCacheService smartCacheService) {
        this.communeRepository = communeRepository;
        this.departmentRepository = departmentRepository;
        this.regionRepository = regionRepository;
        this.airQualityRepository = airQualityRepository;
        this.weatherDataRepository = weatherDataRepository;
        this.inseeApiClient = inseeApiClient;
        this.atmoApiClient = atmoApiClient;
        this.openMeteoApiClient = openMeteoApiClient;
        this.communeDetailMapper = communeDetailMapper;
        this.smartCacheService = smartCacheService;
    }

    @Transactional(readOnly = true)
    public List<CommuneDTO> getCommunesByDepartment(Long departmentId, int page, int size, String search) {
        log.info("Fetching communes for department {} (page: {}, size: {}, search: '{}')",
            departmentId, page, size, search != null ? search : "none");

        // Verify department exists before querying communes
        if (!departmentRepository.existsById(departmentId)) {
            log.warn("Department not found with ID: {}", departmentId);
            throw new ResourceNotFoundException("Department not found with ID: " + departmentId);
        }

        Pageable pageable = PageRequest.of(page, size);
        List<Object[]> results;

        // Query repository with appropriate method based on search criteria
        if (search != null && !search.isEmpty()) {
            results = communeRepository.findByDepartmentIdAndNameWithLatestAirQuality(
                departmentId, search, pageable);
            log.debug("Using filtered search query with air quality data");
        } else {
            results = communeRepository.findByDepartmentIdWithLatestAirQuality(
                departmentId, pageable);
            log.debug("Using standard query with air quality data");
        }

        // Calculate statistics for monitoring
        long totalCommunes = results.size();
        long communesWithAirQuality = results.stream()
            .filter(row -> row[1] != null) // atmoIndex is not null
            .count();
        long communesWithoutAirQuality = totalCommunes - communesWithAirQuality;

        log.info("Retrieved {} communes: {} with air quality data, {} without",
            totalCommunes, communesWithAirQuality, communesWithoutAirQuality);

        // Map Object[] results to CommuneDTO with null-safe air quality data
        return results.stream()
            .map(row -> {
                Commune c = (Commune) row[0];
                Integer atmoIndex = JpqlResultConverter.toIntegerNullable(row[1]);
                String qualifier = JpqlResultConverter.toStringNullable(row[2]);
                String color = JpqlResultConverter.toStringNullable(row[3]);

                // Log warning if coordinates are missing (impacts map display)
                if (c.getLatitude() == null || c.getLongitude() == null) {
                    log.warn("Commune {} ({}) has missing coordinates - will not display on map",
                        c.getName(), c.getInseeCode());
                }

                return new CommuneDTO(
                    c.getId(),
                    c.getInseeCode(),
                    c.getName(),
                    String.valueOf(c.getDepartment().getDepartmentCode()),
                    c.getRegionCode(),
                    c.getPopulation(),
                    c.getLatitude(),
                    c.getLongitude(),
                    atmoIndex,      // Null if no air quality data
                    qualifier,      // Null
                    color           // Null
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Searches for communes by name or INSEE code (cache temporarily disabled).
     *
     * @param query search query (name or INSEE code)
     * @param limit maximum number of results
     * @return list of matching commune DTOs
     */
    // @Cacheable(value = "communes", key = "#query + ':' + #limit", unless = "#result == null || #result.isEmpty()")
    @Transactional
    public List<CommuneDTO> searchCommunes(String query, int limit) {
        log.info("Cache miss - Searching communes by name or INSEE code: {} (limit: {})", query, limit);

        // First, search in local database
        Page<Commune> communePage = communeRepository.searchByNameOrInseeCode(query, PageRequest.of(0, limit));
        List<Commune> communes = communePage.getContent();

        // If no results found in database, fetch ALL from INSEE API and insert
        if (communes.isEmpty()) {
            log.info("No communes in database. Fetching from INSEE API...");
            try {
                // Check if query looks like an INSEE code (5 digits)
                if (query.matches("\\d{5}")) {
                    log.info("Query '{}' matches INSEE code pattern (5 digits), using exact code lookup", query);
                    CommuneDTO fetchedCommune = fetchCommuneByInseeCode(query).block();
                    if (fetchedCommune != null) {
                        log.info("✓ Successfully fetched commune from INSEE API: {} ({})",
                                fetchedCommune.name(), fetchedCommune.inseeCode());
                        return List.of(fetchedCommune);
                    }
                } else {
                    log.info("Query '{}' is a name search, fetching ALL matching communes", query);
                    // Fetch and save ALL matching communes
                    List<CommuneDTO> fetchedCommunes = fetchAndSaveAllCommunesFromInsee(query);

                    if (!fetchedCommunes.isEmpty()) {
                        log.info("✓ Successfully fetched {} communes from INSEE API", fetchedCommunes.size());

                        // Apply limit AFTER insertion
                        List<CommuneDTO> limitedResults = fetchedCommunes.stream()
                            .limit(limit)
                            .collect(Collectors.toList());

                        // Enrich coordinates for the limited results
                        return enrichCoordinatesIfMissing(limitedResults);
                    } else {
                        log.warn("No communes found in INSEE API for: {}", query);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to fetch communes from INSEE API: {}", query, e);
            }

            return List.of();  // Return empty if API fetch failed
        }

        // Convert database results to DTOs
        List<CommuneDTO> results = communes.stream()
                .map(c -> new CommuneDTO(
                        c.getId(),
                        c.getInseeCode(),
                        c.getName(),
                        String.valueOf(c.getDepartment().getDepartmentCode()),
                        c.getRegionCode(),
                        c.getPopulation(),
                        c.getLatitude(),
                        c.getLongitude(),
                        null,  // atmoIndex - not fetched in search
                        null,  // qualifier - not fetched in search
                        null   // color - not fetched in search
                ))
                .collect(Collectors.toList());

        // Enrich coordinates for results that are missing them
        return enrichCoordinatesIfMissing(results);
    }

    /**
     * Clears all commune cache using SmartCacheService.
     */
    public void evictAllCommuneCache() {
        smartCacheService.clearAll();
        log.info("Cleared all commune cache");
    }

    /**
     * Get communes with coordinates, filtered by minimum population
     *
     * PERFORMANCE OPTIMIZATION: Use minPopulation filter for initial map load to prevent
     * loading 35,000+ communes (~5MB) on page load. Progressive loading strategy loads
     * more communes as user zooms in.
     *
     * Population thresholds:
     * - Zoom 6 (initial): minPopulation=50000 → ~200 communes (~50KB) - Major cities only
     * - Zoom 8-9: minPopulation=20000 → ~500 communes (~150KB)
     * - Zoom 10-11: minPopulation=10000 → ~1000 communes (~300KB)
     * - Zoom 12+: minPopulation=undefined → All 35K communes (~5MB)
     *
     * @param minPopulation Minimum population threshold
     * @return List of communes matching criteria with coordinates and air quality data
     */
    @Transactional(readOnly = true)
    public List<CommuneDTO> getCommunesWithCoordinates(int minPopulation) {
        log.debug("Fetching communes with valid coordinates and population >= {}", minPopulation);

        List<Object[]> results = communeRepository
            .findCommunesWithCoordinatesAndAirQualityByMinPopulation(minPopulation);

        log.debug("Found {} communes matching population filter >= {}", results.size(), minPopulation);

        // Calculate statistics for monitoring air quality data coverage
        long communesWithAirQuality = results.stream()
            .filter(row -> row[1] != null) // atmoIndex is not null
            .count();
        long communesWithoutAirQuality = results.size() - communesWithAirQuality;

        log.info("Population filter >= {}: {} communes ({} with AQ data, {} without)",
            minPopulation, results.size(), communesWithAirQuality, communesWithoutAirQuality);

        // Map Object[] results to CommuneDTO with populated air quality fields
        return results.stream()
            .map(row -> {
                Commune c = (Commune) row[0];
                Integer atmoIndex = JpqlResultConverter.toIntegerNullable(row[1]);
                String qualifier = JpqlResultConverter.toStringNullable(row[2]);
                String color = JpqlResultConverter.toStringNullable(row[3]);

                return new CommuneDTO(
                    c.getId(),
                    c.getInseeCode(),
                    c.getName(),
                    c.getDepartment() != null ? c.getDepartment().getDepartmentCode() : null,
                    c.getDepartment() != null && c.getDepartment().getRegion() != null
                        ? c.getDepartment().getRegion().getRegionCode() : null,
                    c.getPopulation(),
                    c.getLatitude(),
                    c.getLongitude(),
                    atmoIndex,   // populated or null
                    qualifier,   // populated or null
                    color        // populated or null
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Gets all communes that have valid coordinates for map display.
     *
     * This method is used by the interactive map component to render commune markers.
     * Cache temporarily disabled for frontend development - fetches fresh data from database.
     * Only communes with non-null latitude and longitude values are returned.
     *
     * @return list of commune DTOs with coordinates
     */
    // @Cacheable(value = "communes", key = "'all-with-coordinates'") // Re-enable after testing
    @Transactional(readOnly = true)
    public List<CommuneDTO> getAllCommunesWithCoordinates() {
        log.info("Fetching all communes with valid coordinates AND air quality data for map display");

        // Use new repository method that includes air quality data via LEFT JOIN
        List<Object[]> results = communeRepository.findCommunesWithCoordinatesAndAirQuality();

        log.info("Found {} communes with coordinates", results.size());

        // Calculate statistics for monitoring air quality data coverage
        long communesWithAirQuality = results.stream()
            .filter(row -> row[1] != null) // atmoIndex is not null
            .count();
        long communesWithoutAirQuality = results.size() - communesWithAirQuality;

        log.info("Air quality data coverage: {} with data ({} %), {} without data",
            communesWithAirQuality,
            results.isEmpty() ? 0 : (communesWithAirQuality * 100 / results.size()),
            communesWithoutAirQuality);

        // Map Object[] results to CommuneDTO with populated air quality fields
        return results.stream()
            .map(row -> {
                Commune c = (Commune) row[0];
                Integer atmoIndex = JpqlResultConverter.toIntegerNullable(row[1]);  // Can be null
                String qualifier = JpqlResultConverter.toStringNullable(row[2]);    // Can be null
                String color = JpqlResultConverter.toStringNullable(row[3]);        // Can be null

                return new CommuneDTO(
                    c.getId(),
                    c.getInseeCode(),
                    c.getName(),
                    c.getDepartment() != null ? c.getDepartment().getDepartmentCode() : null,
                    c.getDepartment() != null && c.getDepartment().getRegion() != null
                        ? c.getDepartment().getRegion().getRegionCode() : null,
                    c.getPopulation(),
                    c.getLatitude(),
                    c.getLongitude(),
                    atmoIndex,   // populated or null
                    qualifier,   // populated or null
                    color        // populated or null
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Gets communes by department code (for external API controller).
     *
     * @param departmentCode department code as string
     * @return list of commune DTOs
     */
    public List<CommuneDTO> getCommunesByDepartment(String departmentCode) {
        log.info("Fetching communes for department: {}", departmentCode);

        List<Commune> communes = communeRepository.findByDepartmentCode(departmentCode);

        return communes.stream()
            .map(c -> new CommuneDTO(
                c.getId(),
                c.getInseeCode(),
                c.getName(),
                departmentCode,
                c.getRegionCode(),
                c.getPopulation(),
                c.getLatitude(),
                c.getLongitude(),
                null,  // atmoIndex - not fetched in this method
                null,  // qualifier - not fetched in this method
                null   // color - not fetched in this method
            ))
            .collect(Collectors.toList());
    }

    /**
     * Gets coordinates for a specific commune using INSEE API.
     *
     * @param communeId INSEE code of the commune
     * @return Mono containing coordinates [latitude, longitude]
     */
    public Mono<Double[]> getCommuneCoordinates(String communeId) {
        log.info("Fetching coordinates for commune: {}", communeId);

        return inseeApiClient.getCommuneCoordinates(communeId)
            .doOnSuccess(coordinates -> log.info("Retrieved coordinates for commune {}: [{}, {}]",
                                               communeId, coordinates[0], coordinates[1]))
            .doOnError(error -> log.error("Failed to retrieve coordinates for commune: {}", communeId, error));
    }

    /**
     * Synchronizes demographic data for a commune from INSEE API.
     *
     * @param communeId INSEE code of the commune
     * @return Mono containing updated commune DTO
     */
    public Mono<CommuneDTO> syncDemographicData(String communeId) {
        log.info("Synchronizing demographic data for commune: {}", communeId);

        return inseeApiClient.getDemographicData(communeId)
            .map(demographicData -> {
                // Find existing commune
                Commune commune = communeRepository.findByInseeCode(communeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Commune not found: " + communeId));

                // Update demographic data
                commune.setPopulation(demographicData.population());

                // Save updated commune
                Commune savedCommune = communeRepository.save(commune);

                log.info("Updated demographic data for commune: {} - Population: {}",
                        communeId, savedCommune.getPopulation());

                return new CommuneDTO(
                    savedCommune.getId(),
                    savedCommune.getInseeCode(),
                    savedCommune.getName(),
                    String.valueOf(savedCommune.getDepartment().getDepartmentCode()),
                    savedCommune.getRegionCode(),
                    savedCommune.getPopulation(),
                    savedCommune.getLatitude(),
                    savedCommune.getLongitude(),
                    null,  // atmoIndex - not synced in this method
                    null,  // qualifier - not synced in this method
                    null   // color - not synced in this method
                );
            })
            .doOnError(error -> log.error("Failed to sync demographic data for commune: {}", communeId, error));
    }

    /**
     * Fetches commune data from INSEE API by exact INSEE code and saves to database.
     *
     * @param inseeCode exact 5-digit INSEE code
     * @return Mono containing the saved commune DTO
     */
    @Transactional
    public Mono<CommuneDTO> fetchCommuneByInseeCode(String inseeCode) {
        log.info("Fetching commune data from INSEE API by code: {}", inseeCode);

        return inseeApiClient.getCommuneData(inseeCode)
            .flatMap(this::saveInseeDataToDatabase)
            .doOnSuccess(communeDTO -> log.info("Successfully fetched and saved commune from INSEE API by code: {}", communeDTO))
            .doOnError(error -> log.error("Failed to fetch and save commune from INSEE API by code: {}", inseeCode, error));
    }

    /**
     * Fetches commune data from INSEE API by name and saves to database.
     *
     * @param communeName name of the commune to search for
     * @return Mono containing the saved commune DTO
     */
    @Transactional
    public Mono<CommuneDTO> fetchAndSaveCommuneFromInsee(String communeName) {
        log.info("Fetching commune data from INSEE API by name: {}", communeName);

        return inseeApiClient.searchCommunesByName(communeName)
            .next()
            .flatMap(this::saveInseeDataToDatabase)
            .doOnSuccess(communeDTO -> log.info("Successfully fetched and saved commune from INSEE API by name: {}", communeDTO))
            .doOnError(error -> log.error("Failed to fetch and save commune from INSEE API by name: {}", communeName, error));
    }

    /**
     * Fetches ALL commune data from INSEE API and saves to database.
     *
     * @param communeName name to search for
     * @return List of saved commune DTOs
     */
    @Transactional
    public List<CommuneDTO> fetchAndSaveAllCommunesFromInsee(String communeName) {
        log.info("Fetching ALL commune data from INSEE API for: {}", communeName);

        try {
            // Fetch ALL matching communes from INSEE API (no limit)
            List<InseeCommuneResponse> inseeResponses = inseeApiClient
                .searchCommunesByName(communeName)
                .collectList()
                .block();

            if (inseeResponses == null || inseeResponses.isEmpty()) {
                log.warn("No communes returned from INSEE API for: {}", communeName);
                return List.of();
            }

            log.info("✓ Fetched {} communes from INSEE API for query: '{}'",
                inseeResponses.size(), communeName);

            // Save ALL communes to database (batch operation)
            int savedCount = 0;
            int skippedCount = 0;
            int errorCount = 0;
            List<CommuneDTO> savedCommunes = new ArrayList<>();

            for (InseeCommuneResponse inseeResponse : inseeResponses) {
                try {
                    // Check if commune already exists (by INSEE code)
                    if (communeRepository.findByInseeCode(inseeResponse.inseeCode()).isPresent()) {
                        log.debug("Commune {} ({}) already exists, skipping",
                            inseeResponse.name(), inseeResponse.inseeCode());
                        skippedCount++;
                        continue;
                    }

                    // Save to database
                    CommuneDTO savedCommune = saveInseeDataToDatabase(inseeResponse).block();
                    if (savedCommune != null) {
                        savedCommunes.add(savedCommune);
                        savedCount++;
                    }
                } catch (Exception e) {
                    errorCount++;
                    log.error("Failed to save commune {} ({}): {}",
                        inseeResponse.name(), inseeResponse.inseeCode(), e.getMessage());
                }
            }

            log.info("✓ INSEE API insertion complete for '{}': {} saved, {} skipped, {} errors",
                communeName, savedCount, skippedCount, errorCount);

            // Always query database to return ALL matching communes (saved + already existing)
            // This ensures to return the complete result set matching the search query
            log.info("Querying database for all communes matching: {}", communeName);
            Page<Commune> communePage = communeRepository.searchByNameOrInseeCode(
                communeName,
                PageRequest.of(0, 100)  // Fetch all matching communes
            );

            List<CommuneDTO> allMatchingCommunes = communePage.getContent().stream()
                .map(c -> new CommuneDTO(
                    c.getId(),
                    c.getInseeCode(),
                    c.getName(),
                    c.getDepartmentCode(),
                    c.getRegionCode(),
                    c.getPopulation(),
                    c.getLatitude(),
                    c.getLongitude(),
                    null,  // atmoIndex - not fetched in INSEE sync
                    null,  // qualifier - not fetched in INSEE sync
                    null   // color - not fetched in INSEE sync
                ))
                .collect(Collectors.toList());

            log.info("✓ Returning {} total communes matching '{}'", allMatchingCommunes.size(), communeName);
            return allMatchingCommunes;

        } catch (Exception e) {
            log.error("Error fetching/saving communes from INSEE API for: {}", communeName, e);
            return List.of();
        }
    }

    /**
     * Saves INSEE commune data to database with proper geographic hierarchy.
     *
     * Creates or updates Region -> Department -> Commune relationships.
     *
     * @param inseeResponse INSEE API response for a commune
     * @return Mono containing the saved commune DTO
     */
    @Transactional
    public Mono<CommuneDTO> saveInseeDataToDatabase(InseeCommuneResponse inseeResponse) {
        return Mono.fromCallable(() -> {
            log.info("Saving INSEE data to database for commune: {} ({})",
                    inseeResponse.name(), inseeResponse.inseeCode());

            // Find or create Region
            Region region = regionRepository.findByRegionCode(inseeResponse.regionCode())
                .orElseGet(() -> {
                    log.info("Creating new region with code: {}", inseeResponse.regionCode());
                    Region newRegion = new Region();
                    newRegion.setRegionCode(inseeResponse.regionCode());
                    // Use actual region name from INSEE API
                    String regionName = inseeResponse.region() != null && inseeResponse.region().nom() != null
                        ? inseeResponse.region().nom()
                        : "Region " + inseeResponse.regionCode();
                    newRegion.setName(regionName);
                    return regionRepository.save(newRegion);
                });

            // Update region name if it has changed
            if (inseeResponse.region() != null && inseeResponse.region().nom() != null) {
                if (!inseeResponse.region().nom().equals(region.getName())) {
                    region.setName(inseeResponse.region().nom());
                    regionRepository.save(region);
                }
            }

            // Find or create Department
            Department department = departmentRepository.findAll().stream()
                .filter(d -> d.getDepartmentCode().equals(inseeResponse.departmentCode()))
                .findFirst()
                .orElseGet(() -> {
                    log.info("Creating new department with code: {}", inseeResponse.departmentCode());
                    Department newDepartment = new Department();
                    newDepartment.setDepartmentCode(inseeResponse.departmentCode());
                    // Use actual department name from INSEE API
                    String departmentName = inseeResponse.departement() != null && inseeResponse.departement().nom() != null
                        ? inseeResponse.departement().nom()
                        : "Department " + inseeResponse.departmentCode();
                    newDepartment.setName(departmentName);
                    newDepartment.setRegionCode(inseeResponse.regionCode());
                    newDepartment.setRegion(region);
                    return departmentRepository.save(newDepartment);
                });

            // Update department name if it has changed
            if (inseeResponse.departement() != null && inseeResponse.departement().nom() != null) {
                if (!inseeResponse.departement().nom().equals(department.getName())) {
                    department.setName(inseeResponse.departement().nom());
                    departmentRepository.save(department);
                }
            }

            // Check if commune already exists
            Commune commune = communeRepository.findByInseeCode(inseeResponse.inseeCode())
                .orElse(new Commune());

            // Update commune data
            commune.setInseeCode(inseeResponse.inseeCode());
            commune.setName(inseeResponse.name());
            commune.setDepartmentCode(inseeResponse.departmentCode());
            commune.setRegionCode(inseeResponse.regionCode());
            commune.setPopulation(inseeResponse.population() != null ? inseeResponse.population().longValue() : 0L);
            commune.setDepartment(department);

            // Set coordinates if available
            if (inseeResponse.centre() != null) {
                Double latitude = inseeResponse.centre().getLatitude();
                Double longitude = inseeResponse.centre().getLongitude();
                if (latitude != null && longitude != null) {
                    commune.setLatitude(BigDecimal.valueOf(latitude));
                    commune.setLongitude(BigDecimal.valueOf(longitude));
                    log.info("Set coordinates for {}: lat={}, lng={}",
                            inseeResponse.name(), latitude, longitude);
                }
            }

            // Save commune
            Commune savedCommune = communeRepository.save(commune);
            log.info("✓ Saved commune: {} ({}) - ID: {}, Pop: {}",
                    savedCommune.getName(),
                    savedCommune.getInseeCode(),
                    savedCommune.getId(),
                    savedCommune.getPopulation());

            // Return DTO
            return new CommuneDTO(
                savedCommune.getId(),
                savedCommune.getInseeCode(),
                savedCommune.getName(),
                savedCommune.getDepartmentCode(),
                savedCommune.getRegionCode(),
                savedCommune.getPopulation(),
                savedCommune.getLatitude(),
                savedCommune.getLongitude(),
                null,  // atmoIndex - not fetched from INSEE API
                null,  // qualifier - not fetched from INSEE API
                null   // color - not fetched from INSEE API
            );
        });
    }

    /**
     * Checks if a commune has valid coordinates.
     *
     * @param commune commune DTO to check
     * @return true if both latitude and longitude are present
     */
    private boolean hasCoordinates(CommuneDTO commune) {
        return commune.latitude() != null && commune.longitude() != null;
    }

    /**
     * Enriches communes with missing coordinates by fetching from INSEE API.
     * Only processes communes in the provided list (respects limit parameter).
     * Updates database for future requests.
     *
     * Note: This private method inherits the transaction from the calling public method.
     *
     * @param communes list of communes to check and enrich
     * @return enriched list with coordinates
     */
    private List<CommuneDTO> enrichCoordinatesIfMissing(List<CommuneDTO> communes) {
        if (communes == null || communes.isEmpty()) {
            return communes;
        }

        List<CommuneDTO> enrichedCommunes = new ArrayList<>();
        int enrichedCount = 0;

        for (CommuneDTO communeDTO : communes) {
            if (!hasCoordinates(communeDTO)) {
                // DEBUG - only in development
                log.debug("Commune '{}' ({}) missing coordinates. Fetching from INSEE API...",
                        communeDTO.name(), communeDTO.inseeCode());

                try {
                    CommuneDTO enriched = fetchCommuneByInseeCode(communeDTO.inseeCode()).block();

                    if (enriched != null && hasCoordinates(enriched)) {
                        enrichedCommunes.add(enriched);
                        enrichedCount++;

                        // DEBUG - only in development
                        log.debug("Enriched coordinates for '{}': lat={}, lng={}",
                                enriched.name(), enriched.latitude(), enriched.longitude());
                    } else {
                        // WARN - kept in production (unusual situation)
                        log.warn("INSEE API did not provide coordinates for '{}' ({})",
                                communeDTO.name(), communeDTO.inseeCode());
                        enrichedCommunes.add(communeDTO);
                    }
                } catch (Exception e) {
                    // ERROR - always kept in production
                    log.error("Failed to fetch coordinates for '{}' ({}): {}",
                            communeDTO.name(), communeDTO.inseeCode(), e.getMessage());
                    enrichedCommunes.add(communeDTO);
                }
            } else {
                enrichedCommunes.add(communeDTO);
            }
        }

        // INFO - kept in production, but only if enrichment happened
        if (enrichedCount > 0) {
            log.info("Enriched {} commune(s) with coordinates", enrichedCount);
        }

        return enrichedCommunes;
    }

    /**
     * Gets detailed commune information with integrated environmental data using smart caching.
     *
     * Cache-Aware Data Aggregation Strategy:
     * 1. Check SmartCache first (cache key: "commune-details:" + inseeCode)
     * 2. If cache miss or stale, execute fetchCommuneDetailFromDatabase()
     * 3. Cache result with 6-hour TTL (ON_DEMAND_FETCH)
     *
     * Data Aggregation (preserved as-is):
     * 1. Fetch commune data with eager loading (department, region)
     * 2. Fetch latest air quality data for the commune
     * 3. Fetch latest weather data for the commune
     * 4. Aggregate all data using CommuneDetailMapper
     *
     * @param inseeCode INSEE code of the commune
     * @return CommuneDetailResponse containing aggregated commune, air quality, and weather data
     * @throws ResourceNotFoundException if commune not found
     */
    @Transactional(readOnly = true)
    public CommuneDetailResponse getCommuneDetailWithEnvironmentalData(String inseeCode) {
        log.debug("Fetching commune detail with environmental data for INSEE code: {}", inseeCode);

        // Validate INSEE code format
        if (inseeCode == null || !inseeCode.matches("\\d{5}")) {
            log.warn("Invalid INSEE code format: {}", inseeCode);
            throw new IllegalArgumentException("INSEE code must be exactly 5 digits");
        }

        String cacheKey = "commune-details:" + inseeCode;

        return smartCacheService.getOrFetch(
            cacheKey,
            CommuneDetailResponse.class,
            CacheMetadata.DataSource.ON_DEMAND_FETCH,
            false, // forceRefresh
            () -> fetchCommuneDetailFromDatabase(inseeCode)
        ).getData();
    }

    /**
     * Fetches commune detail with aggregated environmental data from database.
     *
     * This method preserves the existing data aggregation logic exactly as-is.
     * Called by SmartCacheService when cache miss occurs or refresh is needed.
     *
     * @param inseeCode INSEE code of the commune
     * @return CommuneDetailResponse with aggregated data
     * @throws ResourceNotFoundException if commune not found
     */
    private CommuneDetailResponse fetchCommuneDetailFromDatabase(String inseeCode) {
        log.debug("Cache miss or refresh needed, fetching commune detail from database");

        // Step 1: Fetch commune from database with eager loading (single query)
        Commune commune = communeRepository.findByInseeCodeWithEagerLoading(inseeCode)
            .orElseThrow(() -> {
                log.error("Commune not found for INSEE code: {}", inseeCode);
                return new ResourceNotFoundException("Commune not found with INSEE code: " + inseeCode);
            });

        log.debug("Found commune: {} ({}) - Department: {}, Region: {}",
            commune.getName(),
            commune.getInseeCode(),
            commune.getDepartment() != null ? commune.getDepartment().getName() : "N/A",
            commune.getDepartment() != null && commune.getDepartment().getRegion() != null
                ? commune.getDepartment().getRegion().getName() : "N/A"
        );

        // Step 2: Fetch latest air quality from database
        Optional<AirQuality> airQualityOpt = airQualityRepository.findLatestByCommune_InseeCode(inseeCode);
        AirQuality airQuality = airQualityOpt.orElse(null);

        if (airQuality != null) {
            log.debug("Found air quality data for {}: ATMO index={}, date={}",
                inseeCode, airQuality.getAtmoIndex(), airQuality.getMeasurementDate());
        } else {
            log.debug("No air quality data found in database for commune: {}", inseeCode);
        }

        // Step 3: Fetch latest weather data from database
        Optional<WeatherData> weatherOpt = weatherDataRepository.getMostRecentWeatherByInseeCode(inseeCode);
        WeatherData weather = weatherOpt.orElse(null);

        if (weather != null) {
            log.debug("Found weather data for {}: temp={}°C, date={}",
                inseeCode, weather.getTemperature(), weather.getMeasurementDate());
        } else {
            log.debug("No weather data found in database for commune: {}", inseeCode);
        }

        // Step 4: Build response DTO using MapStruct mapper
        CommuneDetailResponse response = communeDetailMapper.toCommuneDetailResponse(
            commune,
            airQuality,
            weather
        );

        log.info("Successfully retrieved commune detail for {} from database (air quality: {}, weather: {})",
            inseeCode,
            airQuality != null ? "available" : "unavailable",
            weather != null ? "available" : "unavailable"
        );

        return response;
    }

    /**
     * Evicts commune details cache for specific commune using SmartCacheService.
     *
     * @param inseeCode INSEE code of the commune
     */
    public void evictCommuneDetailsCache(String inseeCode) {
        String cacheKey = "commune-details:" + inseeCode;
        smartCacheService.invalidate(cacheKey);
        log.info("Evicted commune details cache for INSEE code: {}", inseeCode);
    }

    /**
     * Clears all commune details cache using SmartCacheService.
     */
    public void evictAllCommuneDetailsCache() {
        smartCacheService.clearAll();
        log.info("Cleared all commune details cache");
    }
}
