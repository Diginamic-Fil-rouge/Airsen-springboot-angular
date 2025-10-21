package fr.airsen.api.service;

import fr.airsen.api.config.CacheTestConfiguration;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.WeatherData;
import fr.airsen.api.external.client.AtmoApiClient;
import fr.airsen.api.external.client.InseeApiClient;
import fr.airsen.api.external.client.OpenMeteoApiClient;
import fr.airsen.api.external.dto.openmeteo.OpenMeteoCurrentResponse;
import fr.airsen.api.repository.*;
import fr.airsen.api.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link WeatherService} cache behavior.
 *
 * Tests verify:
 * - Weather data caching with 30-minute TTL
 * - Cache hit/miss scenarios
 * - Database fallback for recent data
 * - Cache eviction (specific and all entries)
 */
@SpringBootTest
@Import(CacheTestConfiguration.class)
@ActiveProfiles("test")
@DisplayName("WeatherService Cache Tests")
class WeatherServiceCacheTest {

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private OpenMeteoApiClient openMeteoApiClient;

    @MockBean
    private WeatherDataRepository weatherDataRepository;

    @MockBean
    private CommuneRepository communeRepository;

    // Additional beans required by Spring Boot context
    @MockBean
    private AirQualityService airQualityService;
    
    @MockBean
    private CommuneService communeService;
    
    @MockBean
    private NotificationService notificationService;
    
    @MockBean
    private JwtBlacklistService jwtBlacklistService;
    
    @MockBean
    private AlertProcessingService alertProcessingService;

    // All repositories (excluding already mocked ones)
    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private AirQualityRepository airQualityRepository;
    
    @MockBean
    private RegionRepository regionRepository;
    
    @MockBean
    private DepartmentRepository departmentRepository;
    
    @MockBean
    private AlertRepository alertRepository;
    
    @MockBean
    private AlertHistoryRepository alertHistoryRepository;
    
    @MockBean
    private NotificationRepository notificationRepository;
    
    @MockBean
    private ForumCategoryRepository forumCategoryRepository;
    
    @MockBean
    private ForumThreadRepository forumThreadRepository;
    
    @MockBean
    private ForumMessageRepository forumMessageRepository;
    
    @MockBean
    private ForumVoteRepository forumVoteRepository;

    // All external API clients (excluding already mocked ones)
    @MockBean
    private InseeApiClient inseeApiClient;
    
    @MockBean
    private AtmoApiClient atmoApiClient;

    private static final String TEST_INSEE_CODE = "75056"; // Paris

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        Cache cache = cacheManager.getCache("weather");
        if (cache != null) {
            cache.clear();
        }
    }

    @Nested
    @DisplayName("Cache Hit/Miss Tests")
    class CacheHitMissTests {

        @Test
        @DisplayName("Should cache weather data on first call (cache miss)")
        void shouldCacheWeatherDataOnFirstCall() {
            // Given - Mock dependencies
            Commune mockCommune = createMockCommune(TEST_INSEE_CODE);
            WeatherData mockWeatherData = createMockWeatherData(TEST_INSEE_CODE);
            OpenMeteoCurrentResponse mockResponse = createMockOpenMeteoResponse();

            when(communeRepository.findByInseeCode(TEST_INSEE_CODE))
                .thenReturn(Optional.of(mockCommune));
            when(weatherDataRepository.getMostRecentWeatherByInseeCode(TEST_INSEE_CODE))
                .thenReturn(Optional.empty());
            when(openMeteoApiClient.getCurrentWeatherByCoordinates(any(Double[].class)))
                .thenReturn(Mono.just(mockResponse));
            when(weatherDataRepository.save(any(WeatherData.class)))
                .thenReturn(mockWeatherData);

            // When - First call (cache miss)
            Mono<WeatherData> result = weatherService.getCurrentWeatherForCommune(TEST_INSEE_CODE);

            // Then - Verify result
            StepVerifier.create(result)
                .expectNextMatches(wd -> wd.getCommune().getInseeCode().equals(TEST_INSEE_CODE))
                .verifyComplete();

            // Verify external API was called
            verify(openMeteoApiClient, times(1)).getCurrentWeatherByCoordinates(any(Double[].class));

            // Verify data is cached
            Cache cache = cacheManager.getCache("weather");
            assertThat(cache).isNotNull();
            assertThat(cache.get(TEST_INSEE_CODE)).isNotNull();
        }

        @Test
        @DisplayName("Should return cached data on second call without API call (cache hit)")
        void shouldReturnCachedDataOnSecondCall() {
            // Given - Data already in cache
            WeatherData cachedData = createMockWeatherData(TEST_INSEE_CODE);
            Cache cache = cacheManager.getCache("weather");
            Objects.requireNonNull(cache).put(TEST_INSEE_CODE, Mono.just(cachedData));

            // When - Second call (cache hit)
            Mono<WeatherData> result = weatherService.getCurrentWeatherForCommune(TEST_INSEE_CODE);

            // Then - Verify cached data returned
            StepVerifier.create(result)
                .expectNextMatches(wd -> wd.getCommune().getInseeCode().equals(TEST_INSEE_CODE))
                .verifyComplete();

            // Verify NO external API call (cache hit)
            verify(openMeteoApiClient, never()).getCurrentWeatherByCoordinates(any());
            verify(weatherDataRepository, never()).getMostRecentWeatherByInseeCode(anyString());
        }
    }

    @Nested
    @DisplayName("Cache Eviction Tests")
    class CacheEvictionTests {

        @Test
        @DisplayName("Should evict specific cache entry")
        void shouldEvictSpecificCacheEntry() {
            // Given - Data in cache
            WeatherData cachedData = createMockWeatherData(TEST_INSEE_CODE);
            Cache cache = cacheManager.getCache("weather");
            Objects.requireNonNull(cache).put(TEST_INSEE_CODE, Mono.just(cachedData));
            assertThat(cache.get(TEST_INSEE_CODE)).isNotNull();

            // When - Evict cache
            weatherService.evictWeatherCache(TEST_INSEE_CODE);

            // Then - Cache entry should be removed
            assertThat(cache.get(TEST_INSEE_CODE)).isNull();
        }

        @Test
        @DisplayName("Should evict all cache entries")
        void shouldEvictAllCacheEntries() {
            // Given - Multiple entries in cache
            Cache cache = cacheManager.getCache("weather");
            Objects.requireNonNull(cache).put("75056", Mono.just(createMockWeatherData("75056")));
            cache.put("69123", Mono.just(createMockWeatherData("69123")));
            cache.put("13055", Mono.just(createMockWeatherData("13055")));

            assertThat(cache.get("75056")).isNotNull();
            assertThat(cache.get("69123")).isNotNull();
            assertThat(cache.get("13055")).isNotNull();

            // When - Evict all caches
            weatherService.evictAllWeatherCache();

            // Then - All cache entries should be removed
            assertThat(cache.get("75056")).isNull();
            assertThat(cache.get("69123")).isNull();
            assertThat(cache.get("13055")).isNull();
        }
    }

    @Nested
    @DisplayName("Database Fallback Tests")
    class DatabaseFallbackTests {

        @Test
        @DisplayName("Should return recent database data without API call")
        void shouldReturnRecentDatabaseData() {
            // Given - Recent data in database (within 24 hours)
            WeatherData recentData = createMockWeatherData(TEST_INSEE_CODE);
            recentData.setMeasurementDate(LocalDate.now());

            when(weatherDataRepository.getMostRecentWeatherByInseeCode(TEST_INSEE_CODE))
                .thenReturn(Optional.of(recentData));

            // When
            Mono<WeatherData> result = weatherService.getCurrentWeatherForCommune(TEST_INSEE_CODE);

            // Then - Should return database data without API call
            StepVerifier.create(result)
                .expectNextMatches(wd -> wd.getCommune().getInseeCode().equals(TEST_INSEE_CODE))
                .verifyComplete();

            // Verify NO API call (database fallback worked)
            verify(openMeteoApiClient, never()).getCurrentWeatherByCoordinates(any());
        }

        @Test
        @DisplayName("Should fetch fresh data when database data is old")
        void shouldFetchFreshDataWhenDatabaseDataOld() {
            // Given - Old data in database (older than 24 hours)
            WeatherData oldData = createMockWeatherData(TEST_INSEE_CODE);
            oldData.setMeasurementDate(LocalDate.now().minusDays(2));

            Commune mockCommune = createMockCommune(TEST_INSEE_CODE);
            WeatherData freshData = createMockWeatherData(TEST_INSEE_CODE);
            OpenMeteoCurrentResponse mockResponse = createMockOpenMeteoResponse();

            when(weatherDataRepository.getMostRecentWeatherByInseeCode(TEST_INSEE_CODE))
                .thenReturn(Optional.of(oldData));
            when(communeRepository.findByInseeCode(TEST_INSEE_CODE))
                .thenReturn(Optional.of(mockCommune));
            when(openMeteoApiClient.getCurrentWeatherByCoordinates(any(Double[].class)))
                .thenReturn(Mono.just(mockResponse));
            when(weatherDataRepository.save(any(WeatherData.class)))
                .thenReturn(freshData);

            // When
            Mono<WeatherData> result = weatherService.getCurrentWeatherForCommune(TEST_INSEE_CODE);

            // Then - Should fetch fresh data from API
            StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

            // Verify API call was made (database fallback rejected old data)
            verify(openMeteoApiClient, times(1)).getCurrentWeatherByCoordinates(any(Double[].class));
        }
    }

    // Helper methods

    private Commune createMockCommune(String inseeCode) {
        Commune commune = new Commune();
        commune.setInseeCode(inseeCode);
        commune.setName("Test Commune");
        commune.setLatitude(new BigDecimal("48.8566"));
        commune.setLongitude(new BigDecimal("2.3522"));
        return commune;
    }

    private WeatherData createMockWeatherData(String inseeCode) {
        Commune commune = createMockCommune(inseeCode);

        WeatherData weatherData = new WeatherData();
        weatherData.setId(1);
        weatherData.setCommune(commune);
        weatherData.setTemperature(20.5);
        weatherData.setHumidity(65.0);
        weatherData.setWindSpeed(10.0);
        weatherData.setWindDirection(180.0);
        weatherData.setWeatherCode(1); // Clear sky
        weatherData.setMeasurementDate(LocalDate.now());
        weatherData.setCreatedAt(LocalDate.now());

        return weatherData;
    }

    private OpenMeteoCurrentResponse createMockOpenMeteoResponse() {
        OpenMeteoCurrentResponse.CurrentWeather currentWeather =
            new OpenMeteoCurrentResponse.CurrentWeather(
                java.time.LocalDateTime.now(),
                20.5,           // temperature
                65,             // humidity
                10.0,           // windSpeed
                180,            // windDirection
                1,              // weatherCode
                0.0,            // precipitation
                1013.25,        // pressure
                10000.0         // visibility
            );

        return new OpenMeteoCurrentResponse(
            "Europe/Paris",
            currentWeather
        );
    }
}
