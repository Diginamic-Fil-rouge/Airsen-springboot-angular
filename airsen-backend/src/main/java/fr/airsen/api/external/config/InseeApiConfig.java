package fr.airsen.api.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for INSEE Geographic API integration.
 * 
 * Handles French administrative data and demographic information.
 */
@Configuration
@ConfigurationProperties(prefix = "external.insee-api")
public class InseeApiConfig {
    
    private String baseUrl = "https://geo.api.gouv.fr";
    private int timeoutMs = 5000;
    private int retryAttempts = 3;

    /**
     * Creates a configured WebClient for INSEE API.
     * 
     * @param builder base WebClient builder
     * @return configured WebClient for INSEE API
     */
    @Bean
    @Qualifier("inseeWebClient")
    public WebClient inseeWebClient(WebClient.Builder builder) {
        return builder
            .baseUrl(getBaseUrl())
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("User-Agent", "Airsen-App/1.0")
            .build();
    }

    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public void setRetryAttempts(int retryAttempts) {
        this.retryAttempts = retryAttempts;
    }
}