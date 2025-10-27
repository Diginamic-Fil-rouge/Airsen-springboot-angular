package fr.airsen.api.external.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configuration for Open-Meteo Weather API integration.
 *
 * Provides weather forecast and historical data with authentication required.
 */
@Configuration
@ConfigurationProperties(prefix = "external.open-meteo")
public class OpenMeteoApiConfig {

    private String baseUrl = "https://api.open-meteo.com/v1";
    private int timeoutMs = 20000;
    private int retryAttempts = 3;

    /**
     * Creates a configured WebClient for Open-Meteo API.
     *
     * @param builder base WebClient builder
     * @return configured WebClient for Open-Meteo API
     */
    @Bean
    @Qualifier("openMeteoWebClient")
    public WebClient openMeteoWebClient(WebClient.Builder builder) {
        // Configure connection and read timeouts
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMillis(timeoutMs))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs);

        return builder
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
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
