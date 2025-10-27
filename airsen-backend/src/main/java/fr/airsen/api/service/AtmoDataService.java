package fr.airsen.api.service;

import fr.airsen.api.entity.AirQuality;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.external.client.AtmoApiClient;
import fr.airsen.api.external.dto.atmo.AtmoAirQualityResponse;
import fr.airsen.api.repository.AirQualityRepository;
import fr.airsen.api.repository.CommuneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

/**
 * Service for managing ATMO air quality data integration.
 * 
 * Handles fetching data from the ATMO France API and storing
 * it in the local database with proper mapping.
 */
@Service
@Transactional
public class AtmoDataService {

    private static final Logger log = LoggerFactory.getLogger(AtmoDataService.class);

    private final AtmoApiClient atmoApiClient;
    private final AirQualityRepository airQualityRepository;
    private final CommuneRepository communeRepository;

    public AtmoDataService(AtmoApiClient atmoApiClient,
                           AirQualityRepository airQualityRepository,
                           CommuneRepository communeRepository) {
        this.atmoApiClient = atmoApiClient;
        this.airQualityRepository = airQualityRepository;
        this.communeRepository = communeRepository;
    }

    /**
     * Fetches and stores current air quality data for all available communes.
     *
     * @return Mono containing the count of stored records
     */
    public Mono<Integer> fetchAndStoreCurrentAirQuality() {
        log.info("Starting fetch and store operation for current air quality data");

        return atmoApiClient.getCurrentAirQualityIndices()
                .flatMap(this::convertAndSaveAirQuality)
                .collectList()
                .map(savedEntities -> {
                    int count = savedEntities.size();
                    log.info("Successfully stored {} air quality records", count);
                    return count;
                })
                .doOnError(error -> log.error("Failed to fetch and store air quality data", error));
    }

    /**
     * Fetches current air quality data for a specific commune.
     *
     * @param communeInseeCode INSEE code of the commune
     * @return Mono containing the stored AirQuality entity
     */
    public Mono<AirQuality> fetchAndStoreAirQualityForCommune(String communeInseeCode) {
        log.info("Fetching air quality data for commune: {}", communeInseeCode);

        return atmoApiClient.getCurrentAirQuality(communeInseeCode)
                .flatMap(this::convertAndSaveAirQuality)
                .switchIfEmpty(Mono.error(new IllegalArgumentException(
                        "Commune " + communeInseeCode + " not found in database. Please ensure the commune exists before fetching air quality data.")))
                .doOnSuccess(entity -> log.info("Successfully stored air quality data for commune: {}", communeInseeCode))
                .doOnError(error -> log.error("Failed to fetch air quality for commune: {}", communeInseeCode, error));
    }

    /**
     * Tests the ATMO API connection by fetching a small sample of data.
     *
     * @return Mono containing success indicator
     */
    public Mono<Boolean> testApiConnection() {
        log.info("Testing ATMO API connection");

        return atmoApiClient.getCurrentAirQualityIndices()
                .take(1)
                .hasElements()
                .doOnSuccess(hasData -> {
                    if (hasData) {
                        log.info("ATMO API connection test successful");
                    } else {
                        log.warn("ATMO API connection test returned no data");
                    }
                })
                .doOnError(error -> log.error("ATMO API connection test failed", error))
                .onErrorReturn(false);
    }

    /**
     * Retrieves the count of stored air quality records for today.
     *
     * @return count of today's records
     */
    public long getTodayAirQualityCount() {
        LocalDate today = LocalDate.now();
        return airQualityRepository.countByMeasurementDate(today);
    }

    /**
     * Converts ATMO API response to AirQuality entity and saves it to database.
     *
     * @param atmoResponse the ATMO API response
     * @return Mono containing the saved AirQuality entity
     */
    private Mono<AirQuality> convertAndSaveAirQuality(AtmoAirQualityResponse atmoResponse) {
        return Mono.fromCallable(() -> communeRepository.findByInseeCode(atmoResponse.communeInsee()))
                .flatMap(optionalCommune -> {
                    if (optionalCommune.isEmpty()) {
                        log.warn("Commune not found in database for INSEE code: {} (commune name: {}). ATMO data exists but cannot be stored without commune reference.",
                                atmoResponse.communeInsee(), atmoResponse.zoneName());
                        return Mono.empty();
                    }

                    Commune commune = optionalCommune.get();

                    // Create AirQuality entity
                    AirQuality airQuality = new AirQuality();
                    airQuality.setCommune(commune);
                    airQuality.setMeasurementDate(LocalDate.parse(atmoResponse.measurementDate()));
                    airQuality.setAtmIndex(atmoResponse.atmoIndex());
                    airQuality.setAtmoQual(atmoResponse.qualifier());
                    airQuality.setAtmoColor(atmoResponse.color());
                    airQuality.setCreatedAt(LocalDate.now());

                    // Map pollutant codes
                    if (atmoResponse.no2Code() != null) {
                        Integer no2Conc = convertCodeToConcentration("NO2", atmoResponse.no2Code());
                        if (no2Conc != null) {
                            airQuality.setNO2(no2Conc);
                        }
                    }
                    if (atmoResponse.o3Code() != null) {
                        Integer o3Conc = convertCodeToConcentration("O3", atmoResponse.o3Code());
                        if (o3Conc != null) {
                            airQuality.setO3(o3Conc);
                        }
                    }
                    if (atmoResponse.pm10Code() != null) {
                        Integer pm10Conc = convertCodeToConcentration("PM10", atmoResponse.pm10Code());
                        if (pm10Conc != null) {
                            airQuality.setPm10(pm10Conc);
                        }
                    }
                    if (atmoResponse.pm25Code() != null) {
                        Integer pm25Concentration = convertCodeToConcentration("PM25", atmoResponse.pm25Code());
                        if (pm25Concentration != null) {
                            airQuality.setPm25(pm25Concentration);
                        }
                    }
                    if (atmoResponse.so2Code() != null) {
                        Integer so2Conc = convertCodeToConcentration("SO2", atmoResponse.so2Code());
                        if (so2Conc != null) {
                            airQuality.setSO2(so2Conc);
                        }
                    }

                    // Save to database
                    return Mono.just(airQualityRepository.save(airQuality));
                });
    }

    /**
     * Converts ATMO pollutant codes to approximate integer concentrations.
     *
     * @param pollutant the pollutant type (NO2, O3, PM10, PM25, SO2)
     * @param code      the ATMO code (1-6)
     * @return approximate concentration in μg/m³ as Integer
     */
    private Integer convertCodeToConcentration(String pollutant, Integer code) {
        if (code == null || code < 1 || code > 6) {
            return null;
        }

        // Approximate concentration ranges based on ATMO indices
        // These are simplified mappings for demonstration purposes
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