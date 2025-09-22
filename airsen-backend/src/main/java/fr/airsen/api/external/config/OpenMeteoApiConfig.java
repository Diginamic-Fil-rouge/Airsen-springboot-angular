package fr.airsen.api.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for Open-Meteo Weather API integration.
 * 
 * Provides weather forecast and historical data with authentication required.
 */
@Configuration
@ConfigurationProperties(prefix = "external.open-meteo")
public class OpenMeteoApiConfig {
    
    private String baseUrl = "https://api.open-meteo.com/v1";
    private int timeoutMs = 5000;
    private int retryAttempts = 2;

    /**
     * Creates a configured WebClient for Open-Meteo API.
     * 
     * @param builder base WebClient builder
     * @return configured WebClient for Open-Meteo API
     */
    @Bean
    @Qualifier("openMeteoWebClient")
    public WebClient openMeteoWebClient(WebClient.Builder builder) {
        return builder
            .baseUrl(baseUrl)
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