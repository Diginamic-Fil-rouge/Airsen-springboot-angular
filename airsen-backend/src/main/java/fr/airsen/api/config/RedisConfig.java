package fr.airsen.api.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis configuration for caching external API responses and session data.
 *
 * Configures connection settings, serialization, and provides RedisTemplate beans
 * for different use cases (caching, sessions, etc.).
 *
 * NOTE: This configuration is optional and will only activate if Redis is available.
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.host", havingValue = "redis", matchIfMissing = false)
public class RedisConfig {

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.database:0}")
    private int redisDatabase;

    /**
     * Creates Redis connection factory with configured settings.
     * 
     * @return configured LettuceConnectionFactory
     */
    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        log.info("Configuring Redis connection to {}:{} database {}", redisHost, redisPort, redisDatabase);
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        config.setDatabase(redisDatabase);
        
        if (redisPassword != null && !redisPassword.trim().isEmpty()) {
            config.setPassword(redisPassword);
        }
        
        return new LettuceConnectionFactory(config);
    }

    /**
     * Creates the main RedisTemplate for general caching purposes.
     * 
     * @param connectionFactory Redis connection factory
     * @return configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        log.info("Creating RedisTemplate bean for caching");
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        

        // Use JSON serializer for values (without polymorphic type info to avoid @class requirement)
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        // Disable default typing to avoid @class property requirement for Records and DTOs
        // This prevents serialization issues with external API response objects
        
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();

        return template;
    }

    /**
     * Creates CacheManager with different TTL configurations for different cache types.
     *
     * @param connectionFactory Redis connection factory
     * @return configured RedisCacheManager
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Creating CacheManager bean with custom TTL configurations");

        // Default configuration - 1 hour TTL
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // Cache-specific configurations with different TTL values
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Air Quality Cache - 1 hour TTL (real-time data from ATMO France)
        cacheConfigurations.put("air-quality", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Weather Cache - 30 minutes TTL (frequently updated from Open-Meteo)
        cacheConfigurations.put("weather", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Population Cache - 24 hours TTL (static data from INSEE)
        cacheConfigurations.put("population", defaultConfig.entryTtl(Duration.ofHours(24)));

        // Geographic Cache - 7 days TTL (rarely changes)
        cacheConfigurations.put("geography", defaultConfig.entryTtl(Duration.ofDays(7)));

        // Commune Cache - 7 days TTL (static administrative data)
        cacheConfigurations.put("communes", defaultConfig.entryTtl(Duration.ofDays(7)));

        // Region Cache - 7 days TTL (static administrative data)
        cacheConfigurations.put("regions", defaultConfig.entryTtl(Duration.ofDays(7)));

        // Department Cache - 7 days TTL (static administrative data)
        cacheConfigurations.put("departments", defaultConfig.entryTtl(Duration.ofDays(7)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
