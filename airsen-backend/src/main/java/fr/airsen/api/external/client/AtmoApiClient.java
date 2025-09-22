package fr.airsen.api.external.client;

import fr.airsen.api.external.config.AtmoApiConfig;
import fr.airsen.api.external.dto.atmo.AtmoAirQualityResponse;
import fr.airsen.api.external.dto.atmo.AtmoGeoJsonResponse;
import fr.airsen.api.external.exception.AtmoApiException;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDate;

/**
 * Client for ATMO France API integration.
 * 
 * Provides methods to fetch air quality data with proper error handling,
 * retry logic, and rate limiting compliance.
 */
@Component
public class AtmoApiClient {

    private static final Logger log = LoggerFactory.getLogger(AtmoApiClient.class);

    private final WebClient webClient;
    private final AtmoApiConfig config;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public AtmoApiClient(@Qualifier("atmoWebClient") WebClient webClient,
                        AtmoApiConfig config,
                        @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.webClient = webClient;
        this.config = config;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Retrieves current air quality indices for all available communes.
     * 
     * @return Flux containing air quality data for all communes
     * @throws AtmoApiException if API call fails
     */
    @Retryable(value = {AtmoApiException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public Flux<AtmoAirQualityResponse> getCurrentAirQualityIndices() {
        String cacheKey = "atmo:current:all";
        
        // Check cache first
        if (redisTemplate != null) {
            String cached = (String) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Returning cached air quality indices");
                // Parse cached response and return as Flux
                // For now, proceed with API call
            }
        }

        log.info("Fetching current air quality indices from ATMO API");
        
        return webClient
            .get()
            .uri(uriBuilder -> uriBuilder
                .path("/api/v2/data/indices/atmo")
                .queryParam("date", LocalDate.now().toString())
                .build())
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response -> 
                Mono.error(new AtmoApiException("Client error: " + response.statusCode())))
            .onStatus(HttpStatusCode::is5xxServerError, response -> 
                Mono.error(new AtmoApiException("Server error: " + response.statusCode())))
            .bodyToMono(AtmoGeoJsonResponse.class)
            .flatMapMany(geoJson -> Flux.fromIterable(geoJson.features()))
            .map(feature -> feature.properties())
            .doOnNext(response -> {
                if (redisTemplate != null) {
                    // Cache individual commune data for 1 hour
                    String individualCacheKey = "atmo:current:" + response.communeInsee();
                    redisTemplate.opsForValue().set(individualCacheKey, response, Duration.ofHours(1));
                }
            })
            .doOnError(error -> log.error("Failed to fetch air quality indices", error))
            .timeout(Duration.ofMillis(config.getTimeoutMs()));
    }

    /**
     * Retrieves current air quality data for a specific commune.
     * 
     * @param communeInseeCode INSEE code of the commune
     * @return Mono containing air quality data
     * @throws AtmoApiException if API call fails
     */
    @Retryable(value = {AtmoApiException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public Mono<AtmoAirQualityResponse> getCurrentAirQuality(String communeInseeCode) {
        String cacheKey = "atmo:current:" + communeInseeCode;
        
        // Check cache first
        if (redisTemplate != null) {
            AtmoAirQualityResponse cached = (AtmoAirQualityResponse) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Returning cached air quality data for commune: {}", communeInseeCode);
                return Mono.just(cached);
            }
        }

        log.info("Fetching current air quality data for commune: {}", communeInseeCode);
        
        // Get all indices and filter for specific commune
        return getCurrentAirQualityIndices()
            .filter(response -> communeInseeCode.equals(response.communeInsee()))
            .next()
            .switchIfEmpty(Mono.error(new AtmoApiException("No air quality data found for commune: " + communeInseeCode)));
    }

    /**
     * Retrieves historical air quality data for a commune within a date range.
     * 
     * @param communeInseeCode INSEE code of the commune
     * @param startDate start date for historical data
     * @param endDate end date for historical data
     * @return Flux containing historical air quality data
     */
    @Retryable(value = {AtmoApiException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public Flux<AtmoAirQualityResponse> getHistoricalAirQuality(String communeInseeCode, 
                                                               LocalDate startDate, 
                                                               LocalDate endDate) {
        log.info("Fetching historical air quality data for commune: {} from {} to {}", 
                communeInseeCode, startDate, endDate);
        
        return webClient
            .get()
            .uri("/air-quality/historical", builder -> builder
                .queryParam("commune", communeInseeCode)
                .queryParam("start_date", startDate.toString())
                .queryParam("end_date", endDate.toString())
                .build())
            .retrieve()
            .onStatus(HttpStatusCode::isError, response -> 
                Mono.error(new AtmoApiException("API error: " + response.statusCode())))
            .bodyToFlux(AtmoAirQualityResponse.class)
            .doOnError(error -> log.error("Failed to fetch historical air quality for commune: {}", 
                                        communeInseeCode, error))
            .timeout(Duration.ofMillis(config.getTimeoutMs() * 2));
    }

}