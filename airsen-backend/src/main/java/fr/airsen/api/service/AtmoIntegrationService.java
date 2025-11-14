package fr.airsen.api.service;

import fr.airsen.api.dto.AirQualityResponseDTO;
import fr.airsen.api.dto.response.NearestAirQualityResult;
import fr.airsen.api.dto.response.AirQualityResponse;
import fr.airsen.api.entity.AirQuality;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.cacheData.CacheMetadata;
import fr.airsen.api.external.client.AtmoApiClient;
import fr.airsen.api.external.dto.atmo.AtmoAirQualityResponse;
import fr.airsen.api.mapper.AirQualityMapper;
import fr.airsen.api.repository.AirQualityRepository;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.service.cacheData.SmartCacheService;
import fr.airsen.api.exception.AirQualityDataNotFoundException;
import fr.airsen.api.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Production service for ATMO France API integration.
 *
 * Handles automatic data synchronization, caching, and database storage
 * of air quality measurements for the Airsen application.
 */
@Service
@Transactional
public class AtmoIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(AtmoIntegrationService.class);
    private static final int BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;

    private final AtmoApiClient atmoApiClient;
    private final AirQualityRepository airQualityRepository;
    private final CommuneRepository communeRepository;
    private final GeoDistanceService geoDistanceService;
    private final SmartCacheService smartCacheService;
    private final AirQualityMapper airQualityMapper;

    public AtmoIntegrationService(AtmoApiClient atmoApiClient,
                                 AirQualityRepository airQualityRepository,
                                 CommuneRepository communeRepository,
                                 GeoDistanceService geoDistanceService,
                                 SmartCacheService smartCacheService,
                                 AirQualityMapper airQualityMapper) {
        this.atmoApiClient = atmoApiClient;
        this.airQualityRepository = airQualityRepository;
        this.communeRepository = communeRepository;
        this.geoDistanceService = geoDistanceService;
        this.smartCacheService = smartCacheService;
        this.airQualityMapper = airQualityMapper;
    }

    /**
     * Scheduled task to fetch and store current ATMO air quality data.
     *
     * Runs daily at 10:00 AM (configurable via scheduling.atmo.cron property).
     * Default: 10:00 AM Europe/Paris timezone.
     *
     * Configuration:
     * - scheduling.atmo.cron: Cron expression (default: 0 0 10 * * *)
     * - scheduling.atmo.timezone: Timezone (default: Europe/Paris)
     * - scheduling.enabled: Master switch to enable/disable all scheduling (default: true)
     *
     * Can be disabled by setting scheduling.enabled=false in application.yml
     */
    @Scheduled(cron = "${scheduling.atmo.cron}", zone = "${scheduling.atmo.timezone}")
    @Async
    @ConditionalOnProperty(
        value = "scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public void scheduledDataSync() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("========================================");
        log.info("SCHEDULED ATMO SYNC STARTED at {}", startTime);
        log.info("========================================");

        syncCurrentAirQualityData()
            .doOnSuccess(count -> {
                LocalDateTime endTime = LocalDateTime.now();
                Duration duration = Duration.between(startTime, endTime);
                log.info("========================================");
                log.info("SCHEDULED ATMO SYNC COMPLETED at {}", endTime);
                log.info("Records processed: {}", count);
                log.info("Duration: {} seconds", duration.getSeconds());
                log.info("========================================");
            })
            .doOnError(error -> {
                LocalDateTime endTime = LocalDateTime.now();
                Duration duration = Duration.between(startTime, endTime);
                log.error("========================================");
                log.error("SCHEDULED ATMO SYNC FAILED at {}", endTime);
                log.error("Duration before failure: {} seconds", duration.getSeconds());
                log.error("Error: {}", error.getMessage(), error);
                log.error("========================================");
            })
            .subscribe();
    }

    /**
     * Manual trigger for immediate data synchronization.
     *
     * @return Mono containing the number of records processed
     */
    public Mono<Integer> syncCurrentAirQualityData() {
        log.info("Starting manual ATMO data synchronization");

        return atmoApiClient.getCurrentAirQualityIndices()
            .doOnNext(response -> log.debug("Processing air quality data for commune: {}", response.communeInsee()))
            .flatMap(this::processAndStoreAirQuality)
            .collectList()
            .map(results -> {
                int successCount = (int) results.stream().filter(success -> success).count();
                int totalCount = results.size();
                log.info("ATMO sync completed: {}/{} records processed successfully", successCount, totalCount);
                return successCount;
            })
            .onErrorResume(error -> {
                log.error("ATMO sync failed completely", error);
                return Mono.just(0);
            });
    }

    /**
     * Fetches air quality data for a specific commune by INSEE code.
     * Also saves/updates the data in the database.
     *
     * @param inseeCode INSEE code of the commune
     * @return Mono containing the air quality data as DTO
     */
    public Mono<Optional<AirQualityResponseDTO>> getAirQualityForCommune(String inseeCode) {
        log.info("Starting getAirQualityForCommune for: {}", inseeCode);

        return atmoApiClient.getCurrentAirQuality(inseeCode)
            .flatMap(this::convertToAirQuality)
            .map(airQuality -> {
                try {
                    log.info("Attempting to save air quality data for commune: {} with measurement date: {}",
                            inseeCode, airQuality.getMeasurementDate());
                    log.info("Air quality data: index={}, qualifier={}, color={}",
                            airQuality.getAtmIndex(), airQuality.getQualifier(), airQuality.getColor());

                    // Check if data already exists for today
                    LocalDate measurementDate = airQuality.getMeasurementDate();
                    Optional<AirQuality> existing = airQualityRepository
                        .findByCommuneAndMeasurementDateWithEagerLoading(airQuality.getCommune(), measurementDate);

                    AirQuality savedEntity;
                    if (existing.isPresent()) {
                        // Update existing record
                        AirQuality existingRecord = existing.get();
                        log.info("Found existing record with ID: {}, updating...", existingRecord.getId());
                        updateAirQualityRecord(existingRecord, airQuality);
                        AirQuality updatedRecord = airQualityRepository.save(existingRecord);
                        log.info("Successfully updated air quality record with ID: {} for commune: {}", updatedRecord.getId(), inseeCode);

                        // Reload with eager loading to ensure relationships are initialized
                        savedEntity = airQualityRepository
                            .findByCommuneAndMeasurementDateWithEagerLoading(updatedRecord.getCommune(), updatedRecord.getMeasurementDate())
                            .orElse(updatedRecord);
                    } else {
                        // Create new record
                        log.info("No existing record found, creating new record...");
                        AirQuality newRecord = airQualityRepository.save(airQuality);
                        log.info("Successfully created new air quality record with ID: {} for commune: {}", newRecord.getId(), inseeCode);

                        // Reload with eager loading to avoid LazyInitializationException
                        savedEntity = airQualityRepository
                            .findByCommuneAndMeasurementDateWithEagerLoading(newRecord.getCommune(), newRecord.getMeasurementDate())
                            .orElse(newRecord);
                    }

                    // Convert to DTO within transactional context to avoid lazy loading issues
                    log.info("Converting saved entity to DTO for commune: {}", inseeCode);
                    try {
                        AirQualityResponseDTO dto = AirQualityResponseDTO.fromEntity(savedEntity);
                        log.info("Successfully converted to DTO");
                        return Optional.of(dto);
                    } catch (Exception dtoException) {
                        log.error("Failed to convert entity to DTO: {}", dtoException.getMessage(), dtoException);
                        throw dtoException;
                    }
                } catch (Exception e) {
                    log.error("Failed to store air quality data for commune: {} - Error: {}", inseeCode, e.getMessage(), e);
                    // Convert to DTO even if save failed
                    try {
                        log.info("Converting unsaved entity to DTO for commune: {}", inseeCode);
                        AirQualityResponseDTO dto = AirQualityResponseDTO.fromEntity(airQuality);
                        log.info("Successfully converted unsaved entity to DTO");
                        return Optional.of(dto);
                    } catch (Exception dtoException) {
                        log.error("Failed to convert unsaved entity to DTO: {}", dtoException.getMessage(), dtoException);
                        throw new RuntimeException("Failed to convert air quality data to response format", dtoException);
                    }
                }
            })
            .switchIfEmpty(Mono.just(Optional.empty()))
            .onErrorReturn(Optional.empty())
            .doOnNext(result -> {
                if (result.isPresent()) {
                    log.debug("Successfully retrieved and stored air quality for commune: {}", inseeCode);
                } else {
                    log.warn("No air quality data available for commune: {}", inseeCode);
                }
            });
    }

    /**
     * Gets air quality data for a commune synchronously.
     * This method is designed for Redis cache integration later.
     *
     * @param inseeCode INSEE code of the commune
     * @return Optional containing the air quality data as DTO
     */
    public Optional<AirQualityResponseDTO> getAirQualityForCommuneSync(String inseeCode) {
        log.info("Synchronous air quality request for commune: {}", inseeCode);

        try {
            // Convert reactive call to synchronous using block()
            // This is where we'll add Redis cache logic later
            Optional<AirQualityResponseDTO> result = getAirQualityForCommune(inseeCode)
                .block(java.time.Duration.ofSeconds(10)); // 10 second timeout

            log.info("Successfully retrieved air quality data for commune: {}", inseeCode);
            return result != null ? result : Optional.empty();

        } catch (Exception e) {
            log.error("Error in synchronous air quality request for commune: {}", inseeCode, e);
            // Fallback to database data if external API fails
            return getLatestStoredAirQualityLegacy(inseeCode);
        }
    }

    /**
     * Gets air quality response for a commune with smart caching and geodistance fallback.
     *
     * Cache-Aware Data Retrieval Strategy:
     * 1. Check SmartCache first (cache key: "air-quality:" + inseeCode)
     * 2. If cache miss or stale, execute fetchAirQualityWithFallback()
     * 3. Cache result with 6-hour TTL (ON_DEMAND_FETCH)
     *
     * Fallback Strategy (preserved as-is):
     * 1. Query database for recent data from requested commune
     * 2. If no direct data exists, attempt geodistance fallback within 20km
     * 3. Return AirQualityResponse with proper DataSource metadata (DIRECT/ESTIMATED)
     *
     * @param inseeCode INSEE code of the commune
     * @return AirQualityResponse containing air quality data with source transparency
     * @throws ResourceNotFoundException if no air quality data within 20km threshold
     */
    @Transactional(readOnly = true)
    public AirQualityResponse getLatestStoredAirQuality(String inseeCode) {
        log.debug("Fetching air quality for commune: {}", inseeCode);

        String cacheKey = "air-quality:" + inseeCode;

        return smartCacheService.getOrFetch(
            cacheKey,
            AirQualityResponse.class,
            CacheMetadata.DataSource.ON_DEMAND_FETCH,
            false, // forceRefresh
            () -> fetchAirQualityWithFallback(inseeCode)
        ).getData();
    }

    /**
     * Fetches air quality data with geodistance fallback logic (extracted from cache wrapper).
     *
     * This method preserves the existing 20km Haversine fallback exactly as-is.
     * Called by SmartCacheService when cache miss occurs or refresh is needed.
     *
     * @param inseeCode INSEE code of the commune
     * @return AirQualityResponse with proper DataSource metadata
     * @throws ResourceNotFoundException if no air quality data within 20km threshold
     */
    private AirQualityResponse fetchAirQualityWithFallback(String inseeCode) {
        log.debug("Cache miss or refresh needed, fetching air quality from database");

        // Validate commune exists first - prevents 500 errors for non-existent communes
        Optional<Commune> requestedCommune = communeRepository.findByInseeCode(inseeCode);
        if (requestedCommune.isEmpty()) {
            log.warn("Commune not found: {}", inseeCode);
            throw new ResourceNotFoundException("Commune not found");
        }

        // Try direct database query
        Optional<AirQuality> directData = airQualityRepository
            .findLatestByCommune_InseeCode(inseeCode);

        if (directData.isPresent()) {
            log.debug("Found direct air quality data for commune: {}", inseeCode);
            return airQualityMapper.toDirectResponse(directData.get());
        }

        // Geodistance fallback (20km threshold) - PRESERVED AS-IS
        log.debug("No direct data, attempting geodistance fallback within 20km");
        Optional<NearestAirQualityResult> nearestData = geoDistanceService
            .findNearestCommuneWithAirQuality(inseeCode, 20.0);

        if (nearestData.isPresent()) {
            log.info("Using estimated air quality from {} ({} km away)",
                     nearestData.get().communeName(),
                     nearestData.get().distanceKm());
            return airQualityMapper.toEstimatedResponse(nearestData.get(), requestedCommune.get());
        }

        throw new ResourceNotFoundException(
            "No air quality data within 20km"
        );
    }

    /**
     * Gets the latest stored air quality data for a commune (legacy method for backward compatibility).
     *
     * @param inseeCode INSEE code of the commune
     * @return Optional containing the latest air quality data as DTO
     * @deprecated Use getLatestStoredAirQuality() that returns AirQualityResponse with caching
     */
    @Deprecated
    public Optional<AirQualityResponseDTO> getLatestStoredAirQualityLegacy(String inseeCode) {
        log.info("Fetching latest stored air quality data for commune: {} (legacy method)", inseeCode);

        // Validate commune exists first - prevents 500 errors for non-existent communes
        if (communeRepository.findByInseeCode(inseeCode).isEmpty()) {
            log.warn("Commune not found: {}", inseeCode);
            return Optional.empty();  // Controller will throw proper 404 ResourceNotFoundException
        }

        // Step 1: Try to get recent direct data from database
        Optional<AirQuality> directDataOpt = airQualityRepository.findLatestByCommune_InseeCode(inseeCode);

        if (directDataOpt.isPresent()) {
            AirQuality directData = directDataOpt.get();
            if (directData.getMeasurementDate().isAfter(LocalDate.now().minusDays(1))) {
                log.debug("Found recent direct air quality data for commune: {} (measurement date: {})",
                        inseeCode, directData.getMeasurementDate());
                return Optional.of(AirQualityResponseDTO.fromEntity(directData));
            }
        }

        // Step 2: No recent direct data - attempt geodistance fallback (20km threshold per PRD)
        log.info("No recent direct air quality data for commune: {}, attempting geodistance fallback (20km)",
                inseeCode);

        Optional<NearestAirQualityResult> nearestResult = geoDistanceService
            .findNearestCommuneWithAirQuality(inseeCode, 20.0);

        if (nearestResult.isPresent()) {
            NearestAirQualityResult result = nearestResult.get();
            log.info("Found air quality data from nearest commune: {} at distance: {:.2f} km (measurement date: {})",
                    result.communeName(), result.distanceKm(), result.measurementDate());

            // Convert NearestAirQualityResult to AirQualityResponseDTO
            return Optional.of(convertNearestResultToAirQualityDTO(inseeCode, result));
        }

        // Step 3: No data available within 20km threshold
        log.warn("No air quality data available within 20km radius for commune: {}", inseeCode);
        return Optional.empty();
    }

    /**
     * Evicts air quality cache for specific commune using SmartCacheService.
     *
     * @param inseeCode INSEE code of the commune
     */
    public void evictAirQualityCache(String inseeCode) {
        String cacheKey = "air-quality:" + inseeCode;
        smartCacheService.invalidate(cacheKey);
        log.info("Evicted air quality cache for INSEE code: {}", inseeCode);
    }

    /**
     * Clears all air quality cache using SmartCacheService.
     */
    public void evictAllAirQualityCache() {
        smartCacheService.clearAll();
        log.info("Cleared all air quality cache");
    }

    /**
     * Gets statistics about today's air quality data.
     *
     * @return Statistics about stored data
     */
    public AirQualityStats getTodayStats() {
        LocalDate today = LocalDate.now();
        long todayCount = airQualityRepository.countByMeasurementDate(today);
        long totalCommunes = communeRepository.count();

        return new AirQualityStats(todayCount, totalCommunes, today);
    }

    /**
     * Gets air quality for a commune with database-first and geodistance fallback.
     * Returns DIRECT data if a recent record (<1 day) exists for the commune.
     * Otherwise, returns ESTIMATED data from nearest commune within 20km.
     * Throws AirQualityDataNotFoundException if none found within 20km.
     */
    public AirQualityResponse getAirQualityWithFallback(String inseeCode) {
        Commune requested = communeRepository.findByInseeCode(inseeCode)
            .orElseThrow(() -> new ResourceNotFoundException("Commune not found: " + inseeCode));

        // Direct data path
        Optional<AirQuality> directOpt = airQualityRepository.findLatestByCommune_InseeCode(inseeCode);
        if (directOpt.isPresent()) {
            AirQuality direct = directOpt.get();
            if (direct.getMeasurementDate().isAfter(LocalDate.now().minusDays(1))) {
                Map<String, Integer> pollutants = new java.util.HashMap<>();
                pollutants.put("NO2", direct.getNo2Concentration());
                pollutants.put("O3", direct.getO3Concentration());
                pollutants.put("PM10", direct.getPm10Concentration());
                pollutants.put("PM25", direct.getPm25Concentration());
                pollutants.put("SO2", direct.getSo2Concentration());
                return AirQualityResponse.direct(
                    inseeCode,
                    requested.getName(),
                    direct.getMeasurementDate(),
                    direct.getAtmoIndex(),
                    direct.getQualifier(),
                    direct.getColor(),
                    pollutants
                );
            }
        }

        // Estimated fallback path (20km)
        Optional<NearestAirQualityResult> nearestOpt = geoDistanceService.findNearestCommuneWithAirQuality(inseeCode, 20.0);
        if (nearestOpt.isPresent()) {
            NearestAirQualityResult r = nearestOpt.get();
            Map<String, Integer> pollutants = new java.util.HashMap<>();
            pollutants.put("NO2", r.no2());
            pollutants.put("O3", r.o3());
            pollutants.put("PM10", r.pm10());
            pollutants.put("PM25", r.pm25());
            pollutants.put("SO2", r.so2());
            return AirQualityResponse.estimated(
                inseeCode,
                requested.getName(),
                r.measurementDate(),
                r.atmoIndex(),
                r.qualifier(),
                r.color(),
                pollutants,
                r.communeName(),
                r.distanceKm()
            );
        }

        // No data within threshold
        throw new AirQualityDataNotFoundException("No air quality data within 20km");
    }

    /**
     * Processes and stores a single air quality response.
     *
     * @param atmoResponse the ATMO API response
     * @return Mono<Boolean> indicating success
     */
    private Mono<Boolean> processAndStoreAirQuality(AtmoAirQualityResponse atmoResponse) {
        return convertToAirQuality(atmoResponse)
            .map(airQuality -> {
                try {
                    // Check if data already exists for today
                    LocalDate measurementDate = airQuality.getMeasurementDate();
                    Optional<AirQuality> existing = airQualityRepository
                        .findByCommuneAndMeasurementDateWithEagerLoading(airQuality.getCommune(), measurementDate);

                    if (existing.isPresent()) {
                        // Update existing record
                        AirQuality existingRecord = existing.get();
                        updateAirQualityRecord(existingRecord, airQuality);
                        airQualityRepository.save(existingRecord);
                        log.debug("Updated existing air quality record for commune: {}",
                                airQuality.getCommune().getInseeCode());
                    } else {
                        // Create new record
                        airQualityRepository.save(airQuality);
                        log.debug("Created new air quality record for commune: {}",
                                airQuality.getCommune().getInseeCode());
                    }
                    return true;
                } catch (Exception e) {
                    log.error("Failed to store air quality data for commune: {}",
                            atmoResponse.communeInsee(), e);
                    return false;
                }
            })
            .onErrorReturn(false);
    }

    /**
     * Converts ATMO API response to AirQuality entity.
     *
     * @param atmoResponse the ATMO API response
     * @return Mono containing the converted AirQuality entity
     */
    private Mono<AirQuality> convertToAirQuality(AtmoAirQualityResponse atmoResponse) {
        return Mono.fromCallable(() -> {
            log.info("Converting ATMO response for commune: {}", atmoResponse.communeInsee());
            log.info("ATMO response details: measurementDate={}, atmoIndex={}, qualifier={}, color={}",
                    atmoResponse.measurementDate(), atmoResponse.atmoIndex(), atmoResponse.qualifier(), atmoResponse.color());

            // Find the commune by INSEE code with eager loading
            Optional<Commune> communeOpt = communeRepository.findByInseeCodeWithEagerLoading(atmoResponse.communeInsee());
            if (communeOpt.isEmpty()) {
                log.error("Commune not found for INSEE code: {}", atmoResponse.communeInsee());
                throw new IllegalArgumentException("Commune not found for INSEE code: " + atmoResponse.communeInsee());
            }

            Commune commune = communeOpt.get();
            log.info("Found commune: {} (ID: {})", commune.getName(), commune.getId());

            AirQuality airQuality = new AirQuality();

            // Set basic properties
            airQuality.setCommune(commune);
            airQuality.setMeasurementDate(LocalDate.parse(atmoResponse.measurementDate()));
            airQuality.setAtmIndex(atmoResponse.atmoIndex());
            airQuality.setAtmoQual(atmoResponse.qualifier());
            airQuality.setAtmoColor(atmoResponse.color());
            airQuality.setCreatedAt(LocalDate.now());

            log.info("Set basic air quality properties: measurementDate={}, atmoIndex={}, qualifier={}, color={}",
                    airQuality.getMeasurementDate(), airQuality.getAtmIndex(), airQuality.getAtmoQual(), airQuality.getAtmoColor());

            // Map pollutant codes directly from the ATMO API response
            if (atmoResponse.no2Code() != null) {
                Integer no2Conc = convertCodeToConcentration("NO2", atmoResponse.no2Code());
                if (no2Conc != null) {
                    airQuality.setNO2(no2Conc);
                    log.info("Set NO2 concentration: {}", no2Conc);
                }
            }
            if (atmoResponse.o3Code() != null) {
                Integer o3Conc = convertCodeToConcentration("O3", atmoResponse.o3Code());
                if (o3Conc != null) {
                    airQuality.setO3(o3Conc);
                    log.info("Set O3 concentration: {}", o3Conc);
                }
            }
            if (atmoResponse.pm10Code() != null) {
                Integer pm10Conc = convertCodeToConcentration("PM10", atmoResponse.pm10Code());
                if (pm10Conc != null) {
                    airQuality.setPm10(pm10Conc);
                    log.info("Set PM10 concentration: {}", pm10Conc);
                }
            }
            if (atmoResponse.pm25Code() != null) {
                Integer pm25Concentration = convertCodeToConcentration("PM25", atmoResponse.pm25Code());
                if (pm25Concentration != null) {
                    airQuality.setPm25(pm25Concentration);
                    log.info("Set PM25 concentration: {}", pm25Concentration);
                }
            }
            if (atmoResponse.so2Code() != null) {
                Integer so2Conc = convertCodeToConcentration("SO2", atmoResponse.so2Code());
                if (so2Conc != null) {
                    airQuality.setSO2(so2Conc);
                    log.info("Set SO2 concentration: {}", so2Conc);
                }
            }

            log.info("Successfully converted ATMO response to AirQuality entity for commune: {}", atmoResponse.communeInsee());
            return airQuality;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    /**
     * Converts ATMO pollutant codes to approximate concentrations.
     *
     * @param pollutant the pollutant type (NO2, O3, PM10, PM25, SO2)
     * @param code the ATMO code (1-6)
     * @return approximate concentration in μg/m³
     */
    private Integer convertCodeToConcentration(String pollutant, Integer code) {
        if (code == null || code < 1 || code > 6) {
            return null;
        }

        // Approximate concentration ranges based on ATMO indices
        return switch (pollutant.toUpperCase()) {
            case "NO2" -> switch (code) {
                case 1 -> 20;     // Good
                case 2 -> 40;     // Moderate
                case 3 -> 90;     // Unhealthy for sensitive
                case 4 -> 120;    // Unhealthy
                case 5 -> 230;    // Very unhealthy
                case 6 -> 300;    // Hazardous
                default -> null;
            };
            case "O3" -> switch (code) {
                case 1 -> 60;
                case 2 -> 120;
                case 3 -> 160;
                case 4 -> 200;
                case 5 -> 240;
                case 6 -> 300;
                default -> null;
            };
            case "PM10" -> switch (code) {
                case 1 -> 20;
                case 2 -> 40;
                case 3 -> 50;
                case 4 -> 100;
                case 5 -> 150;
                case 6 -> 200;
                default -> null;
            };
            case "PM25" -> switch (code) {
                case 1 -> 10;
                case 2 -> 20;
                case 3 -> 25;
                case 4 -> 50;
                case 5 -> 75;
                case 6 -> 100;
                default -> null;
            };
            case "SO2" -> switch (code) {
                case 1 -> 50;
                case 2 -> 100;
                case 3 -> 200;
                case 4 -> 350;
                case 5 -> 500;
                case 6 -> 750;
                default -> null;
            };
            default -> null;
        };
    }

    /**
     * Updates an existing air quality record with new data.
     *
     * @param existing the existing record
     * @param newData the new data
     */
    private void updateAirQualityRecord(AirQuality existing, AirQuality newData) {
        existing.setAtmoIndex(newData.getAtmoIndex());
        existing.setQualifier(newData.getQualifier());
        existing.setColor(newData.getColor());
        existing.setNo2Concentration(newData.getNo2Concentration());
        existing.setO3Concentration(newData.getO3Concentration());
        existing.setPm10Concentration(newData.getPm10Concentration());
        existing.setPm25Concentration(newData.getPm25Concentration());
        existing.setSo2Concentration(newData.getSo2Concentration());
        existing.setCreatedAt(LocalDate.now()); // Update timestamp
    }

    /**
     * Converts NearestAirQualityResult to AirQualityResponseDTO.
     *
     * Used when returning estimated air quality data from nearest commune.
     * Creates a DTO representation of the air quality data for API response.
     *
     * @param requestedCommuneInseeCode INSEE code of requested commune
     * @param nearestResult Air quality result from nearest commune
     * @return AirQualityResponseDTO with estimated data
     */
    private AirQualityResponseDTO convertNearestResultToAirQualityDTO(String requestedCommuneInseeCode,
                                                                      NearestAirQualityResult nearestResult) {
        // Find the requested commune to link the DTO
        Commune commune = communeRepository.findByInseeCode(requestedCommuneInseeCode)
            .orElseThrow(() -> new IllegalArgumentException("Commune not found: " + requestedCommuneInseeCode));

        // Get department and region names
        String departmentName = commune.getDepartment() != null ? commune.getDepartment().getName() : "";
        String regionName = commune.getDepartment() != null && commune.getDepartment().getRegion() != null
            ? commune.getDepartment().getRegion().getName() : "";

        // Create AirQualityResponseDTO with nearest commune's data
        // Note: The DTO will show commune info for the requested commune,
        // but the data (pollutants, index, etc.) are from the nearest commune
        return new AirQualityResponseDTO(
            requestedCommuneInseeCode,
            commune.getName(),
            departmentName,
            regionName,
            nearestResult.measurementDate(),
            nearestResult.atmoIndex(),
            nearestResult.qualifier(),
            nearestResult.color(),
            nearestResult.no2(),
            nearestResult.o3(),
            nearestResult.pm10(),
            nearestResult.pm25(),
            nearestResult.so2(),
            LocalDate.now()  // Use current date as createdAt
        );
    }

    /**
     * Statistics record for air quality data.
     */
    public record AirQualityStats(
        long recordsToday,
        long totalCommunes,
        LocalDate date
    ) {
        public double coveragePercentage() {
            return totalCommunes > 0 ? (recordsToday * 100.0) / totalCommunes : 0.0;
        }
    }
}
