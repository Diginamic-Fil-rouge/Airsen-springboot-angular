package fr.airsen.api.service;

import fr.airsen.api.entity.AirQuality;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.external.client.AtmoApiClient;
import fr.airsen.api.external.dto.atmo.AtmoAirQualityResponse;
import fr.airsen.api.repository.AirQualityRepository;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Service for integrating ATMO air quality data with the application.
 *
 * Handles data transformation, validation, and persistence
 * for air quality measurements from external ATMO API.
 */
@Service
@Transactional
public class AirQualityService {

    private static final Logger log = LoggerFactory.getLogger(AirQualityService.class);

    private final AtmoApiClient atmoApiClient;
    private final AirQualityRepository airQualityRepository;
    private final CommuneRepository communeRepository;

    public AirQualityService(AtmoApiClient atmoApiClient,
                            AirQualityRepository airQualityRepository,
                            CommuneRepository communeRepository)
                            {
        this.atmoApiClient = atmoApiClient;
        this.airQualityRepository = airQualityRepository;
        this.communeRepository = communeRepository;
    }

    /**
     * Updates air quality data for all tracked communes.
     *
     * This method is called by scheduled tasks to keep air quality data current.
     *
     * @return Mono<Void> indicating completion
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public Mono<Void> updateAllAirQualityData() {
        log.info("Starting scheduled air quality data update");

        List<Commune> communes = communeRepository.findAll();
        log.info("Found {} communes to update", communes.size());

        return Flux.fromIterable(communes)
            .flatMap(commune ->
                updateAirQualityForCommune(commune.getInseeCode())
                    .onErrorContinue((error, commune_obj) ->
                        log.warn("Failed to update air quality for commune: {}",
                               commune.getInseeCode(), error)))
            .then()
            .doOnSuccess(v -> log.info("Completed scheduled air quality data update"))
            .doOnError(error -> log.error("Failed to complete air quality data update", error));
    }

    /**
     * Updates air quality data for a specific commune.
     *
     * @param communeInseeCode INSEE code of the commune
     * @return Mono<AirQuality> containing the updated data
     */
    public Mono<AirQuality> updateAirQualityForCommune(String communeInseeCode) {
        log.info("Updating air quality data for commune: {}", communeInseeCode);

        return atmoApiClient.getCurrentAirQuality(communeInseeCode)
            .map(this::mapToEntity)
            .flatMap(airQuality -> {
                AirQuality saved = airQualityRepository.save(airQuality);

                // Trigger alert processing for the new data
//                alertProcessingService.processAirQualityUpdate(saved);

                log.info("Successfully updated air quality data for commune: {} with ATMO index: {}",
                        communeInseeCode, saved.getAtmoIndex());

                return Mono.just(saved);
            });
    }

    /**
     * Gets current air quality data for a commune (cached for 1 hour).
     *
     * @param communeInseeCode INSEE code of the commune
     * @return Mono<AirQuality> containing current data
     */
    @Cacheable(value = "air-quality", key = "#communeInseeCode", unless = "#result == null")
    public Mono<AirQuality> getCurrentAirQuality(String communeInseeCode) {
        log.info("Cache miss - Fetching air quality data from ATMO API for INSEE code: {}", communeInseeCode);

        // Try to get from database first
        Optional<AirQuality> existingOpt = airQualityRepository.findLatestByCommune_InseeCode(communeInseeCode);

        if (existingOpt.isPresent()) {
            AirQuality existing = existingOpt.get();
            if (existing.getMeasurementDate().isAfter(LocalDate.now().minusDays(1))) {
                log.debug("Returning recent air quality data from database for commune: {}", communeInseeCode);
                return Mono.just(existing);
            }
        }

        // Fetch fresh data if no recent data exists
        log.info("Fetching fresh air quality data for commune: {}", communeInseeCode);
        return updateAirQualityForCommune(communeInseeCode);
    }

    /**
     * Evicts cache for specific commune.
     *
     * @param communeInseeCode INSEE code of the commune
     */
    @CacheEvict(value = "air-quality", key = "#communeInseeCode")
    public void evictAirQualityCache(String communeInseeCode) {
        log.info("Evicted air quality cache for INSEE code: {}", communeInseeCode);
    }

    /**
     * Clears all air quality cache.
     */
    @CacheEvict(value = "air-quality", allEntries = true)
    public void evictAllAirQualityCache() {
        log.info("Cleared all air quality cache");
    }

    /**
     * Gets historical air quality data for a commune.
     *
     * @param communeInseeCode INSEE code of the commune
     * @param startDate start date for historical data
     * @param endDate end date for historical data
     * @return Flux<AirQuality> containing historical data
     */
    public Flux<AirQuality> getHistoricalAirQuality(String communeInseeCode,
                                                   LocalDate startDate,
                                                   LocalDate endDate) {
        log.info("Fetching historical air quality data for commune: {} from {} to {}",
                communeInseeCode, startDate, endDate);

        return atmoApiClient.getHistoricalAirQuality(communeInseeCode, startDate, endDate)
            .map(this::mapToEntity)
            .flatMap(airQuality -> {
                AirQuality saved = airQualityRepository.save(airQuality);
                return Mono.just(saved);
            })
            .doOnComplete(() -> log.info("Completed historical data fetch for commune: {}",
                                       communeInseeCode));
    }

    /**
     * Forces update of air quality data for a commune (bypasses cache).
     *
     * @param communeInseeCode INSEE code of the commune
     * @return Mono<AirQuality> containing the updated data
     */
    public Mono<AirQuality> forceUpdateAirQuality(String communeInseeCode) {
        log.info("Force updating air quality data for commune: {}", communeInseeCode);
        return updateAirQualityForCommune(communeInseeCode);
    }

    /**
     * Get daily air quality measurements for current date.
     *
     * Business logic concept from air_quality_service (commit 6be37b8),
     * implemented with reactive patterns and proper error handling.
     *
     * @return Flux of current day air quality data
     */
    public Flux<AirQuality> getAllDailyAirQualities() {
        LocalDate today = LocalDate.now();
        log.info("Fetching daily air quality data for date: {}", today);

        return Flux.fromIterable(airQualityRepository.findByMeasurementDate(today))
            .doOnNext(airQuality -> log.debug("Found air quality data for commune: {}",
                                             airQuality.getCommune().getInseeCode()))
            .doOnComplete(() -> log.info("Completed daily air quality data fetch"))
            .onErrorResume(error -> {
                log.error("Error fetching daily air quality data", error);
                return Flux.empty();
            });
    }

    /**
     * Maps ATMO API response to AirQuality entity.
     *
     * @param response ATMO API response
     * @return AirQuality entity
     */
    private AirQuality mapToEntity(AtmoAirQualityResponse response) {
        Commune commune = communeRepository.findByInseeCode(response.communeInsee())
            .orElseThrow(() -> new ResourceNotFoundException("Commune not found: " + response.communeInsee()));

        AirQuality airQuality = new AirQuality();
        airQuality.setCommune(commune);
        airQuality.setMeasurementDate(LocalDate.parse(response.measurementDate()));
        airQuality.setAtmIndex(response.atmoIndex());
        airQuality.setAtmoQual(response.qualifier());
        airQuality.setAtmoColor(response.color());

        // Map pollutant codes from ATMO API
        if (response.no2Code() != null) {
            Integer no2Concentration = convertCodeToConcentration("NO2", response.no2Code());
            if (no2Concentration != null) {
                airQuality.setNO2(no2Concentration);
            }
        }
        if (response.o3Code() != null) {
            Integer o3Concentration = convertCodeToConcentration("O3", response.o3Code());
            if (o3Concentration != null) {
                airQuality.setO3(o3Concentration);
            }
        }
        if (response.pm10Code() != null) {
            Integer pm10Concentration = convertCodeToConcentration("PM10", response.pm10Code());
            if (pm10Concentration != null) {
                airQuality.setPm10(pm10Concentration);
            }
        }
        if (response.pm25Code() != null) {
            Integer pm25Concentration = convertCodeToConcentration("PM25", response.pm25Code());
            if (pm25Concentration != null) {
                airQuality.setPm25(pm25Concentration);
            }
        }
        if (response.so2Code() != null) {
            Integer so2Concentration = convertCodeToConcentration("SO2", response.so2Code());
            if (so2Concentration != null) {
                airQuality.setSO2(so2Concentration);
            }
        }

        return airQuality;
    }

    /**
     * Converts ATMO pollutant codes to approximate integer concentrations.
     *
     * @param pollutant the pollutant type (NO2, O3, PM10, PM25, SO2)
     * @param code the ATMO code (1-6)
     * @return approximate concentration in μg/m³ as Integer
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
}
