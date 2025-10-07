package fr.airsen.api.service;

import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.WeatherData;
import fr.airsen.api.external.client.InseeApiClient;
import fr.airsen.api.external.client.OpenMeteoApiClient;
import fr.airsen.api.external.dto.openmeteo.OpenMeteoCurrentResponse;
import fr.airsen.api.external.dto.openmeteo.OpenMeteoForecastResponse;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.WeatherDataRepository;
import fr.airsen.api.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for integrating weather data from Open-Meteo API.
 * 
 * Uses INSEE API to get commune coordinates and Open-Meteo API for weather data.
 * Handles data transformation, validation, and persistence for weather measurements.
 */
@Service
@Transactional
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private final OpenMeteoApiClient openMeteoApiClient;
    private final InseeApiClient inseeApiClient;
    private final WeatherDataRepository weatherDataRepository;
    private final CommuneRepository communeRepository;

    public WeatherService(OpenMeteoApiClient openMeteoApiClient,
                         InseeApiClient inseeApiClient,
                         WeatherDataRepository weatherDataRepository,
                         CommuneRepository communeRepository) {
        this.openMeteoApiClient = openMeteoApiClient;
        this.inseeApiClient = inseeApiClient;
        this.weatherDataRepository = weatherDataRepository;
        this.communeRepository = communeRepository;
    }

    /**
     * Updates weather data for all tracked communes.
     * 
     * This method is called by scheduled tasks to keep weather data current.
     * Uses INSEE API to get coordinates, then fetches weather from Open-Meteo.
     * 
     * @return Mono<Void> indicating completion
     */
    @Scheduled(fixedRate = 1800000) // Every 30 minutes
    public Mono<Void> updateAllWeatherData() {
        log.info("Starting scheduled weather data update");
        
        List<Commune> communes = communeRepository.findAll();
        log.info("Found {} communes to update weather data", communes.size());
        
        return Flux.fromIterable(communes)
            .flatMap(commune -> 
                updateWeatherForCommune(commune.getInseeCode())
                    .onErrorContinue((error, commune_obj) -> 
                        log.warn("Failed to update weather for commune: {}", 
                               commune.getInseeCode(), error)))
            .then()
            .doOnSuccess(v -> log.info("Completed scheduled weather data update"))
            .doOnError(error -> log.error("Failed to complete weather data update", error));
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
     * Gets current weather data for a commune.
     * 
     * @param communeInseeCode INSEE code of the commune
     * @return Mono<WeatherData> containing current weather data
     */
    public Mono<WeatherData> getCurrentWeatherForCommune(String communeInseeCode) {
        // Try to get recent data from database first
        Optional<WeatherData> existingOpt = weatherDataRepository.getMostRecentWeatherByInseeCode(communeInseeCode);
        
        if (existingOpt.isPresent()) {
            WeatherData existing = existingOpt.get();
            if (existing.getMeasurementDate().isAfter(LocalDate.now().minusDays(1))) {
                log.debug("Returning recent weather data from database for commune: {}", communeInseeCode);
                return Mono.just(existing);
            }
        }
        
        // Fetch fresh data if no recent data exists
        log.info("Fetching fresh weather data for commune: {}", communeInseeCode);
        return updateWeatherForCommune(communeInseeCode);
    }

    /**
     * Gets current weather data for a commune (alias for backward compatibility).
     * 
     * @param communeInseeCode INSEE code of the commune
     * @return Mono<WeatherData> containing current weather data
     */
    public Mono<WeatherData> getCurrentWeather(String communeInseeCode) {
        return getCurrentWeatherForCommune(communeInseeCode);
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

                                // Note: humidity and wind direction are not in daily forecast data
                                weatherData.setHumidity(0.0);
                                weatherData.setWindDirection(0.0);

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
     * Gets weather data for multiple communes in a region.
     * 
     * @param regionCode INSEE region code
     * @return Flux<WeatherData> containing weather data for all communes in the region
     */
    public Flux<WeatherData> getRegionWeatherData(String regionCode) {
        log.info("Fetching weather data for all communes in region: {}", regionCode);
        
        List<Commune> communes = communeRepository.findByRegionCode(regionCode);
        
        return Flux.fromIterable(communes)
            .flatMap(commune -> getCurrentWeather(commune.getInseeCode()))
            .doOnComplete(() -> log.info("Completed weather data fetch for region: {}", regionCode));
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
            weatherData.setTemperature(response.current().temperature() != null ? response.current().temperature() : 0.0);
            weatherData.setHumidity(response.current().humidity() != null ? response.current().humidity().doubleValue() : 0.0);
            weatherData.setWindSpeed(response.current().windSpeed() != null ? response.current().windSpeed() : 0.0);
            weatherData.setWindDirection(response.current().windDirection() != null ? response.current().windDirection().doubleValue() : 0.0);
            weatherData.setWeatherCode(response.current().weatherCode() != null ? response.current().weatherCode() : 0);
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
}