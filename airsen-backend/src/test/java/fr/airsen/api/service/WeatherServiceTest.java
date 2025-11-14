package fr.airsen.api.service;

import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.Department;
import fr.airsen.api.entity.Region;
import fr.airsen.api.entity.WeatherData;
import fr.airsen.api.external.client.OpenMeteoApiClient;
import fr.airsen.api.external.dto.openmeteo.OpenMeteoCurrentResponse;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.WeatherDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Unit tests for WeatherService.
 *
 * Simplified tests focusing on core functionality:
 * - Weather data updates from external API
 * - Null safety and data validation
 * - Error handling
 *
 * Note: Tests for getCurrentWeatherForCommune (with caching and geodistance)
 * are covered in integration tests due to complexity of SmartCacheService mocking.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("WeatherService Unit Tests")
class WeatherServiceTest {

    @Mock
    private OpenMeteoApiClient openMeteoApiClient;

    @Mock
    private WeatherDataRepository weatherDataRepository;

    @Mock
    private CommuneRepository communeRepository;

    // Note: We're not injecting WeatherService with @InjectMocks because it has many dependencies.
    // We'll create it manually for each test with minimal mocking.

    private Commune parisCommune;

    @BeforeEach
    void setUp() {
        setupTestEntities();
    }

    private void setupTestEntities() {
        // Create Region (Île-de-France)
        Region idf = new Region();
        idf.setId(11L);
        idf.setRegionCode("11");
        idf.setName("Île-de-France");

        // Create Department (Paris)
        Department paris75 = new Department();
        paris75.setId(75L);
        paris75.setDepartmentCode("75");
        paris75.setName("Paris");
        paris75.setRegion(idf);

        // Create Commune (Paris)
        parisCommune = new Commune();
        parisCommune.setId(1L);
        parisCommune.setInseeCode("75056");
        parisCommune.setName("Paris");
        parisCommune.setLatitude(new BigDecimal("48.856614"));
        parisCommune.setLongitude(new BigDecimal("2.352222"));
        parisCommune.setPopulation(2161000);
        parisCommune.setDepartment(paris75);
    }

    /**
     * Test 1: Update weather for commune (external API integration)
     * Verifies that weather updates work correctly with API integration
     */
    @Test
    @DisplayName("Should update weather data from external API")
    void shouldUpdateWeatherDataFromExternalAPI() {
        // Given
        String inseeCode = "75056";
        Double[] coordinates = {2.352222, 48.856614}; // [longitude, latitude]

        OpenMeteoCurrentResponse.CurrentWeather currentWeather = new OpenMeteoCurrentResponse.CurrentWeather(
            null,    // time
            15.8,    // temperature
            68,      // humidity
            18.0,    // apparentTemperature
            1,       // weatherCode
            12.5,    // windSpeed
            240,     // windDirection
            null,    // windGusts
            null,    // precipitation
            null,    // rain
            null,    // showers
            null,    // snowfall
            null,    // cloudCover
            null,    // pressureMsl
            null     // visibility
        );
        OpenMeteoCurrentResponse apiResponse = new OpenMeteoCurrentResponse("Europe/Paris", currentWeather);

        when(communeRepository.findByInseeCode(inseeCode))
            .thenReturn(Optional.of(parisCommune));
        when(openMeteoApiClient.getCurrentWeatherByCoordinates(coordinates))
            .thenReturn(Mono.just(apiResponse));
        when(weatherDataRepository.save(any(WeatherData.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Create minimal WeatherService for testing
        WeatherService weatherService = new WeatherService(
            openMeteoApiClient,
            null, // inseeApiClient not needed for this test
            weatherDataRepository,
            communeRepository
        );

        // When
        Mono<WeatherData> result = weatherService.updateWeatherForCommune(inseeCode);

        // Then
        StepVerifier.create(result)
            .expectNextMatches(weatherData -> {
                assertEquals(parisCommune.getInseeCode(), weatherData.getCommune().getInseeCode());
                assertEquals(15.8, weatherData.getTemperature());
                assertEquals(68, weatherData.getHumidity());
                assertEquals(12.5, weatherData.getWindSpeed());
                assertEquals(240, weatherData.getWindDirection());
                assertEquals(1, weatherData.getWeatherCode());
                assertEquals(LocalDate.now(), weatherData.getMeasurementDate());
                return true;
            })
            .verifyComplete();

        verify(openMeteoApiClient).getCurrentWeatherByCoordinates(coordinates);
        verify(weatherDataRepository).save(any(WeatherData.class));
    }

    /**
     * Test 2: Null safety for missing data fields
     * Verifies that the service handles null values gracefully
     */
    @Test
    @DisplayName("Should handle null values in weather data gracefully")
    void shouldHandleNullWeatherDataGracefully() {
        // Given
        String inseeCode = "75056";
        OpenMeteoCurrentResponse.CurrentWeather nullWeather = new OpenMeteoCurrentResponse.CurrentWeather(
            null,    // time
            null,    // temperature
            null,    // humidity
            null,    // apparentTemperature
            null,    // weatherCode
            null,    // windSpeed
            null,    // windDirection
            null,    // windGusts
            null,    // precipitation
            null,    // rain
            null,    // showers
            null,    // snowfall
            null,    // cloudCover
            null,    // pressureMsl
            null     // visibility
        );
        OpenMeteoCurrentResponse apiResponse = new OpenMeteoCurrentResponse("Europe/Paris", nullWeather);

        when(communeRepository.findByInseeCode(inseeCode))
            .thenReturn(Optional.of(parisCommune));
        when(openMeteoApiClient.getCurrentWeatherByCoordinates(any()))
            .thenReturn(Mono.just(apiResponse));
        when(weatherDataRepository.save(any(WeatherData.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WeatherService weatherService = new WeatherService(
            openMeteoApiClient,
            null,
            weatherDataRepository,
            communeRepository
        );

        // When
        Mono<WeatherData> result = weatherService.updateWeatherForCommune(inseeCode);

        // Then
        StepVerifier.create(result)
            .expectNextMatches(weatherData -> {
                // Should use default values for null fields
                assertEquals(0.0, weatherData.getTemperature());
                assertEquals(0, weatherData.getHumidity());
                assertEquals(0.0, weatherData.getWindSpeed());
                assertEquals(0, weatherData.getWindDirection());
                assertEquals(0, weatherData.getWeatherCode());
                return true;
            })
            .verifyComplete();
    }

    /**
     * Test 3: Update weather error handling
     * Verifies proper error handling when external API fails
     */
    @Test
    @DisplayName("Should handle external API errors gracefully")
    void shouldHandleExternalAPIErrors() {
        // Given
        String inseeCode = "75056";
        when(communeRepository.findByInseeCode(inseeCode))
            .thenReturn(Optional.of(parisCommune));
        when(openMeteoApiClient.getCurrentWeatherByCoordinates(any()))
            .thenReturn(Mono.error(new RuntimeException("External API unavailable")));

        WeatherService weatherService = new WeatherService(
            openMeteoApiClient,
            null,
            weatherDataRepository,
            communeRepository
        );

        // When
        Mono<WeatherData> result = weatherService.updateWeatherForCommune(inseeCode);

        // Then
        StepVerifier.create(result)
            .expectError(RuntimeException.class)
            .verify();

        verify(weatherDataRepository, never()).save(any());
    }

    /**
     * Test 4: Commune without coordinates handling
     * Verifies error handling when commune has missing coordinate data
     */
    @Test
    @DisplayName("Should throw exception when commune has no coordinates")
    void shouldThrowExceptionWhenCommuneHasNoCoordinates() {
        // Given
        String inseeCode = "12345";
        Commune communeWithoutCoords = new Commune();
        communeWithoutCoords.setInseeCode(inseeCode);
        communeWithoutCoords.setName("Test Commune");
        // No latitude/longitude set

        when(communeRepository.findByInseeCode(inseeCode))
            .thenReturn(Optional.of(communeWithoutCoords));

        WeatherService weatherService = new WeatherService(
            openMeteoApiClient,
            null,
            weatherDataRepository,
            communeRepository
        );

        // When
        Mono<WeatherData> result = weatherService.updateWeatherForCommune(inseeCode);

        // Then
        StepVerifier.create(result)
            .expectError(IllegalStateException.class)
            .verify();

        verify(openMeteoApiClient, never()).getCurrentWeatherByCoordinates(any());
    }

    /**
     * Test 5: Commune not found handling
     * Verifies error handling when commune doesn't exist
     */
    @Test
    @DisplayName("Should throw ResourceNotFoundException when commune not found")
    void shouldThrowExceptionWhenCommuneNotFound() {
        // Given
        String inseeCode = "99999";
        when(communeRepository.findByInseeCode(inseeCode))
            .thenReturn(Optional.empty());

        WeatherService weatherService = new WeatherService(
            openMeteoApiClient,
            null,
            weatherDataRepository,
            communeRepository
        );

        // When
        Mono<WeatherData> result = weatherService.updateWeatherForCommune(inseeCode);

        // Then
        StepVerifier.create(result)
            .expectError(fr.airsen.api.exception.ResourceNotFoundException.class)
            .verify();

        verify(openMeteoApiClient, never()).getCurrentWeatherByCoordinates(any());
        verify(weatherDataRepository, never()).save(any());
    }

    /**
     * Test 6: Verify all weather fields are correctly mapped from API response
     * Tests that advanced weather fields (apparentTemperature, precipitation, etc.) are properly saved
     */
    @Test
    @DisplayName("Should correctly map all weather fields including advanced fields")
    void shouldMapAllWeatherFieldsCorrectly() {
        // Given
        String inseeCode = "75056";
        Double[] coordinates = {2.352222, 48.856614};

        OpenMeteoCurrentResponse.CurrentWeather currentWeather = new OpenMeteoCurrentResponse.CurrentWeather(
            null,       // time
            15.8,       // temperature
            68,         // humidity
            18.0,       // apparentTemperature
            1,          // weatherCode
            12.5,       // windSpeed
            240,        // windDirection
            20.5,       // windGusts
            2.5,        // precipitation
            1.8,        // rain
            0.7,        // showers
            0.0,        // snowfall
            75,         // cloudCover
            1013.2,     // pressureMsl
            10000.0     // visibility
        );
        OpenMeteoCurrentResponse apiResponse = new OpenMeteoCurrentResponse("Europe/Paris", currentWeather);

        when(communeRepository.findByInseeCode(inseeCode))
            .thenReturn(Optional.of(parisCommune));
        when(openMeteoApiClient.getCurrentWeatherByCoordinates(coordinates))
            .thenReturn(Mono.just(apiResponse));
        when(weatherDataRepository.save(any(WeatherData.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        WeatherService weatherService = new WeatherService(
            openMeteoApiClient,
            null,
            weatherDataRepository,
            communeRepository
        );

        // When
        Mono<WeatherData> result = weatherService.updateWeatherForCommune(inseeCode);

        // Then
        StepVerifier.create(result)
            .expectNextMatches(weatherData -> {
                // Basic fields
                assertEquals(15.8, weatherData.getTemperature());
                assertEquals(68, weatherData.getHumidity());
                assertEquals(12.5, weatherData.getWindSpeed());
                assertEquals(240, weatherData.getWindDirection());
                assertEquals(1, weatherData.getWeatherCode());

                // Advanced fields
                assertEquals(18.0, weatherData.getApparentTemperature());
                assertEquals(2.5, weatherData.getPrecipitation());
                assertEquals(1.8, weatherData.getRain());
                assertEquals(0.7, weatherData.getShowers());
                assertEquals(0.0, weatherData.getSnowfall());
                assertEquals(75, weatherData.getCloudCover());
                assertEquals(20.5, weatherData.getWindGusts());
                assertEquals(1013.2, weatherData.getPressureMsl());

                return true;
            })
            .verifyComplete();
    }
}
