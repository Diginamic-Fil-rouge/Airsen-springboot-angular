package fr.airsen.api.external.client;

import fr.airsen.api.external.config.OpenMeteoApiConfig;
import fr.airsen.api.external.dto.openmeteo.OpenMeteoCurrentResponse;
import fr.airsen.api.external.dto.openmeteo.OpenMeteoForecastResponse;
import fr.airsen.api.external.exception.WeatherApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatusCode;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Client for Open-Meteo Weather API integration.
 * 
 * Provides weather data including current conditions and forecasts
 * for geographic coordinates obtained from INSEE API.
 */
@Component
public class OpenMeteoApiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenMeteoApiClient.class);

    private final WebClient webClient;
    private final OpenMeteoApiConfig config;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public OpenMeteoApiClient(@Qualifier("openMeteoWebClient") WebClient webClient,
                             OpenMeteoApiConfig config,
                             @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.webClient = webClient;
        this.config = config;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Retrieves current weather conditions for specific coordinates.
     * 
     * Coordinates should be obtained from INSEE API for the commune.
     * 
     * @param latitude geographic latitude
     * @param longitude geographic longitude
     * @return Mono containing current weather data
     */
    @Retryable(value = {WeatherApiException.class}, maxAttempts = 3 , backoff = @Backoff(delay = 500))
    public Mono<OpenMeteoCurrentResponse> getCurrentWeather(Double latitude, Double longitude) {
        String cacheKey = String.format("weather:current:%.2f:%.2f", latitude, longitude);
        
        // Temporarily disable Redis caching to avoid deserialization issues
        // TODO: Fix Redis serialization for Record classes
        /*
        if (redisTemplate != null) {
            OpenMeteoCurrentResponse cached = (OpenMeteoCurrentResponse) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Returning cached weather data for coordinates: [{}, {}]", longitude, latitude);
                return Mono.just(cached);
            }
        }
        */

        log.info("Fetching current weather for coordinates: [{}, {}]", longitude, latitude);
        
        return webClient
            .get()
            .uri("/forecast", builder -> builder
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("current", "temperature_2m,relative_humidity_2m,wind_speed_10m,wind_direction_10m,weather_code,precipitation,pressure_msl,visibility")
                .queryParam("timezone", "Europe/Paris")
                .queryParam("forecast_days", 1)
                .build())
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response -> 
                Mono.error(new WeatherApiException("Invalid coordinates or parameters: " + response.statusCode())))
            .onStatus(HttpStatusCode::is5xxServerError, response -> 
                Mono.error(new WeatherApiException("Weather service error: " + response.statusCode())))
            .bodyToMono(OpenMeteoCurrentResponse.class)
            .doOnSuccess(response -> {
                // Temporarily disable Redis caching to avoid deserialization issues
                // TODO: Fix Redis serialization for Record classes
                /*
                if (redisTemplate != null) {
                    // Cache for 30 minutes
                    redisTemplate.opsForValue().set(cacheKey, response, Duration.ofMinutes(30));
                    log.info("Cached weather data for coordinates: [{}, {}]", longitude, latitude);
                } else {
                    log.debug("Redis not available - skipping cache for weather data: [{}, {}]", longitude, latitude);
                }
                */
                log.debug("Weather data fetched successfully for coordinates: [{}, {}]", longitude, latitude);
            })
            .doOnError(error -> log.error("Failed to fetch weather for coordinates: [{}, {}]", 
                                        longitude, latitude, error))
            .timeout(Duration.ofMillis(config.getTimeoutMs()));
    }

    /**
     * Retrieves weather forecast for specific coordinates.
     * 
     * @param latitude geographic latitude
     * @param longitude geographic longitude
     * @param forecastDays number of forecast days (1-16)
     * @return Mono containing forecast data
     */
    @Retryable(value = {WeatherApiException.class}, maxAttempts = 2, backoff = @Backoff(delay = 500))
    public Mono<OpenMeteoForecastResponse> getWeatherForecast(Double latitude, Double longitude, int forecastDays) {
        String cacheKey = String.format("weather:forecast:%.2f:%.2f:%d", latitude, longitude, forecastDays);
        
        // Temporarily disable Redis caching to avoid deserialization issues
        // TODO: Fix Redis serialization for Record classes
        /*
        if (redisTemplate != null) {
            OpenMeteoForecastResponse cached = (OpenMeteoForecastResponse) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Returning cached forecast data for coordinates: [{}, {}]", longitude, latitude);
                return Mono.just(cached);
            }
        }
        */

        log.info("Fetching weather forecast for coordinates: [{}, {}] for {} days", 
                longitude, latitude, forecastDays);
        
        return webClient
            .get()
            .uri("/forecast", builder -> builder
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_sum,wind_speed_10m_max,weather_code")
                .queryParam("timezone", "Europe/Paris")
                .queryParam("forecast_days", Math.min(forecastDays, 16))
                .build())
            .retrieve()
            .onStatus(HttpStatusCode::isError, response -> 
                Mono.error(new WeatherApiException("Forecast API error: " + response.statusCode())))
            .bodyToMono(OpenMeteoForecastResponse.class)
            .doOnSuccess(response -> {
                // Temporarily disable Redis caching to avoid deserialization issues
                // TODO: Fix Redis serialization for Record classes
                /*
                if (redisTemplate != null) {
                    // Cache for 2 hours
                    redisTemplate.opsForValue().set(cacheKey, response, Duration.ofHours(2));
                    log.info("Cached forecast data for coordinates: [{}, {}]", longitude, latitude);
                } else {
                    log.debug("Redis not available - skipping cache for forecast data: [{}, {}]", longitude, latitude);
                }
                */
                log.debug("Forecast data fetched successfully for coordinates: [{}, {}]", longitude, latitude);
            })
            .doOnError(error -> log.error("Failed to fetch forecast for coordinates: [{}, {}]", 
                                        longitude, latitude, error))
            .timeout(Duration.ofMillis(config.getTimeoutMs()));
    }

    /**
     * Gets current weather for a commune using coordinates.
     * 
     * This is a convenience method that takes coordinates as an array.
     * 
     * @param coordinates array containing [longitude, latitude]
     * @return Mono containing current weather data
     */
    public Mono<OpenMeteoCurrentResponse> getCurrentWeatherByCoordinates(Double[] coordinates) {
        if (coordinates == null || coordinates.length < 2) {
            return Mono.error(new WeatherApiException("Invalid coordinates array"));
        }
        
        return getCurrentWeather(coordinates[1], coordinates[0]); // [longitude, latitude] -> lat, lon
    }

    /**
     * Gets weather forecast for a commune using coordinates.
     * 
     * @param coordinates array containing [longitude, latitude]
     * @param forecastDays number of forecast days
     * @return Mono containing forecast data
     */
    public Mono<OpenMeteoForecastResponse> getForecastByCoordinates(Double[] coordinates, int forecastDays) {
        if (coordinates == null || coordinates.length < 2) {
            return Mono.error(new WeatherApiException("Invalid coordinates array"));
        }
        
        return getWeatherForecast(coordinates[1], coordinates[0], forecastDays); // [longitude, latitude] -> lat, lon
    }
}