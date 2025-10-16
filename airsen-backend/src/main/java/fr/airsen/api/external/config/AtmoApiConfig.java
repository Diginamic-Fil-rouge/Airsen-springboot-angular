package fr.airsen.api.external.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.util.StringUtils;
import reactor.netty.http.client.HttpClient;
import java.util.Base64;

/**
 * Configuration for ATMO France API integration.
 * 
 * Handles air quality data retrieval with username/password authentication
 * and rate limiting compliance.
 */
@Configuration
@ConfigurationProperties(prefix = "external.atmo-api")
public class AtmoApiConfig {
    
    private String baseUrl = "https://admindata.atmo-france.org";
    private String username;
    private String password;
    private int timeoutMs = 10000;
    private int retryAttempts = 3;
    private int maxRequestsPerMinute = 60;

    /**
     * Creates a configured WebClient for ATMO France API.
     * 
     * Configures increased buffer size to handle large ATMO API responses
     * that can exceed the default 1MB limit.
     * 
     * @param builder base WebClient builder
     * @return configured WebClient for ATMO API
     */
    @Bean
    @Qualifier("atmoWebClient")
    public WebClient atmoWebClient(WebClient.Builder builder) {
        // Create HttpClient with increased buffer size for large ATMO API responses
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(java.time.Duration.ofMillis(timeoutMs));
        
        WebClient.Builder clientBuilder = builder
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(baseUrl)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
            // Increase buffer size to 25MB to handle large ATMO API responses (typical size ~18MB)
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(25 * 1024 * 1024));
            
        // JWT token will be handled dynamically by the AtmoApiClient
        System.out.println("ATMO DEBUG: ATMO WebClient configured for JWT authentication");
        if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
            System.out.println("ATMO DEBUG: Credentials available - username: " + username + ", password: [PRESENT]");
        } else {
            System.out.println("ATMO DEBUG: No credentials available - username: " + username + ", password: " + (password != null ? "[PRESENT]" : "[MISSING]"));
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