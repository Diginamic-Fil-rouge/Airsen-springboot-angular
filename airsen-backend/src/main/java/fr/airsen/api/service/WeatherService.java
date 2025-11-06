package fr.airsen.api.service;

import fr.airsen.api.dto.response.NearestWeatherResult;
import fr.airsen.api.dto.response.WeatherResponse;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.WeatherData;
import fr.airsen.api.entity.cacheData.CacheMetadata;
import fr.airsen.api.event.WeatherDataUpdatedEvent;
import fr.airsen.api.external.client.InseeApiClient;
import fr.airsen.api.external.client.OpenMeteoApiClient;
import fr.airsen.api.external.dto.openmeteo.OpenMeteoCurrentResponse;
import fr.airsen.api.external.dto.openmeteo.OpenMeteoForecastResponse;
import fr.airsen.api.mapper.WeatherMapper;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.WeatherDataRepository;
import fr.airsen.api.service.cacheData.SmartCacheService;
import fr.airsen.api.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


@Service
@Transactional
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final OpenMeteoApiClient openMeteoApiClient;
    private final InseeApiClient inseeApiClient;
    private final WeatherDataRepository weatherDataRepository;
    private final CommuneRepository communeRepository;
    private final GeoDistanceService geoDistanceService;
    private final SmartCacheService smartCacheService;
    private final WeatherMapper weatherMapper;
    private final ApplicationEventPublisher eventPublisher;

    public WeatherService(OpenMeteoApiClient openMeteoApiClient,
                         InseeApiClient inseeApiClient,
                         WeatherDataRepository weatherDataRepository,
                         CommuneRepository communeRepository,
                         GeoDistanceService geoDistanceService,
                         SmartCacheService smartCacheService,
                         WeatherMapper weatherMapper,
                         ApplicationEventPublisher eventPublisher) {
        this.openMeteoApiClient = openMeteoApiClient;
        this.inseeApiClient = inseeApiClient;
        this.weatherDataRepository = weatherDataRepository;
        this.communeRepository = communeRepository;
        this.geoDistanceService = geoDistanceService;
        this.smartCacheService = smartCacheService;
        this.weatherMapper = weatherMapper;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Updates weather data for all tracked communes.
     *
     * Runs daily at 10:30 AM (configurable via scheduling.weather.cron property).
     * Default: 10:30 Europe/Paris timezone (staggered 30 minutes after ATMO sync, testing phase).
     *
     * Configuration:
     * - scheduling.weather.cron: Cron expression (default: 0 30 10 * * *)
     * - scheduling.weather.timezone: Timezone (default: Europe/Paris)
     *
     * Transaction-Aware Cache Eviction:
     * After successful database updates, publishes WeatherDataUpdatedEvent.
     * The CacheEvictionListener will evict related caches ONLY after transaction commits.
     * If update fails, caches remain intact to preserve stale data.
     */
    @Scheduled(cron = "${scheduling.weather.cron}", zone = "${scheduling.weather.timezone}")
    public void updateAllWeatherData() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("========================================");
        log.info("SCHEDULED WEATHER SYNC STARTED at {}", startTime);
        log.info("========================================");

        try {
            int communesUpdated = updateWeatherDataInTransaction();

            LocalDateTime endTime = LocalDateTime.now();
            java.time.Duration duration = java.time.Duration.between(startTime, endTime);
            log.info("========================================");
            log.info("SCHEDULED WEATHER SYNC COMPLETED SUCCESSFULLY at {}", endTime);
            log.info("Updated {} communes in {} seconds", communesUpdated, duration.getSeconds());
            log.info("WeatherDataUpdatedEvent published - caches will be evicted after transaction commit");
            log.info("========================================");

        } catch (Exception e) {
            LocalDateTime endTime = LocalDateTime.now();
            java.time.Duration duration = java.time.Duration.between(startTime, endTime);
            log.error("========================================");
            log.error("SCHEDULED WEATHER SYNC FAILED at {}", endTime);
            log.error("Duration before failure: {} seconds", duration.getSeconds());
            log.error("Error: {}", e.getMessage(), e);
            log.error("Caches NOT cleared to preserve stale data");
            log.error("========================================");
            // Don't rethrow - allow scheduler to continue on next run
        }
    }

    /**
     * Transactional method to update weather data and publish event.
     *
     * This method ensures that:
     * 1. All database updates happen within a single transaction
     * 2. WeatherDataUpdatedEvent is published INSIDE the transaction
     * 3. CacheEvictionListener will only evict caches AFTER transaction commits
     * 4. If transaction fails, event is never published and caches remain intact
     *
     * @return number of communes successfully updated
     */
    @Transactional
    protected int updateWeatherDataInTransaction() {
        List<Commune> communes = communeRepository.findAll();
        log.info("Found {} communes to update weather data", communes.size());

        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.concurrent.atomic.AtomicInteger failureCount = new java.util.concurrent.atomic.AtomicInteger(0);

        Flux.fromIterable(communes)
            .flatMap(commune ->
                updateWeatherForCommune(commune.getInseeCode())
                    .doOnSuccess(updated -> {
                        successCount.incrementAndGet();
                        log.debug("Updated weather for commune: {}", commune.getInseeCode());
                    })
                    .onErrorContinue((error, commune_obj) -> {
                        failureCount.incrementAndGet();
                        log.warn("Failed to update weather for commune: {} - {}",
                                commune.getInseeCode(), error.getMessage());
                    }))
            .blockLast(); // Block to ensure all updates complete within transaction

        int updated = successCount.get();
        int failed = failureCount.get();

        log.info("Weather data update completed: {} successful, {} failed", updated, failed);

        // Publish event INSIDE transaction - will only fire AFTER commit
        eventPublisher.publishEvent(
            new WeatherDataUpdatedEvent(this, updated, "SCHEDULED_DAILY")
        );

        log.debug("WeatherDataUpdatedEvent published with {} communes updated", updated);

        return updated;
    }

    /**
     * Updates current weather data for a specific commune.
     *
     * First gets coordinates from INSEE API, then fetches weather from Open-Meteo.
     *
     * @param communeInseeCode INSEE code of the commune
     * @return Mono<WeatherData> containing the updated data
     */
    public Mono<WeatherData> updateWeatherForCommune(String communeInseeCode) {
        log.info("Updating weather data for commune: {}", communeInseeCode);

        // Get coordinates from database instead of INSEE API
        return Mono.fromCallable(() -> {
                Commune commune = communeRepository.findByInseeCode(communeInseeCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Commune not found: " + communeInseeCode));

                if (commune.getLatitude() == null || commune.getLongitude() == null) {
                    throw new IllegalStateException("Commune " + communeInseeCode + " has no coordinates in database");
                }

                // Return coordinates as [longitude, latitude] for Open-Meteo API
                return new Double[]{commune.getLongitude().doubleValue(), commune.getLatitude().doubleValue()};
            })
            .flatMap(coordinates -> {
                log.debug("Retrieved coordinates for commune {}: [{}, {}]",
                         communeInseeCode, coordinates[0], coordinates[1]);

                return openMeteoApiClient.getCurrentWeatherByCoordinates(coordinates);
            })
            .map(weatherResponse -> mapToEntity(weatherResponse, communeInseeCode))
            .flatMap(weatherData -> {
                WeatherData saved = weatherDataRepository.save(weatherData);

                log.info("Successfully updated weather data for commune: {} - Temperature: {}°C",
                        communeInseeCode, saved.getTemperature());

                return Mono.just(saved);
            });
    }

    /**
     * Gets current weather response for a commune with smart caching and geodistance fallback.
     *
     * Cache-Aware Data Retrieval Strategy:
     * 1. Check SmartCache first (cache key: "weather:" + inseeCode)
     * 2. If cache miss or stale, execute fetchWeatherWithFallback()
     * 3. Cache result with 6-hour TTL (ON_DEMAND_FETCH)
     *
     * Fallback Strategy (preserved as-is):
     * 1. Query database for recent data from requested commune
     * 2. If no direct data exists, attempt geodistance fallback within 20km
     * 3. Return WeatherResponse with proper DataSource metadata (DIRECT/ESTIMATED)
     *
     * @param communeInseeCode INSEE code of the commune
     * @return WeatherResponse containing weather data with source transparency
     * @throws ResourceNotFoundException if no weather data within 20km threshold
     */
    @Transactional(readOnly = true)
    public WeatherResponse getCurrentWeatherForCommune(String communeInseeCode) {
        log.debug("Fetching weather for commune: {}", communeInseeCode);

        String cacheKey = "weather:" + communeInseeCode;

        return smartCacheService.getOrFetch(
            cacheKey,
            WeatherResponse.class,
            CacheMetadata.DataSource.ON_DEMAND_FETCH,
            false, // forceRefresh
            () -> fetchWeatherWithFallback(communeInseeCode)
        ).getData();
    }

    /**
     * Fetches weather data with geodistance fallback logic (extracted from cache wrapper).
     *
     * This method preserves the existing 20km Haversine fallback exactly as-is.
     * Called by SmartCacheService when cache miss occurs or refresh is needed.
     *
     * @param communeInseeCode INSEE code of the commune
     * @return WeatherResponse with proper DataSource metadata
     * @throws ResourceNotFoundException if no weather data within 20km threshold
     */
    private WeatherResponse fetchWeatherWithFallback(String communeInseeCode) {
        log.debug("Cache miss or refresh needed, fetching weather from database");

        // Try direct database query
        Optional<WeatherData> directData = weatherDataRepository
            .getMostRecentWeatherByInseeCode(communeInseeCode);

        if (directData.isPresent()) {
            log.debug("Found direct weather data for commune: {}", communeInseeCode);
            return weatherMapper.toDirectResponse(directData.get());
        }

        // Geodistance fallback (20km threshold) - PRESERVED AS-IS
        log.debug("No direct data, attempting geodistance fallback within 20km");
        Optional<NearestWeatherResult> nearestData = geoDistanceService
            .findNearestCommuneWithWeather(communeInseeCode, 20.0);

        if (nearestData.isPresent()) {
            log.info("Using estimated weather from {} ({} km away)",
                     nearestData.get().communeName(),
                     nearestData.get().distanceKm());
            return weatherMapper.toEstimatedResponse(nearestData.get());
        }

        throw new ResourceNotFoundException(
            "No weather data within 20km for commune: " + communeInseeCode
        );
    }

    /**
     * Evicts weather cache for specific commune using SmartCacheService.
     *
     * @param communeInseeCode INSEE code of the commune
     */
    public void evictWeatherCache(String communeInseeCode) {
        String cacheKey = "weather:" + communeInseeCode;
        smartCacheService.invalidate(cacheKey);
        log.info("Evicted weather cache for INSEE code: {}", communeInseeCode);
    }

    /**
     * Clears all weather cache using SmartCacheService.
     */
    public void evictAllWeatherCache() {
        smartCacheService.clearAll();
        log.info("Cleared all weather cache");
    }

    /**
     * Gets weather forecast for a commune.
     *
     * @param communeInseeCode INSEE code of the commune
     * @param forecastDays number of forecast days (1-16)
     * @return Flux<WeatherData> containing forecast data as WeatherData entities
     */
    public Flux<WeatherData> getWeatherForecastForCommune(String communeInseeCode, int forecastDays) {
        log.info("Fetching weather forecast for commune: {} for {} days", communeInseeCode, forecastDays);

        return getCommuneCoordinatesWithFallback(communeInseeCode)
            .flatMapMany(coordinates -> {
                log.debug("Using coordinates for forecast: [{}, {}]", coordinates[0], coordinates[1]);

                return openMeteoApiClient.getForecastByCoordinates(coordinates, forecastDays)
                    .flatMapMany(forecast -> {
                        // Convert forecast response to WeatherData entities
                        if (forecast.daily() == null || forecast.daily().dates() == null || forecast.daily().dates().isEmpty()) {
                            log.warn("No forecast data available for commune: {}", communeInseeCode);
                            return Flux.empty();
                        }

                        var daily = forecast.daily();
                        int dataSize = daily.dates().size();

                        return Flux.range(0, dataSize)
                            .map(i -> {
                                Commune commune = communeRepository.findByInseeCode(communeInseeCode)
                                    .orElseThrow(() -> new ResourceNotFoundException("Commune not found: " + communeInseeCode));

                                WeatherData weatherData = new WeatherData();
                                weatherData.setCommune(commune);
                                weatherData.setMeasurementDate(daily.dates().get(i));

                                // Set temperature (average of min and max)
                                Double tempMax = daily.maxTemperatures() != null && i < daily.maxTemperatures().size()
                                    ? daily.maxTemperatures().get(i) : null;
                                Double tempMin = daily.minTemperatures() != null && i < daily.minTemperatures().size()
                                    ? daily.minTemperatures().get(i) : null;

                                if (tempMax != null && tempMin != null) {
                                    weatherData.setTemperature((tempMax + tempMin) / 2);
                                    weatherData.setMaxTemperature(tempMax);
                                    weatherData.setMinTemperature(tempMin);
                                }

                                // Set wind speed
                                if (daily.maxWindSpeed() != null && i < daily.maxWindSpeed().size()) {
                                    weatherData.setWindSpeed(daily.maxWindSpeed().get(i));
                                }

                                // Set weather code
                                if (daily.weatherCodes() != null && i < daily.weatherCodes().size()) {
                                    weatherData.setWeatherCode(daily.weatherCodes().get(i));
                                }

                                // Set precipitation data if available in daily forecast
                                if (daily.precipitationSum() != null && i < daily.precipitationSum().size()) {
                                    weatherData.setPrecipitation(daily.precipitationSum().get(i));
                                }

                                // Note: Daily forecast has limited weather data compared to current weather
                                // These fields are not available in daily forecast and remain default/null:
                                // - humidity and wind direction (not provided in daily data)
                                weatherData.setHumidity(0);
                                weatherData.setWindDirection(0);
                                
                                // - Advanced weather fields (not available in daily forecast)
                                // apparentTemperature, rain, showers, snowfall, cloudCover, windGusts, pressureMsl
                                // These will remain null for forecast data (acceptable behavior)

                                return weatherData;
                            });
                    });
            })
            .doOnComplete(() -> log.info("Retrieved {} day forecast for commune: {}",
                                       forecastDays, communeInseeCode));
    }

    /**
     * Gets weather forecast for a commune (returns raw API response).
     *
     * @param communeInseeCode INSEE code of the commune
     * @param forecastDays number of forecast days (1-16)
     * @return Mono<OpenMeteoForecastResponse> containing forecast data
     */
    public Mono<OpenMeteoForecastResponse> getWeatherForecast(String communeInseeCode, int forecastDays) {
        log.info("Fetching weather forecast for commune: {} for {} days", communeInseeCode, forecastDays);

        return getCommuneCoordinatesWithFallback(communeInseeCode)
            .flatMap(coordinates -> {
                log.debug("Using coordinates for forecast: [{}, {}]", coordinates[0], coordinates[1]);

                return openMeteoApiClient.getForecastByCoordinates(coordinates, forecastDays);
            })
            .doOnSuccess(forecast -> log.info("Retrieved {} day forecast for commune: {}",
                                            forecastDays, communeInseeCode));
    }

    /**
     * Gets historical weather data for a commune.
     *
     * @param communeInseeCode INSEE code of the commune
     * @param startDate start date for historical data
     * @param endDate end date for historical data
     * @return Flux<WeatherData> containing historical weather data
     */
    public Flux<WeatherData> getHistoricalWeather(String communeInseeCode,
                                                 LocalDate startDate,
                                                 LocalDate endDate) {
        log.info("Fetching historical weather data for commune: {} from {} to {}",
                communeInseeCode, startDate, endDate);

        // For historical data, we would typically query our database
        // or make additional API calls if the external API supports historical data
        return Flux.fromIterable(
            weatherDataRepository.findByCommune_InseeCodeAndMeasurementDateBetween(
                communeInseeCode,
                startDate.atStartOfDay(),
                endDate.atTime(23, 59, 59)
            )
        );
    }

    /**
     * Forces update of weather data for a commune (bypasses cache).
     *
     * @param communeInseeCode INSEE code of the commune
     * @return Mono<WeatherData> containing the updated data
     */
    public Mono<WeatherData> forceUpdateWeatherForCommune(String communeInseeCode) {
        log.info("Force updating weather data for commune: {}", communeInseeCode);
        return updateWeatherForCommune(communeInseeCode);
    }

    /**
     * Forces update of weather data for a commune (alias for backward compatibility).
     *
     * @param communeInseeCode INSEE code of the commune
     * @return Mono<WeatherData> containing the updated data
     */
    public Mono<WeatherData> forceUpdateWeather(String communeInseeCode) {
        return forceUpdateWeatherForCommune(communeInseeCode);
    }


    /**
     * Gets coordinates for a commune with fallback mechanism.
     *
     * First tries to get coordinates from database (primary source).
     * If database coordinates are missing, falls back to INSEE API.
     *
     * @param communeInseeCode INSEE code of the commune
     * @return Mono containing coordinates as [longitude, latitude]
     */
    private Mono<Double[]> getCommuneCoordinatesWithFallback(String communeInseeCode) {
        return Mono.fromCallable(() -> {
                Commune commune = communeRepository.findByInseeCode(communeInseeCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Commune not found: " + communeInseeCode));

                // Check if database has coordinates
                if (commune.getLatitude() != null && commune.getLongitude() != null) {
                    log.debug("Using database coordinates for commune {}: [{}, {}]",
                             communeInseeCode, commune.getLongitude(), commune.getLatitude());
                    return new Double[]{commune.getLongitude().doubleValue(), commune.getLatitude().doubleValue()};
                }

                // No coordinates in database, need to fetch from INSEE API
                log.warn("No coordinates in database for commune {}, falling back to INSEE API", communeInseeCode);
                return null;
            })
            .flatMap(coordinates -> {
                if (coordinates != null) {
                    return Mono.just(coordinates);
                }

                // Fallback to INSEE API
                log.info("Fetching coordinates from INSEE API for commune: {}", communeInseeCode);
                return inseeApiClient.getCommuneCoordinates(communeInseeCode)
                    .doOnSuccess(coords -> log.debug("Retrieved coordinates from INSEE API for commune {}: [{}, {}]",
                                                   communeInseeCode, coords[0], coords[1]))
                    .doOnError(error -> log.error("Failed to get coordinates for commune {} from INSEE API",
                                                communeInseeCode, error));
            });
    }

    /**
     * Maps Open-Meteo API response to WeatherData entity.
     *
     * @param response Open-Meteo API response
     * @param communeInseeCode INSEE code of the commune
     * @return WeatherData entity
     */
    private WeatherData mapToEntity(OpenMeteoCurrentResponse response, String communeInseeCode) {
        Commune commune = communeRepository.findByInseeCode(communeInseeCode)
            .orElseThrow(() -> new ResourceNotFoundException("Commune not found: " + communeInseeCode));

        WeatherData weatherData = new WeatherData();
        weatherData.setCommune(commune);
        weatherData.setMeasurementDate(LocalDate.now());
        weatherData.setCreatedAt(LocalDate.now());

        if (response.current() != null) {
            // Basic weather measurements
            weatherData.setTemperature(response.current().temperature() != null ? response.current().temperature() : 0.0);
            weatherData.setHumidity(response.current().humidity() != null ? response.current().humidity() : 0);
            weatherData.setWindSpeed(response.current().windSpeed() != null ? response.current().windSpeed() : 0.0);
            weatherData.setWindDirection(response.current().windDirection() != null ? response.current().windDirection() : 0);
            weatherData.setWeatherCode(response.current().weatherCode() != null ? response.current().weatherCode() : 0);

            // Advanced weather measurements
            weatherData.setApparentTemperature(response.current().apparentTemperature());
            weatherData.setPrecipitation(response.current().precipitation());
            weatherData.setRain(response.current().rain());
            weatherData.setShowers(response.current().showers());
            weatherData.setSnowfall(response.current().snowfall());
            weatherData.setCloudCover(response.current().cloudCover());
            weatherData.setWindGusts(response.current().windGusts());
            weatherData.setPressureMsl(response.current().pressureMsl());
        }

        return weatherData;
    }

    /**
     * Creates a WeatherData entity for forecast data.
     *
     * @param communeInseeCode INSEE code of the commune
     * @param date forecast date
     * @param tempMax maximum temperature
     * @param tempMin minimum temperature
     * @return WeatherData entity for forecast
     */
    private WeatherData createForecastWeatherData(String communeInseeCode,
                                                 java.time.LocalDate date,
                                                 Double tempMax,
                                                 Double tempMin) {
        Commune commune = communeRepository.findByInseeCode(communeInseeCode)
            .orElseThrow(() -> new ResourceNotFoundException("Commune not found: " + communeInseeCode));

        WeatherData weatherData = new WeatherData();
        weatherData.setCommune(commune);
        weatherData.setMeasurementDate(date);
        weatherData.setTemperature((tempMax + tempMin) / 2); // Average temperature
        weatherData.setMaxTemperature(tempMax);
        weatherData.setMinTemperature(tempMin);

        return weatherData;
    }

    /**
     * Converts NearestWeatherResult to WeatherData entity.
     *
     * Used when returning estimated weather data from nearest commune.
     * The returned entity represents data from the nearest commune,
     * not the originally requested commune.
     *
     * @param requestedCommuneInseeCode INSEE code of requested commune
     * @param nearestResult Weather result from nearest commune
     * @return WeatherData entity with estimated data
     */
    private WeatherData convertNearestResultToWeatherData(String requestedCommuneInseeCode,
                                                          NearestWeatherResult nearestResult) {
        Commune estimatedCommune = communeRepository.findByInseeCode(requestedCommuneInseeCode)
            .orElseThrow(() -> new ResourceNotFoundException("Commune not found: " + requestedCommuneInseeCode));

        WeatherData weatherData = new WeatherData();
        weatherData.setCommune(estimatedCommune);
        weatherData.setMeasurementDate(nearestResult.measurementDate());
        weatherData.setCreatedAt(LocalDate.now());

        // Basic weather measurements with null-safe handling
        weatherData.setTemperature(nearestResult.temperature() != null ? nearestResult.temperature() : 0.0);
        weatherData.setHumidity(nearestResult.humidity() != null ? nearestResult.humidity() : 0);
        weatherData.setWindSpeed(nearestResult.windSpeed() != null ? nearestResult.windSpeed() : 0.0);
        weatherData.setWindDirection(nearestResult.windDirection() != null ? nearestResult.windDirection() : 0);
        weatherData.setWeatherCode(nearestResult.weatherCode());

        // Advanced weather measurements (null-safe - these can be null)
        weatherData.setApparentTemperature(nearestResult.apparentTemperature());
        weatherData.setPrecipitation(nearestResult.precipitation());
        weatherData.setRain(nearestResult.rain());
        weatherData.setShowers(nearestResult.showers());
        weatherData.setSnowfall(nearestResult.snowfall());
        weatherData.setCloudCover(nearestResult.cloudCover());
        weatherData.setWindGusts(nearestResult.windGusts());
        weatherData.setPressureMsl(nearestResult.pressureMsl());

        return weatherData;
    }

}
