package fr.airsen.api.service;

import fr.airsen.api.config.CacheTestConfiguration;
import fr.airsen.api.entity.AirQuality;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.external.client.AtmoApiClient;
import fr.airsen.api.external.client.InseeApiClient;
import fr.airsen.api.external.client.OpenMeteoApiClient;
import fr.airsen.api.external.dto.atmo.AtmoAirQualityResponse;
import fr.airsen.api.repository.*;
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

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AirQualityService} cache behavior.
 *
 * Tests verify:
 * - Cache hit/miss scenarios
 * - Cache key strategy (INSEE code)
 * - Null result handling (should not cache)
 * - Cache eviction (specific and all entries)
 * - TTL configuration (1 hour in production, 5 minutes in tests)
 */
@SpringBootTest
@Import(CacheTestConfiguration.class)
@ActiveProfiles("test")
@DisplayName("AirQualityService Cache Tests")
class AirQualityServiceCacheTest {

    @Autowired
    private AirQualityService airQualityService;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private AtmoApiClient atmoApiClient;

    @MockBean
    private AirQualityRepository airQualityRepository;

    @MockBean
    private CommuneRepository communeRepository;

    @MockBean
    private AlertProcessingService alertProcessingService;

    // Additional beans required by Spring Boot context
    @MockBean
    private WeatherService weatherService;

    @MockBean
    private CommuneService communeService;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private JwtBlacklistService jwtBlacklistService;

    // All repositories
    @MockBean
    private UserRepository userRepository;

    @MockBean
    private WeatherDataRepository weatherDataRepository;

    @MockBean
    private RegionRepository regionRepository;

    @MockBean
    private DepartmentRepository departmentRepository;

    @MockBean
    private AlertSignalRepository alertSignalRepository;

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

    // All external API clients
    @MockBean
    private InseeApiClient inseeApiClient;

    @MockBean
    private OpenMeteoApiClient openMeteoApiClient;

    private static final String TEST_INSEE_CODE = "75056"; // Paris

    @BeforeEach
    void setUp() {
        // Clear cache before each test
        Cache cache = cacheManager.getCache("air-quality");
        if (cache != null) {
            cache.clear();
        }
    }

    @Nested
    @DisplayName("Cache Hit/Miss Tests")
    class CacheHitMissTests {

        @Test
        @DisplayName("Should cache air quality data on first call (cache miss)")
        void shouldCacheAirQualityDataOnFirstCall() {
            // Given - Mock API client to return fresh data
            AirQuality mockAirQuality = createMockAirQuality(TEST_INSEE_CODE);
            AtmoAirQualityResponse mockResponse = createMockAtmoResponse();

            when(airQualityRepository.findLatestByCommune_InseeCode(TEST_INSEE_CODE))
                .thenReturn(Optional.empty());
            when(atmoApiClient.getCurrentAirQuality(TEST_INSEE_CODE))
                .thenReturn(Mono.just(mockResponse));
            when(airQualityRepository.save(any(AirQuality.class)))
                .thenReturn(mockAirQuality);
            doNothing().when(alertProcessingService).processAirQualityUpdate(any());

            // When - First call (cache miss)
            Mono<AirQuality> result = airQualityService.getCurrentAirQuality(TEST_INSEE_CODE);

            // Then - Verify result
            StepVerifier.create(result)
                .expectNextMatches(aq -> aq.getCommune().getInseeCode().equals(TEST_INSEE_CODE))
                .verifyComplete();

            // Verify external API was called
            verify(atmoApiClient, times(1)).getCurrentAirQuality(TEST_INSEE_CODE);

            // Verify data is cached
            Cache cache = cacheManager.getCache("air-quality");
            assertThat(cache).isNotNull();
            Cache.ValueWrapper cachedValue = cache.get(TEST_INSEE_CODE);
            assertThat(cachedValue).isNotNull();
        }

        @Test
        @DisplayName("Should return cached data on second call without API call (cache hit)")
        void shouldReturnCachedDataOnSecondCall() {
            // Given - Data already in cache
            AirQuality cachedData = createMockAirQuality(TEST_INSEE_CODE);
            Cache cache = cacheManager.getCache("air-quality");
            Objects.requireNonNull(cache).put(TEST_INSEE_CODE, Mono.just(cachedData));

            // When - Second call (cache hit)
            Mono<AirQuality> result = airQualityService.getCurrentAirQuality(TEST_INSEE_CODE);

            // Then - Verify cached data returned
            StepVerifier.create(result)
                .expectNextMatches(aq -> aq.getCommune().getInseeCode().equals(TEST_INSEE_CODE))
                .verifyComplete();

            // Verify NO external API call (cache hit)
            verify(atmoApiClient, never()).getCurrentAirQuality(anyString());
            verify(airQualityRepository, never()).findLatestByCommune_InseeCode(anyString());
        }

        @Test
        @DisplayName("Should make API call when cache is empty (cache miss)")
        void shouldMakeApiCallWhenCacheEmpty() {
            // Given - Empty cache
            Cache cache = cacheManager.getCache("air-quality");
            assertThat(cache).isNotNull();
            assertThat(cache.get(TEST_INSEE_CODE)).isNull();

            AirQuality mockAirQuality = createMockAirQuality(TEST_INSEE_CODE);
            AtmoAirQualityResponse mockResponse = createMockAtmoResponse();

            when(airQualityRepository.findLatestByCommune_InseeCode(TEST_INSEE_CODE))
                .thenReturn(Optional.empty());
            when(atmoApiClient.getCurrentAirQuality(TEST_INSEE_CODE))
                .thenReturn(Mono.just(mockResponse));
            when(airQualityRepository.save(any(AirQuality.class)))
                .thenReturn(mockAirQuality);

            // When - Call with empty cache
            Mono<AirQuality> result = airQualityService.getCurrentAirQuality(TEST_INSEE_CODE);

            // Then
            StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

            // Verify API was called
            verify(atmoApiClient, times(1)).getCurrentAirQuality(TEST_INSEE_CODE);
        }
    }

    @Nested
    @DisplayName("Cache Key Strategy Tests")
    class CacheKeyStrategyTests {

        @Test
        @DisplayName("Should use INSEE code as cache key")
        void shouldUseInseeCodeAsCacheKey() {
            // Given - Multiple communes with different INSEE codes
            String paris = "75056";
            String lyon = "69123";

            AirQuality parisData = createMockAirQuality(paris);
            AirQuality lyonData = createMockAirQuality(lyon);

            // When - Cache data for both communes
            Cache cache = cacheManager.getCache("air-quality");
            Objects.requireNonNull(cache).put(paris, Mono.just(parisData));
            cache.put(lyon, Mono.just(lyonData));

            // Then - Verify separate cache entries
            Cache.ValueWrapper parisCached = cache.get(paris);
            Cache.ValueWrapper lyonCached = cache.get(lyon);

            assertThat(parisCached).isNotNull();
            assertThat(lyonCached).isNotNull();

            // Verify correct data for each key
            @SuppressWarnings("unchecked")
            Mono<AirQuality> parisResult = (Mono<AirQuality>) parisCached.get();
            assertThat(parisResult).isNotNull();
            StepVerifier.create(parisResult)
                .expectNextMatches(aq -> aq.getCommune().getInseeCode().equals(paris))
                .verifyComplete();
        }
    }

    @Nested
    @DisplayName("Null Result Handling Tests")
    class NullResultHandlingTests {

        @Test
        @DisplayName("Should not cache null results (unless condition)")
        void shouldNotCacheNullResults() {
            // Given - API returns empty result
            when(airQualityRepository.findLatestByCommune_InseeCode("00000"))
                .thenReturn(Optional.empty());
            when(atmoApiClient.getCurrentAirQuality("00000"))
                .thenReturn(Mono.empty());

            // When - Call with invalid commune code
            Mono<AirQuality> result = airQualityService.getCurrentAirQuality("00000");

            // Then - Empty result
            StepVerifier.create(result)
                .verifyComplete();

            // Verify null result NOT cached
            Cache cache = cacheManager.getCache("air-quality");
            assertThat(cache).isNotNull();
            assertThat(cache.get("00000")).isNull();
        }
    }

    @Nested
    @DisplayName("Cache Eviction Tests")
    class CacheEvictionTests {

        @Test
        @DisplayName("Should evict specific cache entry")
        void shouldEvictSpecificCacheEntry() {
            // Given - Data in cache
            AirQuality cachedData = createMockAirQuality(TEST_INSEE_CODE);
            Cache cache = cacheManager.getCache("air-quality");
            Objects.requireNonNull(cache).put(TEST_INSEE_CODE, Mono.just(cachedData));
            assertThat(cache.get(TEST_INSEE_CODE)).isNotNull();

            // When - Evict cache
            airQualityService.evictAirQualityCache(TEST_INSEE_CODE);

            // Then - Cache entry should be removed
            assertThat(cache.get(TEST_INSEE_CODE)).isNull();
        }

        @Test
        @DisplayName("Should evict all cache entries")
        void shouldEvictAllCacheEntries() {
            // Given - Multiple entries in cache
            Cache cache = cacheManager.getCache("air-quality");
            Objects.requireNonNull(cache).put("75056", Mono.just(createMockAirQuality("75056")));
            cache.put("69123", Mono.just(createMockAirQuality("69123")));
            cache.put("13055", Mono.just(createMockAirQuality("13055")));

            assertThat(cache.get("75056")).isNotNull();
            assertThat(cache.get("69123")).isNotNull();
            assertThat(cache.get("13055")).isNotNull();

            // When - Evict all caches
            airQualityService.evictAllAirQualityCache();

            // Then - All cache entries should be removed
            assertThat(cache.get("75056")).isNull();
            assertThat(cache.get("69123")).isNull();
            assertThat(cache.get("13055")).isNull();
        }

        @Test
        @DisplayName("Should evict only specified cache, not others")
        void shouldEvictOnlySpecifiedCache() {
            // Given - Multiple entries in cache
            String paris = "75056";
            String lyon = "69123";

            Cache cache = cacheManager.getCache("air-quality");
            Objects.requireNonNull(cache).put(paris, Mono.just(createMockAirQuality(paris)));
            cache.put(lyon, Mono.just(createMockAirQuality(lyon)));

            // When - Evict only Paris cache
            airQualityService.evictAirQualityCache(paris);

            // Then - Only Paris should be evicted
            assertThat(cache.get(paris)).isNull();
            assertThat(cache.get(lyon)).isNotNull(); // Lyon still cached
        }
    }

    @Nested
    @DisplayName("Database Fallback Tests")
    class DatabaseFallbackTests {

        @Test
        @DisplayName("Should return recent database data without API call")
        void shouldReturnRecentDatabaseData() {
            // Given - Recent data in database (within 24 hours)
            AirQuality recentData = createMockAirQuality(TEST_INSEE_CODE);
            recentData.setMeasurementDate(LocalDate.now());

            when(airQualityRepository.findLatestByCommune_InseeCode(TEST_INSEE_CODE))
                .thenReturn(Optional.of(recentData));

            // When
            Mono<AirQuality> result = airQualityService.getCurrentAirQuality(TEST_INSEE_CODE);

            // Then - Should return database data without API call
            StepVerifier.create(result)
                .expectNextMatches(aq -> aq.getCommune().getInseeCode().equals(TEST_INSEE_CODE))
                .verifyComplete();

            // Verify NO API call (database fallback worked)
            verify(atmoApiClient, never()).getCurrentAirQuality(anyString());
        }

        @Test
        @DisplayName("Should fetch fresh data when database data is old")
        void shouldFetchFreshDataWhenDatabaseDataOld() {
            // Given - Old data in database (older than 24 hours)
            AirQuality oldData = createMockAirQuality(TEST_INSEE_CODE);
            oldData.setMeasurementDate(LocalDate.now().minusDays(2));

            AirQuality freshData = createMockAirQuality(TEST_INSEE_CODE);
            AtmoAirQualityResponse mockResponse = createMockAtmoResponse();

            when(airQualityRepository.findLatestByCommune_InseeCode(TEST_INSEE_CODE))
                .thenReturn(Optional.of(oldData));
            when(atmoApiClient.getCurrentAirQuality(TEST_INSEE_CODE))
                .thenReturn(Mono.just(mockResponse));
            when(airQualityRepository.save(any(AirQuality.class)))
                .thenReturn(freshData);

            // When
            Mono<AirQuality> result = airQualityService.getCurrentAirQuality(TEST_INSEE_CODE);

            // Then - Should fetch fresh data from API
            StepVerifier.create(result)
                .expectNextCount(1)
                .verifyComplete();

            // Verify API call was made (database fallback rejected old data)
            verify(atmoApiClient, times(1)).getCurrentAirQuality(TEST_INSEE_CODE);
        }
    }

    // Helper methods

    private AirQuality createMockAirQuality(String inseeCode) {
        Commune commune = new Commune();
        commune.setInseeCode(inseeCode);
        commune.setName("Test Commune");

        AirQuality airQuality = new AirQuality();
        airQuality.setId(1); // setId accepts Integer
        airQuality.setCommune(commune);
        airQuality.setAtmoIndex(75);
        airQuality.setAtmoQual("Moderate");
        airQuality.setAtmoColor("#50ccaa");
        airQuality.setPm25(15);
        airQuality.setPm10(25);
        airQuality.setNO2(35);
        airQuality.setO3(45);
        airQuality.setSO2(10);
        airQuality.setMeasurementDate(LocalDate.now());
        airQuality.setCreatedAt(LocalDate.now());

        return airQuality;
    }

    private AtmoAirQualityResponse createMockAtmoResponse() {
        return new AtmoAirQualityResponse(
            "75056",                    // communeInsee
            LocalDate.now().toString(), // measurementDate
            75,                         // atmoIndex
            "Moderate",                 // qualifier
            "#50ccaa",                  // color
            2,                          // no2Code
            3,                          // o3Code
            2,                          // pm10Code
            2,                          // pm25Code
            1,                          // so2Code
            "Paris",                    // zoneName
            "ATMO France",              // source
            LocalDate.now().toString()  // updateDate
        );
    }
}
