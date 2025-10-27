package fr.airsen.api.service;

import fr.airsen.api.dto.AirQualityResponseDTO;
import fr.airsen.api.entity.AirQuality;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.external.client.AtmoApiClient;
import fr.airsen.api.external.dto.atmo.AtmoAirQualityResponse;
import fr.airsen.api.repository.AirQualityRepository;
import fr.airsen.api.repository.CommuneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    public AtmoIntegrationService(AtmoApiClient atmoApiClient,
                                 AirQualityRepository airQualityRepository,
                                 CommuneRepository communeRepository) {
        this.atmoApiClient = atmoApiClient;
        this.airQualityRepository = airQualityRepository;
        this.communeRepository = communeRepository;
    }

    /**
     * Scheduled task to fetch and store current ATMO air quality data.
     *
     * Runs daily at midnight (configurable via scheduling.atmo.cron property).
     * Default: 00:00 Europe/Paris timezone.
     *
     * Configuration:
     * - scheduling.atmo.cron: Cron expression (default: 0 0 0 * * *)
     * - scheduling.atmo.timezone: Timezone (default: Europe/Paris)
     */
    @Scheduled(cron = "${scheduling.atmo.cron}", zone = "${scheduling.atmo.timezone}")
    @Async
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
            return getLatestStoredAirQuality(inseeCode);
        }
    }

    /**
     * Gets the latest stored air quality data for a commune as DTO.
     * 
     * @param inseeCode INSEE code of the commune
     * @return Optional containing the latest air quality data as DTO
     */
    public Optional<AirQualityResponseDTO> getLatestStoredAirQuality(String inseeCode) {
        Optional<AirQuality> airQuality = airQualityRepository.findLatestByCommune_InseeCode(inseeCode);
        return airQuality.map(AirQualityResponseDTO::fromEntity);
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