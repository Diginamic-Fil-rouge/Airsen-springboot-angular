package fr.airsen.api.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for ATMO France API integration.
 * 
 * Handles air quality data retrieval with proper authentication
 * and rate limiting compliance.
 */
@Configuration
@ConfigurationProperties(prefix = "external.atmo-api")
public class AtmoApiConfig {
    
    private String baseUrl = "https://admindata.atmo-france.org";
    private String jwtToken;
    private String username;
    private String password;
    private int timeoutMs = 10000;
    private int retryAttempts = 3;
    private int maxRequestsPerMinute = 60;

    /**
     * Creates a configured WebClient for ATMO France API.
     * 
     * @param builder base WebClient builder
     * @return configured WebClient for ATMO API
     */
    @Bean
    @Qualifier("atmoWebClient")
    public WebClient atmoWebClient(WebClient.Builder builder) {
        WebClient.Builder clientBuilder = builder
            .baseUrl(baseUrl)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
            
        // Add JWT token header if configured
        if (jwtToken != null && !jwtToken.isEmpty()) {
            clientBuilder.defaultHeader("Authorization", "Bearer " + jwtToken);
        }
        
        return clientBuilder.build();
    }

    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getJwtToken() {
        return jwtToken;
    }

    public void setJwtToken(String jwtToken) {
        this.jwtToken = jwtToken;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public int getMaxRequestsPerMinute() {
        return maxRequestsPerMinute;
    }

    public void setMaxRequestsPerMinute(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }
}