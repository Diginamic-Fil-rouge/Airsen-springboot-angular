package fr.airsen.api.service;

import fr.airsen.api.dto.CommuneDTO;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.Department;
import fr.airsen.api.entity.Region;
import fr.airsen.api.external.client.InseeApiClient;
import fr.airsen.api.external.dto.insee.InseeCommuneResponse;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.DepartmentRepository;
import fr.airsen.api.repository.RegionRepository;
import fr.airsen.api.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CommuneService {

    private static final Logger log = LoggerFactory.getLogger(CommuneService.class);
    
    private final CommuneRepository communeRepository;
    private final DepartmentRepository departmentRepository;
    private final RegionRepository regionRepository;
    private final InseeApiClient inseeApiClient;

    public CommuneService(CommuneRepository communeRepository, 
                         DepartmentRepository departmentRepository,
                         RegionRepository regionRepository,
                         InseeApiClient inseeApiClient) {
        this.communeRepository = communeRepository;
        this.departmentRepository = departmentRepository;
        this.regionRepository = regionRepository;
        this.inseeApiClient = inseeApiClient;
    }

    @Transactional(readOnly = true)
    public List<CommuneDTO> getCommunesByDepartment(Long departmentId, int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size);
        List<Commune> communes;

        if (search != null && !search.isEmpty()) {
            communes = communeRepository.findByDepartmentIdAndNameContainingIgnoreCase(departmentId, search, pageable);
        } else {
            communes = communeRepository.findByDepartmentIdAsList(departmentId, pageable);
        }

        return communes.stream()
                .map(c -> new CommuneDTO(
                        c.getId(),
                        c.getInseeCode(),
                        c.getName(),
                        String.valueOf(c.getDepartment().getDepartmentCode()),
                        c.getRegionCode(),
                        c.getPopulation()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommuneDTO> searchCommunes(String query, int limit) {
        log.info("Searching communes by name: {}", query);
        
        // First, search in local database
        Page<Commune> communePage = communeRepository.findByNameContainingIgnoreCase(query, PageRequest.of(0, limit));
        List<Commune> communes = communePage.getContent();
        
        // If no results found in database, try to fetch from INSEE API
        if (communes.isEmpty()) {
            log.info("No communes found in database for query: {}. Fetching from INSEE API...", query);
            try {
                // Fetch from INSEE API and save to database
                CommuneDTO fetchedCommune = fetchAndSaveCommuneFromInsee(query).block();
                if (fetchedCommune != null) {
                    log.info("Successfully fetched commune from INSEE API: {}", fetchedCommune.getName());
                    return List.of(fetchedCommune);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch commune from INSEE API: {}", query, e);
            }
        }
        
        return communes.stream()
                .map(c -> new CommuneDTO(
                        c.getId(),
                        c.getInseeCode(),
                        c.getName(),
                        String.valueOf(c.getDepartment().getDepartmentCode()),
                        c.getRegionCode(),
                        c.getPopulation()
                ))
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
                c.getPopulation()
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
                    savedCommune.getPopulation()
                );
            })
            .doOnError(error -> log.error("Failed to sync demographic data for commune: {}", communeId, error));
    }

    /**
     * Fetches commune data from INSEE API and saves to database.
     *
     * @param communeName name of the commune to search for
     * @return Mono containing the saved commune DTO
     */
    @Transactional
    public Mono<CommuneDTO> fetchAndSaveCommuneFromInsee(String communeName) {
        log.info("Fetching commune data from INSEE API for: {}", communeName);
        
        return inseeApiClient.searchCommunesByName(communeName, 1)
            .next()
            .flatMap(this::saveInseeDataToDatabase)
            .doOnSuccess(communeDTO -> log.info("Successfully fetched and saved commune from INSEE API: {}", communeDTO))
            .doOnError(error -> log.error("Failed to fetch and save commune from INSEE API: {}", communeName, error));
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
            log.info("Saved commune to database: {} (ID: {})", 
                    savedCommune.getName(), savedCommune.getId());

            // Return DTO
            return new CommuneDTO(
                savedCommune.getId(),
                savedCommune.getInseeCode(),
                savedCommune.getName(),
                savedCommune.getDepartmentCode(),
                savedCommune.getRegionCode(),
                savedCommune.getPopulation()
            );
        });
    }
}
