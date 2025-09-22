package fr.airsen.api.external.client;

import fr.airsen.api.external.config.InseeApiConfig;
import fr.airsen.api.external.dto.insee.InseeCommuneResponse;
import fr.airsen.api.external.dto.insee.InseeDemographicData;
import fr.airsen.api.external.exception.InseeApiException;
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

/**
 * Client for INSEE Geographic API integration.
 * 
 * Provides French administrative and demographic data
 * for communes, departments, and regions.
 */
@Component
public class InseeApiClient {

    private static final Logger log = LoggerFactory.getLogger(InseeApiClient.class);

    private final WebClient webClient;
    private final InseeApiConfig config;
    private final RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public InseeApiClient(@Qualifier("inseeWebClient") WebClient webClient,
                         InseeApiConfig config,
                         @Autowired(required = false) RedisTemplate<String, Object> redisTemplate) {
        this.webClient = webClient;
        this.config = config;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Retrieves demographic data for a specific commune.
     * 
     * @param inseeCode INSEE code of the commune
     * @return Mono containing commune demographic data
     */
    @Retryable(value = {InseeApiException.class}, maxAttempts = 2, backoff = @Backoff(delay = 500))
    public Mono<InseeCommuneResponse> getCommuneData(String inseeCode) {
        log.info("Fetching INSEE commune data for: {}", inseeCode);
        
        return webClient
            .get()
            .uri("/communes/{code}", inseeCode)
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, response -> 
                Mono.error(new InseeApiException("Commune not found: " + inseeCode)))
            .onStatus(HttpStatusCode::is5xxServerError, response -> 
                Mono.error(new InseeApiException("INSEE API server error: " + response.statusCode())))
            .bodyToMono(InseeCommuneResponse.class)
            .doOnSuccess(response -> log.info("Successfully fetched INSEE data for commune: {}", inseeCode))
            .doOnError(error -> log.error("Failed to fetch INSEE data for commune: {}", inseeCode, error))
            .timeout(Duration.ofMillis(config.getTimeoutMs()));
    }

    /**
     * Retrieves detailed demographic statistics for a commune.
     * 
     * @param inseeCode INSEE code of the commune
     * @return Mono containing demographic statistics
     */
    @Retryable(value = {InseeApiException.class}, maxAttempts = 2, backoff = @Backoff(delay = 500))
    public Mono<InseeDemographicData> getDemographicData(String inseeCode) {
        log.info("Fetching demographic data for commune: {}", inseeCode);
        
        return webClient
            .get()
            .uri("/communes/{code}/donnees-demographiques", inseeCode)
            .retrieve()
            .onStatus(HttpStatusCode::isError, response -> 
                Mono.error(new InseeApiException("Demographic data API error: " + response.statusCode())))
            .bodyToMono(InseeDemographicData.class)
            .doOnSuccess(response -> log.info("Successfully fetched demographic data for commune: {}", inseeCode))
            .doOnError(error -> log.error("Failed to fetch demographic data for commune: {}", 
                                        inseeCode, error))
            .timeout(Duration.ofMillis(config.getTimeoutMs()));
    }

    /**
     * Searches communes by name with partial matching.
     * 
     * Uses the INSEE API structure:
     * https://geo.api.gouv.fr/communes?nom={name}&fields=code,nom,codeDepartement,codeRegion,region,departement,centre,population
     * 
     * @param name partial or complete commune name
     * @param limit maximum number of results to return
     * @return Flux containing matching communes
     */
    public Flux<InseeCommuneResponse> searchCommunesByName(String name, int limit) {
        log.info("Searching communes by name: {} (limit: {}) using INSEE API", name, limit);
        
        return webClient
            .get()
            .uri("/communes", builder -> builder
                .queryParam("nom", name)
                .queryParam("fields", "code,nom,codeDepartement,codeRegion,region,departement,centre,population")
                .queryParam("format", "json")
                .queryParam("boost", "population")
                .build())
            .retrieve()
            .onStatus(HttpStatusCode::isError, response -> 
                Mono.error(new InseeApiException("INSEE API search error: " + response.statusCode())))
            .bodyToFlux(InseeCommuneResponse.class)
            .take(limit)
            .doOnNext(commune -> log.debug("Found commune: {} ({})", commune.name(), commune.inseeCode()))
            .doOnError(error -> log.error("Failed to search communes by name: {}", name, error))
            .timeout(Duration.ofMillis(config.getTimeoutMs()));
    }

    /**
     * Gets coordinates (latitude, longitude) for a commune.
     * 
     * @param inseeCode INSEE code of the commune
     * @return Mono containing coordinates as [longitude, latitude]
     */
    public Mono<Double[]> getCommuneCoordinates(String inseeCode) {
        return getCommuneData(inseeCode)
            .map(commune -> {
                if (commune.centre() != null && commune.centre().getLatitude() != null 
                    && commune.centre().getLongitude() != null) {
                    return new Double[]{commune.centre().getLongitude(), commune.centre().getLatitude()};
                }
                throw new InseeApiException("No coordinates available for commune: " + inseeCode);
            })
            .doOnSuccess(coords -> log.debug("Retrieved coordinates for commune {}: [{}, {}]", 
                                           inseeCode, coords[0], coords[1]));
    }

    /**
     * Gets major French communes with population over a threshold.
     * 
     * @param populationThreshold minimum population
     * @param limit maximum number of results
     * @return Flux containing major communes with complete data
     */
    public Flux<InseeCommuneResponse> getMajorCommunes(int populationThreshold, int limit) {
        log.info("Fetching major communes with population > {} (limit: {})", populationThreshold, limit);
        
        return webClient
            .get()
            .uri("/communes", builder -> builder
                .queryParam("fields", "code,nom,population,centre,codeDepartement,departement,codeRegion,region")
                .queryParam("format", "json")
                .queryParam("boost", "population")
                .build())
            .retrieve()
            .onStatus(HttpStatusCode::isError, response -> 
                Mono.error(new InseeApiException("INSEE API error: " + response.statusCode())))
            .bodyToFlux(InseeCommuneResponse.class)
            .filter(commune -> commune.population() != null && commune.population() >= populationThreshold)
            .take(limit)
            .doOnNext(commune -> log.debug("Found major commune: {} ({}) - pop: {}", 
                                          commune.name(), commune.inseeCode(), commune.population()))
            .doOnError(error -> log.error("Failed to fetch major communes", error))
            .timeout(Duration.ofMillis(config.getTimeoutMs() * 2));
    }
}