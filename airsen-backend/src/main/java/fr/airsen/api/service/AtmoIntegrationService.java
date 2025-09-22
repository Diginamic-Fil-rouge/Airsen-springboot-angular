package fr.airsen.api.service;

import fr.airsen.api.entity.AirQuality;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.external.client.AtmoApiClient;
import fr.airsen.api.external.dto.atmo.AtmoAirQualityResponse;
import fr.airsen.api.external.exception.AtmoApiException;
import fr.airsen.api.repository.AirQualityRepository;
import fr.airsen.api.repository.CommuneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
     * Runs every hour during business hours (8 AM to 8 PM).
     */
    @Scheduled(cron = "0 0 8-20 * * *") // Every hour from 8 AM to 8 PM
    @Async
    public void scheduledDataSync() {
        log.info("Starting scheduled ATMO data synchronization");
        
        syncCurrentAirQualityData()
            .doOnSuccess(count -> log.info("Scheduled sync completed successfully. {} records processed", count))
            .doOnError(error -> log.error("Scheduled sync failed", error))
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
     * 
     * @param inseeCode INSEE code of the commune
     * @return Mono containing the stored AirQuality entity
     */
    @Cacheable(value = "airQuality", key = "#inseeCode + '_' + T(java.time.LocalDate).now()")
    public Mono<Optional<AirQuality>> getAirQualityForCommune(String inseeCode) {
        log.debug("Fetching air quality data for commune: {}", inseeCode);
        
        return atmoApiClient.getCurrentAirQuality(inseeCode)
            .flatMap(this::convertToAirQuality)
            .map(Optional::of)
            .onErrorReturn(Optional.empty())
            .doOnNext(result -> {
                if (result.isPresent()) {
                    log.debug("Successfully retrieved air quality for commune: {}", inseeCode);
                } else {
                    log.warn("No air quality data available for commune: {}", inseeCode);
                }
            });
    }

    /**
     * Gets the latest stored air quality data for a commune.
     * 
     * @param inseeCode INSEE code of the commune
     * @return Optional containing the latest AirQuality entity
     */
    public Optional<AirQuality> getLatestStoredAirQuality(String inseeCode) {
        return airQualityRepository.findLatestByCommune_InseeCode(inseeCode);
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
                        .findByCommuneAndMeasurementDate(airQuality.getCommune(), measurementDate);
                    
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
            // Find the commune by INSEE code
            Optional<Commune> communeOpt = communeRepository.findByInseeCode(atmoResponse.communeInsee());
            if (communeOpt.isEmpty()) {
                throw new IllegalArgumentException("Commune not found for INSEE code: " + atmoResponse.communeInsee());
            }

            Commune commune = communeOpt.get();
            AirQuality airQuality = new AirQuality();
            
            // Set basic properties
            airQuality.setCommune(commune);
            airQuality.setMeasurementDate(LocalDate.parse(atmoResponse.measurementDate()));
            airQuality.setAtmIndex(atmoResponse.atmoIndex());
            airQuality.setAtmoQual(atmoResponse.qualifier());
            airQuality.setAtmoColor(atmoResponse.color());
            airQuality.setCreatedAt(LocalDate.now());

            // Map pollutant codes directly from the ATMO API response
            if (atmoResponse.no2Code() != null) {
                airQuality.setNO2(convertCodeToConcentration("NO2", atmoResponse.no2Code()));
            }
            if (atmoResponse.o3Code() != null) {
                airQuality.setO3(convertCodeToConcentration("O3", atmoResponse.o3Code()));
            }
            if (atmoResponse.pm10Code() != null) {
                airQuality.setPm10(convertCodeToConcentration("PM10", atmoResponse.pm10Code()));
            }
            if (atmoResponse.pm25Code() != null) {
                Double pm25Concentration = convertCodeToConcentration("PM25", atmoResponse.pm25Code());
                if (pm25Concentration != null) {
                    airQuality.setPm25(pm25Concentration.intValue());
                }
            }
            if (atmoResponse.so2Code() != null) {
                airQuality.setSO2(convertCodeToConcentration("SO2", atmoResponse.so2Code()));
            }

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
    private Double convertCodeToConcentration(String pollutant, Integer code) {
        if (code == null || code < 1 || code > 6) {
            return null;
        }
        
        // Approximate concentration ranges based on ATMO indices
        return switch (pollutant.toUpperCase()) {
            case "NO2" -> switch (code) {
                case 1 -> 20.0;   // Good
                case 2 -> 40.0;   // Moderate  
                case 3 -> 90.0;   // Unhealthy for sensitive
                case 4 -> 120.0;  // Unhealthy
                case 5 -> 230.0;  // Very unhealthy
                case 6 -> 300.0;  // Hazardous
                default -> null;
            };
            case "O3" -> switch (code) {
                case 1 -> 60.0;
                case 2 -> 120.0;
                case 3 -> 160.0;
                case 4 -> 200.0;
                case 5 -> 240.0;
                case 6 -> 300.0;
                default -> null;
            };
            case "PM10" -> switch (code) {
                case 1 -> 20.0;
                case 2 -> 40.0;
                case 3 -> 50.0;
                case 4 -> 100.0;
                case 5 -> 150.0;
                case 6 -> 200.0;
                default -> null;
            };
            case "PM25" -> switch (code) {
                case 1 -> 10.0;
                case 2 -> 20.0;
                case 3 -> 25.0;
                case 4 -> 50.0;
                case 5 -> 75.0;
                case 6 -> 100.0;
                default -> null;
            };
            case "SO2" -> switch (code) {
                case 1 -> 50.0;
                case 2 -> 100.0;
                case 3 -> 200.0;
                case 4 -> 350.0;
                case 5 -> 500.0;
                case 6 -> 750.0;
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