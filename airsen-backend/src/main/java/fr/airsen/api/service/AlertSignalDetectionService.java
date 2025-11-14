package fr.airsen.api.service;

import fr.airsen.api.dto.request.CreateManualSignalRequest;
import fr.airsen.api.entity.AlertSignal;
import fr.airsen.api.entity.Region;
import fr.airsen.api.entity.enums.*;
import fr.airsen.api.external.client.AtmoApiClient;
import fr.airsen.api.external.client.OpenMeteoApiClient;
import fr.airsen.api.external.dto.atmo.AtmoAirQualityResponse;
import fr.airsen.api.external.dto.openmeteo.OpenMeteoForecastResponse;
import fr.airsen.api.repository.AlertSignalRepository;
import fr.airsen.api.repository.RegionRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for detecting environmental alert signals from external APIs.
 * 
 * This service automatically detects:
 * - Air pollution episodes from ATMO France API
 * - Weather alerts from Open-Meteo API (heat, wind, rain)
 * 
 * Signals are stored in the database for admin review and campaign creation.
 */
@Service
@Transactional
public class AlertSignalDetectionService {

    private static final Logger log = LoggerFactory.getLogger(AlertSignalDetectionService.class);

    private final AlertSignalRepository alertSignalRepository;
    private final AtmoApiClient atmoApiClient;
    private final OpenMeteoApiClient openMeteoApiClient;
    private final RegionRepository regionRepository;

    // Weather thresholds for alert detection
    private static final double HEAT_THRESHOLD_CELSIUS = 35.0;
    private static final double WIND_THRESHOLD_KMH = 70.0;
    private static final double RAIN_THRESHOLD_MM = 30.0;

    public AlertSignalDetectionService(AlertSignalRepository alertSignalRepository,
                                      AtmoApiClient atmoApiClient,
                                      OpenMeteoApiClient openMeteoApiClient,
                                      RegionRepository regionRepository) {
        this.alertSignalRepository = alertSignalRepository;
        this.atmoApiClient = atmoApiClient;
        this.openMeteoApiClient = openMeteoApiClient;
        this.regionRepository = regionRepository;
    }

    /**
     * Scheduled task that runs hourly to detect all alert signals.
     *
     * This method is automatically executed every hour and orchestrates
     * the detection of both ATMO pollution episodes and weather alerts.
     *
     * Configuration:
     * - alert.detection.cron: Cron expression (default: 0 0 * * * *)
     * - alert.detection.enabled: Enable/disable alert detection (default: true in prod, false in dev)
     *
     * Can be disabled by setting alert.detection.enabled=false in application.yml
     */
    @Scheduled(cron = "${alert.detection.cron:0 0 * * * *}")
    @ConditionalOnProperty(
        value = "alert.detection.enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    public void detectAllSignals() {
        log.info("Starting scheduled alert signal detection");
        
        try {
            List<AlertSignal> atmoSignals = detectAtmoSignals();
            List<AlertSignal> weatherSignals = detectWeatherSignals();
            
            int totalDetected = atmoSignals.size() + weatherSignals.size();
            log.info("Signal detection completed: {} ATMO signals, {} weather signals detected (total: {})",
                    atmoSignals.size(), weatherSignals.size(), totalDetected);
                    
        } catch (Exception e) {
            log.error("Unexpected error during scheduled signal detection", e);
        }
    }

    /**
     * Detects air pollution episodes from ATMO France API.
     * 
     * Retrieves official pollution episodes and creates AlertSignal entities
     * for each detected episode. ATMO episodes are always classified as ALERT level
     * since they represent official pollution warnings.
     * 
     * @return list of created alert signals
     */
    @Transactional
    public List<AlertSignal> detectAtmoSignals() {
        log.info("Starting ATMO pollution episode detection");
        List<AlertSignal> detectedSignals = new ArrayList<>();
        
        try {
            List<AtmoAirQualityResponse> airQualityData = atmoApiClient.getCurrentAirQualityIndices()
                .collectList()
                .block();
            
            if (airQualityData == null || airQualityData.isEmpty()) {
                log.info("No ATMO air quality data available for processing");
                return detectedSignals;
            }
            
            log.debug("Processing {} ATMO air quality entries", airQualityData.size());
            
            for (AtmoAirQualityResponse data : airQualityData) {
                try {
                    if (isAtmoEpisode(data)) {
                        AlertSignal signal = createAtmoAlertSignal(data);
                        AlertSignal saved = alertSignalRepository.save(signal);
                        detectedSignals.add(saved);
                        log.debug("Created ATMO signal: {} for commune {}", saved.getId(), data.communeInsee());
                    }
                } catch (Exception e) {
                    log.error("Error processing ATMO data for commune {}: {}", 
                            data.communeInsee(), e.getMessage());
                }
            }
            
            log.info("ATMO detection completed: {} signals created", detectedSignals.size());
            
        } catch (Exception e) {
            log.error("Failed to detect ATMO signals", e);
        }
        
        return detectedSignals;
    }

    /**
     * Detects weather-related alert conditions from Open-Meteo API.
     * 
     * Checks weather forecasts for all regions and creates alert signals when
     * thresholds are exceeded:
     * - Heat: max temperature >= 35°C
     * - Wind: max wind speed >= 70 km/h
     * - Rain: precipitation >= 30 mm
     * 
     * @return list of created weather alert signals
     */
    @Transactional
    public List<AlertSignal> detectWeatherSignals() {
        log.info("Starting weather alert detection for all regions");
        List<AlertSignal> detectedSignals = new ArrayList<>();
        
        try {
            List<Region> regions = regionRepository.findAll();
            
            if (regions.isEmpty()) {
                log.warn("No regions found in database for weather detection");
                return detectedSignals;
            }
            
            log.debug("Processing weather data for {} regions", regions.size());
            
            for (Region region : regions) {
                try {
                    List<AlertSignal> regionSignals = detectWeatherSignalsForRegion(region);
                    detectedSignals.addAll(regionSignals);
                } catch (Exception e) {
                    log.error("Error processing weather data for region {}: {}", 
                            region.getName(), e.getMessage());
                }
            }
            
            log.info("Weather detection completed: {} signals created", detectedSignals.size());
            
        } catch (Exception e) {
            log.error("Failed to detect weather signals", e);
        }
        
        return detectedSignals;
    }

    /**
     * Detects weather alerts for a specific region.
     * 
     * @param region the region to check
     * @return list of detected weather alert signals
     */
    private List<AlertSignal> detectWeatherSignalsForRegion(Region region) {
        List<AlertSignal> signals = new ArrayList<>();
        
        Double[] coordinates = getRegionCoordinates(region);
        if (coordinates == null) {
            log.warn("No coordinates available for region: {}", region.getName());
            return signals;
        }
        
        try {
            OpenMeteoForecastResponse forecast = openMeteoApiClient
                .getWeatherForecast(coordinates[1], coordinates[0], 3)
                .block();
            
            if (forecast == null || forecast.daily() == null) {
                log.warn("No forecast data available for region: {}", region.getName());
                return signals;
            }
            
            signals.addAll(checkHeatAlerts(forecast, region));
            signals.addAll(checkWindAlerts(forecast, region));
            signals.addAll(checkRainAlerts(forecast, region));
            
        } catch (Exception e) {
            log.error("Failed to fetch weather forecast for region {}: {}", 
                    region.getName(), e.getMessage());
        }
        
        return signals;
    }

    /**
     * Checks for heat alert conditions in weather forecast.
     * 
     * @param forecast weather forecast data
     * @param region the region being checked
     * @return list of heat alert signals
     */
    private List<AlertSignal> checkHeatAlerts(OpenMeteoForecastResponse forecast, Region region) {
        List<AlertSignal> signals = new ArrayList<>();
        
        List<Double> maxTemps = forecast.daily().maxTemperatures();
        if (maxTemps == null || maxTemps.isEmpty()) {
            return signals;
        }
        
        Double maxTemp = maxTemps.stream()
            .filter(temp -> temp != null)
            .max(Double::compareTo)
            .orElse(0.0);
        
        if (maxTemp >= HEAT_THRESHOLD_CELSIUS) {
            AlertSignal signal = new AlertSignal();
            signal.setSource(AlertSignalSource.WEATHER);
            signal.setKind(AlertSignalKind.HEAT);
            signal.setLevel(AlertSignalLevel.ALERT);
            signal.setScopeType(GeographicScopeType.REGION);
            signal.setScopeId(region.getId());
            signal.setSummary(String.format("Heat alert: %.1f°C expected in %s", maxTemp, region.getName()));
            signal.setDetails(String.format("Maximum temperature of %.1f°C forecasted, exceeding alert threshold of %.1f°C", 
                    maxTemp, HEAT_THRESHOLD_CELSIUS));
            signal.setDetectedAt(LocalDateTime.now());
            signal.setValidFrom(LocalDateTime.now());
            signal.setValidTo(LocalDateTime.now().plusDays(3));
            
            AlertSignal saved = alertSignalRepository.save(signal);
            signals.add(saved);
            log.info("Heat alert created for region {}: {}°C", region.getName(), maxTemp);
        }
        
        return signals;
    }

    /**
     * Checks for wind alert conditions in weather forecast.
     * 
     * @param forecast weather forecast data
     * @param region the region being checked
     * @return list of wind alert signals
     */
    private List<AlertSignal> checkWindAlerts(OpenMeteoForecastResponse forecast, Region region) {
        List<AlertSignal> signals = new ArrayList<>();
        
        List<Double> maxWindSpeeds = forecast.daily().maxWindSpeed();
        if (maxWindSpeeds == null || maxWindSpeeds.isEmpty()) {
            return signals;
        }
        
        Double maxWind = maxWindSpeeds.stream()
            .filter(wind -> wind != null)
            .max(Double::compareTo)
            .orElse(0.0);
        
        if (maxWind >= WIND_THRESHOLD_KMH) {
            AlertSignal signal = new AlertSignal();
            signal.setSource(AlertSignalSource.WEATHER);
            signal.setKind(AlertSignalKind.WIND);
            signal.setLevel(AlertSignalLevel.ALERT);
            signal.setScopeType(GeographicScopeType.REGION);
            signal.setScopeId(region.getId());
            signal.setSummary(String.format("Strong wind expected in %s: %.1f km/h", region.getName(), maxWind));
            signal.setDetails(String.format("Maximum wind speed of %.1f km/h forecasted, exceeding alert threshold of %.1f km/h", 
                    maxWind, WIND_THRESHOLD_KMH));
            signal.setDetectedAt(LocalDateTime.now());
            signal.setValidFrom(LocalDateTime.now());
            signal.setValidTo(LocalDateTime.now().plusDays(3));
            
            AlertSignal saved = alertSignalRepository.save(signal);
            signals.add(saved);
            log.info("Wind alert created for region {}: {} km/h", region.getName(), maxWind);
        }
        
        return signals;
    }

    /**
     * Checks for rain alert conditions in weather forecast.
     * 
     * @param forecast weather forecast data
     * @param region the region being checked
     * @return list of rain alert signals
     */
    private List<AlertSignal> checkRainAlerts(OpenMeteoForecastResponse forecast, Region region) {
        List<AlertSignal> signals = new ArrayList<>();
        
        List<Double> precipitationSums = forecast.daily().precipitationSum();
        if (precipitationSums == null || precipitationSums.isEmpty()) {
            return signals;
        }
        
        Double maxRain = precipitationSums.stream()
            .filter(rain -> rain != null)
            .max(Double::compareTo)
            .orElse(0.0);
        
        if (maxRain >= RAIN_THRESHOLD_MM) {
            AlertSignal signal = new AlertSignal();
            signal.setSource(AlertSignalSource.WEATHER);
            signal.setKind(AlertSignalKind.RAIN);
            signal.setLevel(AlertSignalLevel.ALERT);
            signal.setScopeType(GeographicScopeType.REGION);
            signal.setScopeId(region.getId());
            signal.setSummary(String.format("Heavy rain warning for %s: %.1f mm", region.getName(), maxRain));
            signal.setDetails(String.format("Precipitation of %.1f mm forecasted, exceeding alert threshold of %.1f mm", 
                    maxRain, RAIN_THRESHOLD_MM));
            signal.setDetectedAt(LocalDateTime.now());
            signal.setValidFrom(LocalDateTime.now());
            signal.setValidTo(LocalDateTime.now().plusDays(3));
            
            AlertSignal saved = alertSignalRepository.save(signal);
            signals.add(saved);
            log.info("Rain alert created for region {}: {} mm", region.getName(), maxRain);
        }
        
        return signals;
    }

    /**
     * Creates an AlertSignal entity from ATMO air quality data.
     * 
     * @param data ATMO air quality response
     * @return created alert signal
     */
    private AlertSignal createAtmoAlertSignal(AtmoAirQualityResponse data) {
        AlertSignal signal = new AlertSignal();
        signal.setSource(AlertSignalSource.ATMO);
        signal.setKind(mapAtmoKind(data));
        signal.setLevel(AlertSignalLevel.ALERT);
        signal.setScopeType(GeographicScopeType.COMMUNE);
        signal.setScopeId(null); // Will need commune ID mapping
        signal.setSummary(String.format("Air quality alert in %s: AQI %d", 
                data.zoneName(), data.atmoIndex()));
        signal.setDetails(String.format("Air quality index: %d (%s), Zone: %s, Date: %s", 
                data.atmoIndex(), data.qualifier(), data.zoneName(), data.measurementDate()));
        signal.setDetectedAt(LocalDateTime.now());
        signal.setValidFrom(LocalDateTime.now());
        signal.setValidTo(LocalDateTime.now().plusDays(1));
        
        return signal;
    }

    /**
     * Maps ATMO data to AlertSignalKind.
     * 
     * @param data ATMO air quality response
     * @return mapped alert signal kind
     */
    private AlertSignalKind mapAtmoKind(AtmoAirQualityResponse data) {
        // Default to AQI, as ATMO provides general air quality index
        return AlertSignalKind.AQI;
    }

    /**
     * Determines if ATMO data represents a pollution episode.
     * 
     * ATMO air quality index scale (1-6):
     * 1 = Bon (Good), 2 = Moyen (Moderate), 3 = Dégradé (Degraded),
     * 4 = Mauvais (Bad), 5 = Très mauvais (Very Bad), 6 = Extrêmement mauvais (Extremely Bad)
     * 
     * @param data ATMO air quality response
     * @return true if episode detected (index >= 4, meaning Bad or worse)
     */
    private boolean isAtmoEpisode(AtmoAirQualityResponse data) {
        // Consider AQI >= 4 (Bad, Very Bad, or Extremely Bad) as episode
        return data.atmoIndex() != null && data.atmoIndex() >= 4;
    }

    /**
     * Gets coordinates for a region (simplified - uses first department's commune).
     * 
     * @param region the region
     * @return array of [longitude, latitude] or null
     */
    private Double[] getRegionCoordinates(Region region) {
        // Simplified: Use central France coordinates as placeholder
        // In production, you would calculate region centroid or use capital city
        return new Double[]{2.3522, 48.8566}; // Paris coordinates as fallback
    }

    /**
     * Creates a manual alert signal from admin input.
     * 
     * @param request manual signal creation request
     * @return created alert signal
     */
    @Transactional
    public AlertSignal createManualSignal(@Valid CreateManualSignalRequest request) {
        log.info("Creating manual alert signal: {}", request.summary());
        
        AlertSignal signal = new AlertSignal();
        signal.setSource(AlertSignalSource.WEATHER); // Manual signals use WEATHER source
        signal.setKind(request.kind());
        signal.setLevel(request.level());
        signal.setScopeType(request.scopeType());
        signal.setScopeId(request.scopeId());
        signal.setSummary(request.summary());
        signal.setDetails(request.details());
        signal.setDetectedAt(LocalDateTime.now());
        signal.setValidFrom(request.validFrom());
        signal.setValidTo(request.validTo());
        
        AlertSignal saved = alertSignalRepository.save(signal);
        log.info("Manual alert signal created with ID: {}", saved.getId());
        
        return saved;
    }

    /**
     * Retrieves all alert signals with pagination.
     * 
     * @param pageable pagination parameters
     * @return page of alert signals
     */
    @Transactional(readOnly = true)
    public Page<AlertSignal> getAllSignals(Pageable pageable) {
        log.debug("Retrieving all alert signals with pagination");
        return alertSignalRepository.findAll(pageable);
    }

    /**
     * Retrieves an alert signal by ID.
     * 
     * @param id signal identifier
     * @return optional containing the signal if found
     */
    @Transactional(readOnly = true)
    public Optional<AlertSignal> getSignalById(Long id) {
        log.debug("Retrieving alert signal with ID: {}", id);
        return alertSignalRepository.findById(id);
    }

    /**
     * Deletes an alert signal (admin cleanup).
     * 
     * @param id signal identifier to delete
     */
    @Transactional
    public void deleteSignal(Long id) {
        log.info("Deleting alert signal with ID: {}", id);
        
        if (!alertSignalRepository.existsById(id)) {
            log.warn("Attempted to delete non-existent signal: {}", id);
            throw new IllegalArgumentException("Alert signal not found with ID: " + id);
        }
        
        alertSignalRepository.deleteById(id);
        log.info("Alert signal deleted successfully: {}", id);
    }
}
