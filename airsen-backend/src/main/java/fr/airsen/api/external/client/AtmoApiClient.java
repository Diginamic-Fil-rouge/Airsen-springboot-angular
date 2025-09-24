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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private final ObjectMapper objectMapper;
    
    private static final String JWT_TOKEN_CACHE_KEY = "atmo:jwt:token";

    @Autowired
    public AtmoApiClient(@Qualifier("atmoWebClient") WebClient webClient,
                        AtmoApiConfig config,
                        @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.webClient = webClient;
        this.config = config;
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Gets JWT token from ATMO API using username/password authentication.
     * 
     * @return Mono containing the JWT token
     */
    private Mono<String> getJwtToken() {
        log.info("Authenticating with ATMO API to get JWT token");
        String loginPayload = "{\"username\": \"" + config.getUsername() + 
                             "\", \"password\": \"" + config.getPassword() + "\"}";
        
        return webClient.post()
            .uri("/api/login")
            .header("Content-Type", "application/json")
            .bodyValue(loginPayload)
            .retrieve()
            .onStatus(HttpStatusCode::isError, response -> 
                Mono.error(new AtmoApiException("JWT Authentication failed: " + response.statusCode())))
            .bodyToMono(String.class)
            .map(this::extractTokenFromResponse)
            .doOnError(error -> log.error("Failed to get JWT token", error))
            .timeout(Duration.ofMillis(config.getTimeoutMs()));
    }
    
    /**
     * Extracts JWT token from login response.
     * 
     * @param responseJson JSON response from login endpoint
     * @return JWT token string
     */
    private String extractTokenFromResponse(String responseJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(responseJson);
            String token = jsonNode.get("token").asText();
            if (token == null || token.isEmpty()) {
                throw new AtmoApiException("No token found in authentication response");
            }
            log.debug("Successfully extracted JWT token");
            return token;
        } catch (Exception e) {
            throw new AtmoApiException("Failed to parse JWT token from response: " + e.getMessage());
        }
    }

    /**
     * Retrieves current air quality indices for all available communes.
     * 
     * @return Flux containing air quality data for all communes
     * @throws AtmoApiException if API call fails
     */
    @Retryable(value = {AtmoApiException.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public Flux<AtmoAirQualityResponse> getCurrentAirQualityIndices() {
        log.info("Fetching current air quality indices from ATMO API with JWT authentication");
        
        return getJwtToken()
            .flatMapMany(token -> webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                    .path("/api/v2/data/indices/atmo")
                    .queryParam("format", "geojson")
                    .queryParam("date", LocalDate.now().minusDays(1).toString())
                    .build())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> 
                    Mono.error(new AtmoApiException("Client error: " + response.statusCode())))
                .onStatus(HttpStatusCode::is5xxServerError, response -> 
                    Mono.error(new AtmoApiException("Server error: " + response.statusCode())))
                .bodyToMono(AtmoGeoJsonResponse.class)
                .flatMapMany(geoJson -> Flux.fromIterable(geoJson.features()))
                .map(feature -> feature.properties())
                .doOnError(error -> log.error("Failed to fetch air quality indices", error)))
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
        log.info("Fetching current air quality data for commune: {}", communeInseeCode);
        
        // Get all indices and filter for specific commune
        return getCurrentAirQualityIndices()
            .filter(response -> communeInseeCode.equals(response.communeInsee()))
            .next()
            .switchIfEmpty(Mono.empty());
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