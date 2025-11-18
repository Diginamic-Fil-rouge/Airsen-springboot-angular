package fr.airsen.api.mapper;

import fr.airsen.api.dto.response.NearestWeatherResult;
import fr.airsen.api.dto.response.WeatherResponse;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.Department;
import fr.airsen.api.entity.WeatherData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for WeatherMapper.
 *
 * CRITICAL TESTS - Weather Data Transformation Accuracy:
 * These tests ensure weather data is correctly mapped between entities and DTOs,
 * including all 13 weather fields (basic 5 + advanced 8).
 *
 * Critical validations:
 * - All 13 weather fields are mapped correctly
 * - Weather code descriptions are accurate
 * - DIRECT vs ESTIMATED data source attribution
 * - Commune attribution (requested vs source)
 */
@DisplayName("WeatherMapper Unit Tests")
class WeatherMapperTest {

    private WeatherMapper mapper;

    private Commune parisCommune;
    private Commune saintDenisCommune;
    private WeatherData parisWeatherData;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(WeatherMapper.class);
        setupTestEntities();
    }

    private void setupTestEntities() {
        Department parisDept = new Department();
        parisDept.setDepartmentCode("75");
        parisDept.setName("Paris");

        // Paris commune
        parisCommune = new Commune();
        parisCommune.setInseeCode("75056");
        parisCommune.setName("Paris");
        parisCommune.setLatitude(new BigDecimal("48.856614"));
        parisCommune.setLongitude(new BigDecimal("2.352222"));
        parisCommune.setDepartment(parisDept);

        // Saint-Denis commune
        saintDenisCommune = new Commune();
        saintDenisCommune.setInseeCode("93008");
        saintDenisCommune.setName("Saint-Denis");

        // Paris weather data with ALL 13 fields
        parisWeatherData = new WeatherData();
        parisWeatherData.setCommune(parisCommune);
        parisWeatherData.setMeasurementDate(LocalDate.of(2025, 11, 16));
        // Basic fields (5)
        parisWeatherData.setTemperature(18.5);
        parisWeatherData.setHumidity(65);
        parisWeatherData.setWindSpeed(12.5);
        parisWeatherData.setWindDirection(180);
        parisWeatherData.setWeatherCode(2); // Partly cloudy
        // Advanced fields (8)
        parisWeatherData.setApparentTemperature(17.2);
        parisWeatherData.setPrecipitation(2.5);
        parisWeatherData.setRain(2.0);
        parisWeatherData.setShowers(0.5);
        parisWeatherData.setSnowfall(0.0);
        parisWeatherData.setCloudCover(50);
        parisWeatherData.setWindGusts(25.0);
        parisWeatherData.setPressureMsl(1013.25);
    }

    // ============================================
    // CRITICAL Test 1: Direct Response Mapping (All 13 Fields)
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should map all 13 weather fields from entity to DIRECT response")
    void shouldMapAll13WeatherFieldsFromEntityToDirectResponse() {
        // When
        WeatherResponse response = mapper.toDirectResponse(parisWeatherData);

        // Then
        assertThat(response).isNotNull();

        // Verify commune information
        assertThat(response.inseeCode()).isEqualTo("75056");
        assertThat(response.communeName()).isEqualTo("Paris");
        assertThat(response.measurementDate()).isEqualTo(LocalDate.of(2025, 11, 16));

        // Verify basic weather fields (5)
        assertThat(response.temperature()).isCloseTo(18.5, within(0.1));
        assertThat(response.humidity()).isEqualTo(65);
        assertThat(response.windSpeed()).isCloseTo(12.5, within(0.1));
        assertThat(response.windDirection()).isEqualTo(180);
        assertThat(response.weatherCode()).isEqualTo(2);
        assertThat(response.weatherDescription()).isEqualTo("Partly cloudy");

        // Verify advanced weather fields (8)
        assertThat(response.apparentTemperature()).isCloseTo(17.2, within(0.1));
        assertThat(response.precipitation()).isCloseTo(2.5, within(0.1));
        assertThat(response.rain()).isCloseTo(2.0, within(0.1));
        assertThat(response.showers()).isCloseTo(0.5, within(0.1));
        assertThat(response.snowfall()).isCloseTo(0.0, within(0.1));
        assertThat(response.cloudCover()).isEqualTo(50);
        assertThat(response.windGusts()).isCloseTo(25.0, within(0.1));
        assertThat(response.pressureMsl()).isCloseTo(1013.25, within(0.01));

        // Verify data source metadata
        assertThat(response.dataSource()).isEqualTo(WeatherResponse.DataSource.DIRECT);
        assertThat(response.estimatedFromCommune()).isNull();
        assertThat(response.distanceKm()).isNull();
        assertThat(response.dataQualityNote()).isEqualTo("Données mesurées pour cette commune");
    }

    @Test
    @DisplayName("Should handle null advanced weather fields gracefully")
    void shouldHandleNullAdvancedWeatherFieldsGracefully() {
        // Given: Weather data with only basic fields
        WeatherData basicWeather = new WeatherData();
        basicWeather.setCommune(parisCommune);
        basicWeather.setMeasurementDate(LocalDate.now());
        basicWeather.setTemperature(20.0);
        basicWeather.setHumidity(70);
        basicWeather.setWindSpeed(10.0);
        basicWeather.setWindDirection(90);
        basicWeather.setWeatherCode(0);
        // All advanced fields are null

        // When
        WeatherResponse response = mapper.toDirectResponse(basicWeather);

        // Then: Should map without errors
        assertThat(response).isNotNull();
        assertThat(response.temperature()).isCloseTo(20.0, within(0.1));
        assertThat(response.apparentTemperature()).isNull();
        assertThat(response.precipitation()).isNull();
        assertThat(response.rain()).isNull();
        assertThat(response.showers()).isNull();
        assertThat(response.snowfall()).isNull();
        assertThat(response.cloudCover()).isNull();
        assertThat(response.windGusts()).isNull();
        assertThat(response.pressureMsl()).isNull();
    }

    // ============================================
    // CRITICAL Test 2: Estimated Response Mapping
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should map NearestWeatherResult to ESTIMATED response correctly")
    void shouldMapNearestWeatherResultToEstimatedResponse() {
        // Given: Weather data from Saint-Denis (nearest commune)
        NearestWeatherResult nearestResult = new NearestWeatherResult(
            "93008",                    // inseeCode (source)
            "Saint-Denis",              // communeName (source)
            48.936537,                  // latitude
            2.357467,                   // longitude
            LocalDate.of(2025, 11, 16), // measurementDate
            // Basic fields
            22.5,                       // temperature
            60,                         // humidity
            15.0,                       // windSpeed
            270,                        // windDirection
            61,                         // weatherCode (Rain)
            // Advanced fields
            21.0,                       // apparentTemperature
            5.0,                        // precipitation
            4.5,                        // rain
            0.5,                        // showers
            0.0,                        // snowfall
            75,                         // cloudCover
            30.0,                       // windGusts
            1010.5,                     // pressureMsl
            9.2                         // distanceKm
        );

        // When: Map to response for requested commune (Paris)
        WeatherResponse response = mapper.toEstimatedResponse(nearestResult, parisCommune);

        // Then
        assertThat(response).isNotNull();

        // Verify requested commune information (NOT source commune)
        assertThat(response.inseeCode()).isEqualTo("75056");  // Paris
        assertThat(response.communeName()).isEqualTo("Paris"); // Paris

        // Verify weather data FROM nearest commune
        assertThat(response.temperature()).isCloseTo(22.5, within(0.1));
        assertThat(response.humidity()).isEqualTo(60);
        assertThat(response.windSpeed()).isCloseTo(15.0, within(0.1));
        assertThat(response.windDirection()).isEqualTo(270);
        assertThat(response.weatherCode()).isEqualTo(61);
        assertThat(response.weatherDescription()).isEqualTo("Rain");

        // Verify advanced fields FROM nearest commune
        assertThat(response.apparentTemperature()).isCloseTo(21.0, within(0.1));
        assertThat(response.precipitation()).isCloseTo(5.0, within(0.1));
        assertThat(response.rain()).isCloseTo(4.5, within(0.1));
        assertThat(response.showers()).isCloseTo(0.5, within(0.1));
        assertThat(response.snowfall()).isCloseTo(0.0, within(0.1));
        assertThat(response.cloudCover()).isEqualTo(75);
        assertThat(response.windGusts()).isCloseTo(30.0, within(0.1));
        assertThat(response.pressureMsl()).isCloseTo(1010.5, within(0.01));

        // Verify data source metadata
        assertThat(response.dataSource()).isEqualTo(WeatherResponse.DataSource.ESTIMATED);
        assertThat(response.estimatedFromCommune()).isEqualTo("Saint-Denis");
        assertThat(response.distanceKm()).isEqualTo(9.2);
        assertThat(response.dataQualityNote())
            .isEqualTo("Données estimées depuis Saint-Denis (9,2 km)");
    }

    // ============================================
    // CRITICAL Test 3: Weather Code Descriptions
    // ============================================

    @ParameterizedTest
    @CsvSource({
        "0, Clear sky",
        "1, Mainly clear",
        "2, Partly cloudy",
        "3, Overcast",
        "45, Foggy",
        "48, Foggy",
        "51, Drizzle",
        "53, Drizzle",
        "55, Drizzle",
        "61, Rain",
        "63, Rain",
        "65, Rain",
        "71, Snow",
        "73, Snow",
        "75, Snow",
        "77, Snow grains",
        "80, Rain showers",
        "81, Rain showers",
        "82, Rain showers",
        "85, Snow showers",
        "86, Snow showers",
        "95, Thunderstorm",
        "96, Thunderstorm with hail",
        "99, Thunderstorm with hail"
    })
    @DisplayName("CRITICAL: Should map weather codes to correct descriptions")
    void shouldMapWeatherCodesToCorrectDescriptions(int weatherCode, String expectedDescription) {
        // When
        String description = mapper.weatherCodeToDescription(weatherCode);

        // Then
        assertThat(description).isEqualTo(expectedDescription);
    }

    @Test
    @DisplayName("Should return 'Unknown' for null weather code")
    void shouldReturnUnknownForNullWeatherCode() {
        // When
        String description = mapper.weatherCodeToDescription(null);

        // Then
        assertThat(description).isEqualTo("Unknown");
    }

    @Test
    @DisplayName("Should return 'Weather code X' for unknown codes")
    void shouldReturnWeatherCodeXForUnknownCodes() {
        // Given: Invalid weather codes
        int unknownCode1 = 100;
        int unknownCode2 = 42;

        // When
        String description1 = mapper.weatherCodeToDescription(unknownCode1);
        String description2 = mapper.weatherCodeToDescription(unknownCode2);

        // Then
        assertThat(description1).isEqualTo("Weather code 100");
        assertThat(description2).isEqualTo("Weather code 42");
    }

    // ============================================
    // Test 4: Data Quality Note Formatting
    // ============================================

    @Test
    @DisplayName("Should format data quality note with correct distance precision")
    void shouldFormatDataQualityNoteWithCorrectDistancePrecision() {
        // Given: Different distances
        NearestWeatherResult result1 = createNearestWeatherResult("Aubervilliers", 3.5);
        NearestWeatherResult result2 = createNearestWeatherResult("Créteil", 14.8);
        NearestWeatherResult result3 = createNearestWeatherResult("Versailles", 19.9);

        // When
        WeatherResponse response1 = mapper.toEstimatedResponse(result1, parisCommune);
        WeatherResponse response2 = mapper.toEstimatedResponse(result2, parisCommune);
        WeatherResponse response3 = mapper.toEstimatedResponse(result3, parisCommune);

        // Then: Notes should show distances with 1 decimal place
        assertThat(response1.dataQualityNote()).isEqualTo("Données estimées depuis Aubervilliers (3,5 km)");
        assertThat(response2.dataQualityNote()).isEqualTo("Données estimées depuis Créteil (14,8 km)");
        assertThat(response3.dataQualityNote()).isEqualTo("Données estimées depuis Versailles (19,9 km)");
    }

    @Test
    @DisplayName("Should create different data quality notes for DIRECT vs ESTIMATED")
    void shouldCreateDifferentDataQualityNotesForDirectVsEstimated() {
        // Given
        NearestWeatherResult estimatedResult = createNearestWeatherResult("Saint-Denis", 9.2);

        // When
        WeatherResponse directResponse = mapper.toDirectResponse(parisWeatherData);
        WeatherResponse estimatedResponse = mapper.toEstimatedResponse(estimatedResult, parisCommune);

        // Then
        assertThat(directResponse.dataQualityNote())
            .isEqualTo("Données mesurées pour cette commune");
        assertThat(estimatedResponse.dataQualityNote())
            .contains("Données estimées depuis")
            .contains("Saint-Denis")
            .contains("9,2 km");
    }

    // ============================================
    // Test 5: Edge Cases
    // ============================================

    @Test
    @DisplayName("Should map weather data with extreme temperature values")
    void shouldMapWeatherDataWithExtremeTemperatureValues() {
        // Given: Extreme temperatures
        WeatherData extremeHot = new WeatherData();
        extremeHot.setCommune(parisCommune);
        extremeHot.setMeasurementDate(LocalDate.now());
        extremeHot.setTemperature(45.0);  // Extreme heat
        extremeHot.setHumidity(10);
        extremeHot.setWindSpeed(5.0);
        extremeHot.setWindDirection(0);
        extremeHot.setWeatherCode(0);  // Clear sky

        // When
        WeatherResponse response = mapper.toDirectResponse(extremeHot);

        // Then
        assertThat(response.temperature()).isCloseTo(45.0, within(0.1));
    }

    @Test
    @DisplayName("Should map weather data with snow conditions")
    void shouldMapWeatherDataWithSnowConditions() {
        // Given: Snowy weather
        WeatherData snowyWeather = new WeatherData();
        snowyWeather.setCommune(parisCommune);
        snowyWeather.setMeasurementDate(LocalDate.now());
        snowyWeather.setTemperature(-5.0);
        snowyWeather.setHumidity(90);
        snowyWeather.setWindSpeed(20.0);
        snowyWeather.setWindDirection(45);
        snowyWeather.setWeatherCode(71);  // Snow
        snowyWeather.setSnowfall(10.5);
        snowyWeather.setPrecipitation(10.5);

        // When
        WeatherResponse response = mapper.toDirectResponse(snowyWeather);

        // Then
        assertThat(response.weatherCode()).isEqualTo(71);
        assertThat(response.weatherDescription()).isEqualTo("Snow");
        assertThat(response.snowfall()).isCloseTo(10.5, within(0.1));
        assertThat(response.temperature()).isCloseTo(-5.0, within(0.1));
    }

    // ============================================
    // Helper Methods
    // ============================================

    private NearestWeatherResult createNearestWeatherResult(String communeName, double distanceKm) {
        return new NearestWeatherResult(
            "99999",
            communeName,
            48.0,
            2.0,
            LocalDate.now(),
            20.0,  // temperature
            65,    // humidity
            10.0,  // windSpeed
            180,   // windDirection
            2,     // weatherCode (Partly cloudy)
            18.5,  // apparentTemperature
            1.0,   // precipitation
            0.8,   // rain
            0.2,   // showers
            0.0,   // snowfall
            50,    // cloudCover
            15.0,  // windGusts
            1013.0, // pressureMsl
            distanceKm
        );
    }
}
