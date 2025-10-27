package fr.airsen.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Jackson Configuration for JSON Parsing
 * 
 * This configuration ensures robust JSON parsing and serialization across the application:
 * - Handles special characters in strings (e.g., !, @, #, $ in passwords)
 * - Proper Java 8 date/time serialization (LocalDateTime, ZonedDateTime)
 * - Flexible parsing for external API responses
 * - Consistent JSON formatting for REST API responses
 */
@Configuration
public class JacksonConfig {

    /**
     * Configures the primary ObjectMapper bean for the application.
     * 
     * Features:
     * - Backslash escaping for special characters
     * - Java Time Module for date/time handling
     * - Dates serialized as ISO-8601 strings (not timestamps)
     * - Null values excluded from JSON output
     * 
     * @param builder Spring's Jackson2ObjectMapperBuilder
     * @return Configured ObjectMapper instance
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();

        // Register Java 8 date/time module for proper LocalDateTime, ZonedDateTime handling
        objectMapper.registerModule(new JavaTimeModule());

        // Serialize dates as ISO-8601 strings, not timestamps
        // Example: "2024-10-16T11:30:00" instead of 1697456400000
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Don't fail on empty beans (useful for DTOs)
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return objectMapper;
    }
}
