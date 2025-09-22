package fr.airsen.api.external.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.core.publisher.Mono;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Base configuration for WebClient instances used by external API clients.
 * 
 * Provides common timeout, error handling, and logging configuration
 * for all external API communications.
 */
@Configuration
public class WebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    /**
     * Creates a base WebClient builder with common configurations.
     * 
     * @return configured WebClient builder
     */
    @Bean
    @Primary
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
            .clientConnector(httpConnector())
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024))
            .filter(logRequest())
            .filter(logResponse());
    }

    /**
     * Configures HTTP connector with timeouts and connection settings.
     * 
     * @return configured ReactorClientHttpConnector
     */
    @Bean
    public ReactorClientHttpConnector httpConnector() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
            .responseTimeout(Duration.ofSeconds(30))
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(30))
                    .addHandlerLast(new WriteTimeoutHandler(30)));
        
        return new ReactorClientHttpConnector(httpClient);
    }

    /**
     * Creates filter for logging outgoing requests.
     * 
     * @return request logging filter
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                log.debug("External API Request: {} {}", 
                    clientRequest.method(), clientRequest.url());
            }
            return Mono.just(clientRequest);
        });
    }

    /**
     * Creates filter for logging incoming responses.
     * 
     * @return response logging filter
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (log.isDebugEnabled()) {
                log.debug("External API Response: {} for {}", 
                    clientResponse.statusCode(), clientResponse.request().getURI());
            }
            return Mono.just(clientResponse);
        });
    }
}