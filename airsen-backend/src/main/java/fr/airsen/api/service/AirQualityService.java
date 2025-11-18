package fr.airsen.api.service;

import fr.airsen.api.dto.response.NearestAirQualityResult;
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
    private final GeoDistanceService geoDistanceService;

    public AirQualityService(AtmoApiClient atmoApiClient,
                            AirQualityRepository airQualityRepository,
                            CommuneRepository communeRepository,
                            GeoDistanceService geoDistanceService) {
        this.atmoApiClient = atmoApiClient;
        this.airQualityRepository = airQualityRepository;
        this.communeRepository = communeRepository;
        this.geoDistanceService = geoDistanceService;
    }

    /**
     * Updates air quality data for all tracked communes.
     *
     * NOTE: This method is NO LONGER scheduled automatically to prevent rate limiting.
     * Use CacheAwareTieredScheduler for intelligent, tiered cache refresh instead.
     * This method is kept for manual invocation or testing purposes only.
     *
     * @return Mono<Void> indicating completion
     * @deprecated Use CacheAwareTieredScheduler for scheduled updates (population-based tiers)
     */
    // @Scheduled(fixedRate = 3600000) // DISABLED: Replaced by CacheAwareTieredScheduler
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
     * Gets current air quality data for a commune with geodistance fallback.
     *
     * Data retrieval strategy (per PRD):
     * 1. Query database for recent data (< 1 day old) from requested commune
     * 2. If no recent direct data exists, attempt geodistance fallback:
     *    - Find nearest commune with air quality data within 20km radius
     *    - Return estimated data from nearest commune
     * 3. If no data within 20km threshold, return empty Optional
     *
     * Database is populated by scheduled updates via CacheAwareTieredScheduler.
     * This method never calls external APIs directly - only reads from database.
     *
     * @param communeInseeCode INSEE code of the commune
     * @return Mono<AirQuality> containing air quality data (direct or estimated from nearest commune)
     */
    public Mono<AirQuality> getCurrentAirQuality(String communeInseeCode) {
        log.info("Fetching air quality data for commune: {}", communeInseeCode);

        // Step 1: Try to get recent direct data from database
        Optional<AirQuality> directDataOpt = airQualityRepository.findLatestByCommune_InseeCode(communeInseeCode);

        if (directDataOpt.isPresent()) {
            AirQuality directData = directDataOpt.get();
            if (directData.getMeasurementDate().isAfter(LocalDate.now().minusDays(1))) {
                log.debug("Found recent direct air quality data for commune: {} (measurement date: {})",
                        communeInseeCode, directData.getMeasurementDate());
                return Mono.just(directData);
            }
        }

        // Step 2: No recent direct data - attempt geodistance fallback (20km threshold per PRD)
        log.info("No recent direct air quality data for commune: {}, attempting geodistance fallback (20km)",
                communeInseeCode);

        Optional<NearestAirQualityResult> nearestResult = geoDistanceService
            .findNearestCommuneWithAirQuality(communeInseeCode, 20.0);

        if (nearestResult.isPresent()) {
            NearestAirQualityResult result = nearestResult.get();
            log.info("Found air quality data from nearest commune: {} at distance: {:.2f} km (measurement date: {})",
                    result.communeName(), result.distanceKm(), result.measurementDate());

            // Convert NearestAirQualityResult to AirQuality entity for return compatibility
            return Mono.just(convertNearestResultToAirQuality(communeInseeCode, result));
        }

        // Step 3: No data available within 20km threshold
        log.warn("No air quality data available within 20km radius for commune: {}", communeInseeCode);
        return Mono.empty();
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

    /**
     * Converts NearestAirQualityResult to AirQuality entity.
     *
     * Used when returning estimated air quality data from nearest commune.
     * The returned entity represents data from the nearest commune,
     * not the originally requested commune.
     *
     * @param requestedCommuneInseeCode INSEE code of requested commune
     * @param nearestResult Air quality result from nearest commune
     * @return AirQuality entity with estimated data
     */
    private AirQuality convertNearestResultToAirQuality(String requestedCommuneInseeCode,
                                                        NearestAirQualityResult nearestResult) {
        Commune estimatedCommune = communeRepository.findByInseeCode(requestedCommuneInseeCode)
            .orElseThrow(() -> new ResourceNotFoundException("Commune not found: " + requestedCommuneInseeCode));

        AirQuality airQuality = new AirQuality();
        airQuality.setCommune(estimatedCommune);
        airQuality.setMeasurementDate(nearestResult.measurementDate());
        airQuality.setAtmIndex(nearestResult.atmoIndex());
        airQuality.setAtmoQual(nearestResult.qualifier());
        airQuality.setAtmoColor(nearestResult.color());
        airQuality.setNO2(nearestResult.no2());
        airQuality.setO3(nearestResult.o3());
        airQuality.setPm10(nearestResult.pm10());
        airQuality.setPm25(nearestResult.pm25());
        airQuality.setSO2(nearestResult.so2());

        return airQuality;
    }

}
