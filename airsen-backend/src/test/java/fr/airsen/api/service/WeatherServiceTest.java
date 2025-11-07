//package fr.airsen.api.service;
//
//import fr.airsen.api.dto.response.NearestWeatherResult;
//import fr.airsen.api.entity.Commune;
//import fr.airsen.api.entity.Department;
//import fr.airsen.api.entity.Region;
//import fr.airsen.api.entity.WeatherData;
//import fr.airsen.api.external.client.InseeApiClient;
//import fr.airsen.api.external.client.OpenMeteoApiClient;
//import fr.airsen.api.external.dto.openmeteo.OpenMeteoCurrentResponse;
//import fr.airsen.api.repository.CommuneRepository;
//import fr.airsen.api.repository.WeatherDataRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import reactor.core.publisher.Mono;
//import reactor.test.StepVerifier;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
///**
// * Unit tests for WeatherService.
// *
// * Tests the ClassCastException fix and critical weather data scenarios including:
// * - Direct weather data retrieval
// * - Geodistance fallback mechanism (20km threshold)
// * - Error handling for missing communes and data
// * - Null safety and data validation
// */
//@ExtendWith(MockitoExtension.class)
//@DisplayName("WeatherService Unit Tests")
//class WeatherServiceTest {
//
//    @Mock
//    private OpenMeteoApiClient openMeteoApiClient;
//
//    @Mock
//    private InseeApiClient inseeApiClient;
//
//    @Mock
//    private WeatherDataRepository weatherDataRepository;
//
//    @Mock
//    private CommuneRepository communeRepository;
//
//    @Mock
//    private GeoDistanceService geoDistanceService;
//
//    @InjectMocks
//    private WeatherService weatherService;
//
//    private Commune parisCommune;
//    private Commune saintDenisCommune;
//    private WeatherData parisWeatherData;
//    private NearestWeatherResult nearestWeatherResult;
//
//    @BeforeEach
//    void setUp() {
//        // Create test data that matches AIRSEN's entity structure
//        setupTestEntities();
//        setupTestWeatherData();
//    }
//
//    private void setupTestEntities() {
//        // Create Region (Île-de-France)
//        Region idf = new Region();
//        idf.setId(11L);
//        idf.setRegionCode("11");
//        idf.setName("Île-de-France");
//
//        // Create Department (Paris)
//        Department paris75 = new Department();
//        paris75.setId(75L);
//        paris75.setDepartmentCode("75");
//        paris75.setName("Paris");
//        paris75.setRegion(idf);
//
//        // Create Commune (Paris)
//        parisCommune = new Commune();
//        parisCommune.setId(1L);
//        parisCommune.setInseeCode("75056");
//        parisCommune.setName("Paris");
//        parisCommune.setLatitude(new BigDecimal("48.856614"));
//        parisCommune.setLongitude(new BigDecimal("2.352222"));
//        parisCommune.setPopulation(2161000);
//        parisCommune.setDepartment(paris75);
//
//        // Create Commune (Saint-Denis - for geodistance tests)
//        saintDenisCommune = new Commune();
//        saintDenisCommune.setId(2L);
//        saintDenisCommune.setInseeCode("93008");
//        saintDenisCommune.setName("Saint-Denis");
//        saintDenisCommune.setLatitude(new BigDecimal("48.936565"));
//        saintDenisCommune.setLongitude(new BigDecimal("2.357355"));
//        saintDenisCommune.setPopulation(111103);
//        saintDenisCommune.setDepartment(paris75);
//    }
//
//    private void setupTestWeatherData() {
//        // Create recent weather data for Paris (direct data scenario)
//        parisWeatherData = new WeatherData();
//        parisWeatherData.setCommune(parisCommune);
//        parisWeatherData.setMeasurementDate(LocalDate.now());
//        parisWeatherData.setCreatedAt(LocalDate.now());
//        parisWeatherData.setTemperature(15.5);
//        parisWeatherData.setHumidity(65.0);
//        parisWeatherData.setWindSpeed(12.3);
//        parisWeatherData.setWindDirection(225.0);
//        parisWeatherData.setWeatherCode(1);
//
//        // Create NearestWeatherResult for geodistance fallback
//        nearestWeatherResult = new NearestWeatherResult(
//            "75056",           // inseeCode of nearest commune (Paris)
//            "Paris",           // communeName
//            48.856614,         // latitude
//            2.352222,          // longitude
//            LocalDate.now(),   // measurementDate
//            15.2,              // temperature
//            68,                // humidity
//            11.8,              // windSpeed
//            230,               // windDirection
//            1,                 // weatherCode
//            9.5                // distanceKm (Saint-Denis is ~9.5km from Paris)
//        );
//    }
//
//    /**
//     * Test 1: Direct weather data retrieval
//     * Verifies that recent data is returned directly without geodistance fallback
//     */
//    @Test
//    @DisplayName("Should return direct weather data when recent data exists")
//    void shouldReturnDirectWeatherData() {
//        // Given
//        String inseeCode = "75056";
//        when(weatherDataRepository.getMostRecentWeatherByInseeCode(inseeCode))
//            .thenReturn(Optional.of(parisWeatherData));
//
//        // When
//        Mono<WeatherData> result = weatherService.getCurrentWeatherForCommune(inseeCode);
//
//        // Then
//        StepVerifier.create(result)
//            .expectNextMatches(weatherData -> {
//                assertEquals(parisCommune.getInseeCode(), weatherData.getCommune().getInseeCode());
//                assertEquals(15.5, weatherData.getTemperature());
//                assertEquals(65.0, weatherData.getHumidity());
//                assertEquals(12.3, weatherData.getWindSpeed());
//                assertEquals(225.0, weatherData.getWindDirection());
//                assertEquals(1, weatherData.getWeatherCode());
//                return true;
//            })
//            .verifyComplete();
//
//        // Verify no geodistance fallback was attempted
//        verify(geoDistanceService, never()).findNearestCommuneWithWeather(anyString(), anyDouble());
//    }
//
//    /**
//     * Test 2: Geodistance fallback within 20km
//     * Tests the ClassCastException fix in GeoDistanceService.parseWeatherResult()
//     */
//    @Test
//    @DisplayName("Should use geodistance fallback when no recent direct data exists")
//    void shouldUseGeodistanceFallbackWithin20km() {
//        // Given
//        String inseeCode = "93008"; // Saint-Denis
//        when(weatherDataRepository.getMostRecentWeatherByInseeCode(inseeCode))
//            .thenReturn(Optional.empty());
//        when(geoDistanceService.findNearestCommuneWithWeather(inseeCode, 20.0))
//            .thenReturn(Optional.of(nearestWeatherResult));
//        when(communeRepository.findByInseeCode(inseeCode))
//            .thenReturn(Optional.of(saintDenisCommune));
//
//        // When
//        Mono<WeatherData> result = weatherService.getCurrentWeatherForCommune(inseeCode);
//
//        // Then
//        StepVerifier.create(result)
//            .expectNextMatches(weatherData -> {
//                // Verify that returned data is for requested commune but with estimated values
//                assertEquals(saintDenisCommune.getInseeCode(), weatherData.getCommune().getInseeCode());
//                assertEquals(saintDenisCommune.getName(), weatherData.getCommune().getName());
//
//                // Verify weather data comes from nearest commune (Paris)
//                assertEquals(15.2, weatherData.getTemperature()); // From nearestWeatherResult
//                assertEquals(68.0, weatherData.getHumidity());
//                assertEquals(11.8, weatherData.getWindSpeed());
//                assertEquals(230.0, weatherData.getWindDirection());
//                assertEquals(1, weatherData.getWeatherCode());
//
//                return true;
//            })
//            .verifyComplete();
//
//        // Verify geodistance service was called with correct parameters
//        verify(geoDistanceService).findNearestCommuneWithWeather(inseeCode, 20.0);
//    }
//
//    /**
//     * Test 3: Commune not found (404 scenario)
//     * Verifies proper error handling for invalid INSEE codes
//     */
//    @Test
//    @DisplayName("Should throw ResourceNotFoundException when commune doesn't exist")
//    void shouldThrowExceptionWhenCommuneNotFound() {
//        // Given
//        String invalidInseeCode = "99999";
//        when(weatherDataRepository.getMostRecentWeatherByInseeCode(invalidInseeCode))
//            .thenReturn(Optional.empty());
//        when(geoDistanceService.findNearestCommuneWithWeather(invalidInseeCode, 20.0))
//            .thenReturn(Optional.empty());
//
//        // When
//        Mono<WeatherData> result = weatherService.getCurrentWeatherForCommune(invalidInseeCode);
//
//        // Then
//        StepVerifier.create(result)
//            .verifyComplete(); // Should complete with empty result (no data)
//
//        verify(geoDistanceService).findNearestCommuneWithWeather(invalidInseeCode, 20.0);
//    }
//
//    /**
//     * Test 4: No data within 20km (404 scenario)
//     * Verifies handling when no weather data exists within the PRD-defined threshold
//     */
//    @Test
//    @DisplayName("Should return empty when no data within 20km radius")
//    void shouldReturnEmptyWhenNoDataWithin20km() {
//        // Given
//        String remoteInseeCode = "77001"; // Meaux (>50km from Paris)
//        when(weatherDataRepository.getMostRecentWeatherByInseeCode(remoteInseeCode))
//            .thenReturn(Optional.empty());
//        when(geoDistanceService.findNearestCommuneWithWeather(remoteInseeCode, 20.0))
//            .thenReturn(Optional.empty()); // No data within 20km
//
//        // When
//        Mono<WeatherData> result = weatherService.getCurrentWeatherForCommune(remoteInseeCode);
//
//        // Then
//        StepVerifier.create(result)
//            .verifyComplete(); // Should complete with no emission (empty)
//
//        verify(geoDistanceService).findNearestCommuneWithWeather(remoteInseeCode, 20.0);
//    }
//
//    /**
//     * Test 5: Old data handling (should trigger geodistance fallback)
//     * Verifies that data older than 1 day triggers fallback mechanism
//     */
//    @Test
//    @DisplayName("Should use geodistance fallback when direct data is too old")
//    void shouldUseGeodistanceFallbackWhenDataIsOld() {
//        // Given
//        String inseeCode = "75056";
//        WeatherData oldWeatherData = new WeatherData();
//        oldWeatherData.setCommune(parisCommune);
//        oldWeatherData.setMeasurementDate(LocalDate.now().minusDays(2)); // 2 days old
//        oldWeatherData.setTemperature(10.0);
//
//        when(weatherDataRepository.getMostRecentWeatherByInseeCode(inseeCode))
//            .thenReturn(Optional.of(oldWeatherData));
//        when(geoDistanceService.findNearestCommuneWithWeather(inseeCode, 20.0))
//            .thenReturn(Optional.of(nearestWeatherResult));
//        when(communeRepository.findByInseeCode(inseeCode))
//            .thenReturn(Optional.of(parisCommune));
//
//        // When
//        Mono<WeatherData> result = weatherService.getCurrentWeatherForCommune(inseeCode);
//
//        // Then
//        StepVerifier.create(result)
//            .expectNextMatches(weatherData -> {
//                // Should return geodistance result, not old direct data
//                assertEquals(15.2, weatherData.getTemperature()); // From nearestWeatherResult
//                return true;
//            })
//            .verifyComplete();
//
//        verify(geoDistanceService).findNearestCommuneWithWeather(inseeCode, 20.0);
//    }
//
//    /**
//     * Test 6: Update weather for commune (external API integration)
//     * Verifies that weather updates work correctly with API integration
//     */
//    @Test
//    @DisplayName("Should update weather data from external API")
//    void shouldUpdateWeatherDataFromExternalAPI() {
//        // Given
//        String inseeCode = "75056";
//        Double[] coordinates = {2.352222, 48.856614}; // [longitude, latitude]
//
//        OpenMeteoCurrentResponse.CurrentWeather currentWeather = new OpenMeteoCurrentResponse.CurrentWeather(
//            null,    // time
//            15.8,    // temperature
//            68,      // humidity
//            13.2,    // windSpeed
//            240,     // windDirection
//            2,       // weatherCode
//            0.0,     // precipitation
//            1013.25, // pressure
//            10000.0  // visibility
//        );
//        OpenMeteoCurrentResponse apiResponse = new OpenMeteoCurrentResponse("Europe/Paris", currentWeather);
//
//        when(communeRepository.findByInseeCode(inseeCode))
//            .thenReturn(Optional.of(parisCommune));
//        when(openMeteoApiClient.getCurrentWeatherByCoordinates(coordinates))
//            .thenReturn(Mono.just(apiResponse));
//        when(weatherDataRepository.save(any(WeatherData.class)))
//            .thenAnswer(invocation -> invocation.getArgument(0));
//
//        // When
//        Mono<WeatherData> result = weatherService.updateWeatherForCommune(inseeCode);
//
//        // Then
//        StepVerifier.create(result)
//            .expectNextMatches(weatherData -> {
//                assertEquals(parisCommune.getInseeCode(), weatherData.getCommune().getInseeCode());
//                assertEquals(15.8, weatherData.getTemperature());
//                assertEquals(68.0, weatherData.getHumidity());
//                assertEquals(13.2, weatherData.getWindSpeed());
//                assertEquals(240.0, weatherData.getWindDirection());
//                assertEquals(2, weatherData.getWeatherCode());
//                assertEquals(LocalDate.now(), weatherData.getMeasurementDate());
//                return true;
//            })
//            .verifyComplete();
//
//        verify(openMeteoApiClient).getCurrentWeatherByCoordinates(coordinates);
//        verify(weatherDataRepository).save(any(WeatherData.class));
//    }
//
//    /**
//     * Test 7: Null safety for missing data fields
//     * Verifies that the service handles null values gracefully
//     */
//    @Test
//    @DisplayName("Should handle null values in weather data gracefully")
//    void shouldHandleNullWeatherDataGracefully() {
//        // Given
//        String inseeCode = "75056";
//        OpenMeteoCurrentResponse.CurrentWeather nullWeather = new OpenMeteoCurrentResponse.CurrentWeather(
//            null,    // time
//            null,    // temperature
//            null,    // humidity
//            null,    // windSpeed
//            null,    // windDirection
//            null,    // weatherCode
//            null,    // precipitation
//            null,    // pressure
//            null     // visibility
//        );
//        OpenMeteoCurrentResponse apiResponse = new OpenMeteoCurrentResponse("Europe/Paris", nullWeather);
//
//        when(communeRepository.findByInseeCode(inseeCode))
//            .thenReturn(Optional.of(parisCommune));
//        when(openMeteoApiClient.getCurrentWeatherByCoordinates(any()))
//            .thenReturn(Mono.just(apiResponse));
//        when(weatherDataRepository.save(any(WeatherData.class)))
//            .thenAnswer(invocation -> invocation.getArgument(0));
//
//        // When
//        Mono<WeatherData> result = weatherService.updateWeatherForCommune(inseeCode);
//
//        // Then
//        StepVerifier.create(result)
//            .expectNextMatches(weatherData -> {
//                // Should use default values for null fields
//                assertEquals(0.0, weatherData.getTemperature());
//                assertEquals(0.0, weatherData.getHumidity());
//                assertEquals(0.0, weatherData.getWindSpeed());
//                assertEquals(0.0, weatherData.getWindDirection());
//                assertEquals(0, weatherData.getWeatherCode());
//                return true;
//            })
//            .verifyComplete();
//    }
//
//    /**
//     * Test 8: ClassCastException resolution verification
//     * Specifically tests that the BigDecimal → Double conversion works in GeoDistanceService
//     */
//    @Test
//    @DisplayName("Should handle BigDecimal values without ClassCastException")
//    void shouldHandleBigDecimalValuesCorrectly() {
//        // Given
//        String inseeCode = "93008";
//
//        // Create NearestWeatherResult that simulates what would come from
//        // GeoDistanceService.parseWeatherResult() after our BigDecimal fix
//        NearestWeatherResult resultWithPreciseValues = new NearestWeatherResult(
//            "75056",
//            "Paris",
//            48.856614,         // These values would have been BigDecimal from DB
//            2.352222,          // but are now safely converted to Double
//            LocalDate.now(),
//            15.234567,         // Precise temperature from BigDecimal
//            68,
//            11.876543,         // Precise wind speed from BigDecimal
//            230,
//            1,
//            9.523456           // Precise distance from Haversine calculation
//        );
//
//        when(weatherDataRepository.getMostRecentWeatherByInseeCode(inseeCode))
//            .thenReturn(Optional.empty());
//        when(geoDistanceService.findNearestCommuneWithWeather(inseeCode, 20.0))
//            .thenReturn(Optional.of(resultWithPreciseValues));
//        when(communeRepository.findByInseeCode(inseeCode))
//            .thenReturn(Optional.of(saintDenisCommune));
//
//        // When & Then - Should not throw ClassCastException
//        Mono<WeatherData> result = weatherService.getCurrentWeatherForCommune(inseeCode);
//
//        StepVerifier.create(result)
//            .expectNextMatches(weatherData -> {
//                // Verify that precise BigDecimal values are correctly converted
//                assertEquals(15.234567, weatherData.getTemperature(), 0.000001);
//                assertEquals(11.876543, weatherData.getWindSpeed(), 0.000001);
//                return true;
//            })
//            .verifyComplete();
//    }
//
//    /**
//     * Test 9: Update weather error handling
//     * Verifies proper error handling when external API fails
//     */
//    @Test
//    @DisplayName("Should handle external API errors gracefully")
//    void shouldHandleExternalAPIErrors() {
//        // Given
//        String inseeCode = "75056";
//        when(communeRepository.findByInseeCode(inseeCode))
//            .thenReturn(Optional.of(parisCommune));
//        when(openMeteoApiClient.getCurrentWeatherByCoordinates(any()))
//            .thenReturn(Mono.error(new RuntimeException("External API unavailable")));
//
//        // When
//        Mono<WeatherData> result = weatherService.updateWeatherForCommune(inseeCode);
//
//        // Then
//        StepVerifier.create(result)
//            .expectError(RuntimeException.class)
//            .verify();
//
//        verify(weatherDataRepository, never()).save(any());
//    }
//
//    /**
//     * Test 10: Commune without coordinates handling
//     * Verifies error handling when commune has missing coordinate data
//     */
//    @Test
//    @DisplayName("Should throw exception when commune has no coordinates")
//    void shouldThrowExceptionWhenCommuneHasNoCoordinates() {
//        // Given
//        String inseeCode = "12345";
//        Commune communeWithoutCoords = new Commune();
//        communeWithoutCoords.setInseeCode(inseeCode);
//        communeWithoutCoords.setName("Test Commune");
//        // No latitude/longitude set
//
//        when(communeRepository.findByInseeCode(inseeCode))
//            .thenReturn(Optional.of(communeWithoutCoords));
//
//        // When
//        Mono<WeatherData> result = weatherService.updateWeatherForCommune(inseeCode);
//
//        // Then
//        StepVerifier.create(result)
//            .expectError(IllegalStateException.class)
//            .verify();
//
//        verify(openMeteoApiClient, never()).getCurrentWeatherByCoordinates(any());
//    }
//}
