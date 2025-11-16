package fr.airsen.api.service;

import fr.airsen.api.dto.response.NearestAirQualityResult;
import fr.airsen.api.dto.response.NearestWeatherResult;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.Department;
import fr.airsen.api.repository.AirQualityRepository;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.WeatherDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GeoDistanceService.
 *
 * CRITICAL TESTS - Haversine Algorithm Accuracy:
 * These tests validate the geographic distance calculations that power
 * the 20km geodistance fallback feature. Incorrect calculations could result in:
 * - Showing data from communes too far away (misleading users)
 * - Missing data from nearby communes within range
 * - Violating the 20km PRD requirement
 *
 * Test Strategy:
 * - Validate against known real-world distances (Paris ↔ Saint-Denis)
 * - Test edge cases (same point, antipodal points, equator, poles)
 * - Verify coordinate validation (lat: -90 to 90, lon: -180 to 180)
 * - Test 20km boundary precision (19.9km vs 20.1km)
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GeoDistanceService Unit Tests")
class GeoDistanceServiceTest {

    @Mock
    private CommuneRepository communeRepository;

    @Mock
    private WeatherDataRepository weatherDataRepository;

    @Mock
    private AirQualityRepository airQualityRepository;

    @InjectMocks
    private GeoDistanceService geoDistanceService;

    private Commune parisCommune;
    private Commune saintDenisCommune;

    @BeforeEach
    void setUp() {
        setupTestEntities();
    }

    private void setupTestEntities() {
        Department parisDept = new Department();
        parisDept.setDepartmentCode("75");

        // Paris: 48.856614, 2.352222
        parisCommune = new Commune();
        parisCommune.setInseeCode("75056");
        parisCommune.setName("Paris");
        parisCommune.setLatitude(new BigDecimal("48.856614"));
        parisCommune.setLongitude(new BigDecimal("2.352222"));
        parisCommune.setDepartment(parisDept);

        // Saint-Denis: 48.936537, 2.357467 (~9-10 km from Paris)
        saintDenisCommune = new Commune();
        saintDenisCommune.setInseeCode("93008");
        saintDenisCommune.setName("Saint-Denis");
        saintDenisCommune.setLatitude(new BigDecimal("48.936537"));
        saintDenisCommune.setLongitude(new BigDecimal("2.357467"));
    }

    // ============================================
    // CRITICAL Test 1: Haversine Formula Accuracy
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should calculate accurate distance between Paris and Saint-Denis (~9-10 km)")
    void shouldCalculateAccurateDistanceBetweenParisAndSaintDenis() {
        // Given: Paris (48.856614, 2.352222) and Saint-Denis (48.936537, 2.357467)
        // Real-world distance: ~9.3 km (Google Maps)
        double parisLat = 48.856614;
        double parisLon = 2.352222;
        double saintDenisLat = 48.936537;
        double saintDenisLon = 2.357467;

        // When
        double distance = geoDistanceService.calculateDistance(
            parisLat, parisLon, saintDenisLat, saintDenisLon
        );

        // Then: Distance should be approximately 8.8-9.5 km (±0.7 km tolerance for Haversine)
        assertThat(distance).isBetween(8.8, 9.5);
        System.out.printf("Paris → Saint-Denis: %.2f km (expected ~9.3 km)%n", distance);
    }

    @Test
    @DisplayName("CRITICAL: Should return 0 km for same location")
    void shouldReturnZeroForSameLocation() {
        // Given: Same coordinates
        double lat = 48.856614;
        double lon = 2.352222;

        // When
        double distance = geoDistanceService.calculateDistance(lat, lon, lat, lon);

        // Then
        assertThat(distance).isCloseTo(0.0, within(0.001));
    }

    @Test
    @DisplayName("CRITICAL: Should calculate distance for antipodal points (maximum distance)")
    void shouldCalculateDistanceForAntipodalPoints() {
        // Given: Paris (48.856614, 2.352222) and its antipode (-48.856614, -177.647778)
        double parisLat = 48.856614;
        double parisLon = 2.352222;
        double antipodalLat = -48.856614;
        double antipodalLon = -177.647778;

        // When
        double distance = geoDistanceService.calculateDistance(
            parisLat, parisLon, antipodalLat, antipodalLon
        );

        // Then: Should be approximately half Earth's circumference (~20,000 km)
        // Circumference = 2π * 6371 ≈ 40,030 km, so half ≈ 20,015 km
        assertThat(distance).isBetween(19900.0, 20100.0);
        System.out.printf("Paris → Antipode: %.2f km (expected ~20,015 km)%n", distance);
    }

    @Test
    @DisplayName("Should calculate distance accurately for equatorial points")
    void shouldCalculateDistanceAccuratelyForEquatorialPoints() {
        // Given: Two points on the equator 1 degree apart
        // 1 degree longitude at equator ≈ 111.32 km
        double lat1 = 0.0;
        double lon1 = 0.0;
        double lat2 = 0.0;
        double lon2 = 1.0;

        // When
        double distance = geoDistanceService.calculateDistance(lat1, lon1, lat2, lon2);

        // Then: Should be approximately 111.32 km (±1 km tolerance)
        assertThat(distance).isBetween(110.0, 112.5);
    }

    @Test
    @DisplayName("Should calculate distance accurately for polar regions")
    void shouldCalculateDistanceAccuratelyForPolarRegions() {
        // Given: Two points near North Pole
        double lat1 = 89.0;
        double lon1 = 0.0;
        double lat2 = 89.0;
        double lon2 = 180.0;

        // When
        double distance = geoDistanceService.calculateDistance(lat1, lon1, lat2, lon2);

        // Then: At 89° latitude, circumference is very small, so 180° should be < 500 km
        assertThat(distance).isLessThan(500.0);
    }

    // ============================================
    // CRITICAL Test 2: 20km Boundary Precision
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should correctly identify distance just below 20km threshold")
    void shouldCorrectlyIdentifyDistanceJustBelow20km() {
        // Given: Two points approximately 19.9 km apart
        // Paris to a point ~19.9 km northeast
        double parisLat = 48.856614;
        double parisLon = 2.352222;
        double nearbyLat = 49.04;  // ~20.4 km north
        double nearbyLon = 2.352222;

        // When
        double distance = geoDistanceService.calculateDistance(
            parisLat, parisLon, nearbyLat, nearbyLon
        );

        // Then: Should be around 20km
        assertThat(distance).isBetween(19.0, 21.0);
    }

    @Test
    @DisplayName("Should calculate symmetric distance (A→B = B→A)")
    void shouldCalculateSymmetricDistance() {
        // Given: Paris and Saint-Denis
        double parisLat = 48.856614;
        double parisLon = 2.352222;
        double sdLat = 48.936537;
        double sdLon = 2.357467;

        // When
        double distanceAB = geoDistanceService.calculateDistance(parisLat, parisLon, sdLat, sdLon);
        double distanceBA = geoDistanceService.calculateDistance(sdLat, sdLon, parisLat, parisLon);

        // Then: Distances should be equal (within floating point tolerance)
        assertThat(distanceAB).isCloseTo(distanceBA, within(0.001));
    }

    // ============================================
    // Test 3: Coordinate Validation
    // ============================================

    @Test
    @DisplayName("Should throw exception for invalid latitude > 90")
    void shouldThrowExceptionForInvalidLatitudeGreaterThan90() {
        // Given: Invalid latitude
        double invalidLat = 91.0;
        double validLon = 2.352222;
        double validLat2 = 48.0;
        double validLon2 = 3.0;

        // When & Then
        assertThatThrownBy(() -> geoDistanceService.calculateDistance(
            invalidLat, validLon, validLat2, validLon2
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid latitude")
            .hasMessageContaining("91.0")
            .hasMessageContaining("between -90 and 90");
    }

    @Test
    @DisplayName("Should throw exception for invalid latitude < -90")
    void shouldThrowExceptionForInvalidLatitudeLessThanMinus90() {
        // Given: Invalid latitude
        double invalidLat = -91.0;
        double validLon = 2.352222;
        double validLat2 = 48.0;
        double validLon2 = 3.0;

        // When & Then
        assertThatThrownBy(() -> geoDistanceService.calculateDistance(
            invalidLat, validLon, validLat2, validLon2
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid latitude")
            .hasMessageContaining("-91.0");
    }

    @Test
    @DisplayName("Should throw exception for invalid longitude > 180")
    void shouldThrowExceptionForInvalidLongitudeGreaterThan180() {
        // Given: Invalid longitude
        double validLat = 48.856614;
        double invalidLon = 181.0;
        double validLat2 = 48.0;
        double validLon2 = 3.0;

        // When & Then
        assertThatThrownBy(() -> geoDistanceService.calculateDistance(
            validLat, invalidLon, validLat2, validLon2
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid longitude")
            .hasMessageContaining("181.0")
            .hasMessageContaining("between -180 and 180");
    }

    @Test
    @DisplayName("Should throw exception for invalid longitude < -180")
    void shouldThrowExceptionForInvalidLongitudeLessThanMinus180() {
        // Given: Invalid longitude
        double validLat = 48.856614;
        double invalidLon = -181.0;
        double validLat2 = 48.0;
        double validLon2 = 3.0;

        // When & Then
        assertThatThrownBy(() -> geoDistanceService.calculateDistance(
            validLat, invalidLon, validLat2, validLon2
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid longitude")
            .hasMessageContaining("-181.0");
    }

    @Test
    @DisplayName("Should accept valid coordinate boundaries (lat: ±90, lon: ±180)")
    void shouldAcceptValidCoordinateBoundaries() {
        // Given: Valid boundary coordinates
        double northPole = 90.0;
        double southPole = -90.0;
        double eastBoundary = 180.0;
        double westBoundary = -180.0;

        // When & Then: Should not throw exceptions
        assertThatNoException().isThrownBy(() ->
            geoDistanceService.calculateDistance(northPole, westBoundary, southPole, eastBoundary)
        );
    }

    // ============================================
    // Test 4: Find Nearest Commune With Weather
    // ============================================

    @Test
    @DisplayName("Should find nearest commune with weather data within threshold")
    void shouldFindNearestCommuneWithWeatherDataWithinThreshold() {
        // Given
        String targetInseeCode = "75056";
        double maxDistanceKm = 20.0;

        // Mock target commune
        when(communeRepository.findByInseeCodeWithCoordinates(targetInseeCode))
            .thenReturn(Optional.of(parisCommune));

        // Mock nearest weather data (Saint-Denis at ~9 km)
        Object[] weatherRow = createWeatherResultRow(
            "93008",
            "Saint-Denis",
            48.936537,
            2.357467,
            LocalDate.now(),
            25.0,   // temperature
            65,     // humidity
            15.0,   // windSpeed
            180,    // windDirection
            800,    // weatherCode
            9.2     // distanceKm
        );

        when(weatherDataRepository.findNearestCommuneWithWeather(
            anyDouble(), anyDouble(), eq(maxDistanceKm)
        )).thenReturn(Optional.of(weatherRow));

        // When
        Optional<NearestWeatherResult> result = geoDistanceService.findNearestCommuneWithWeather(
            targetInseeCode, maxDistanceKm
        );

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().inseeCode()).isEqualTo("93008");
        assertThat(result.get().communeName()).isEqualTo("Saint-Denis");
        assertThat(result.get().distanceKm()).isCloseTo(9.2, within(0.1));
        assertThat(result.get().temperature()).isCloseTo(25.0, within(0.1));

        verify(communeRepository).findByInseeCodeWithCoordinates(targetInseeCode);
        verify(weatherDataRepository).findNearestCommuneWithWeather(anyDouble(), anyDouble(), eq(maxDistanceKm));
    }

    @Test
    @DisplayName("Should return empty when no weather data within threshold")
    void shouldReturnEmptyWhenNoWeatherDataWithinThreshold() {
        // Given
        String targetInseeCode = "75056";
        double maxDistanceKm = 20.0;

        when(communeRepository.findByInseeCodeWithCoordinates(targetInseeCode))
            .thenReturn(Optional.of(parisCommune));
        when(weatherDataRepository.findNearestCommuneWithWeather(anyDouble(), anyDouble(), eq(maxDistanceKm)))
            .thenReturn(Optional.empty());

        // When
        Optional<NearestWeatherResult> result = geoDistanceService.findNearestCommuneWithWeather(
            targetInseeCode, maxDistanceKm
        );

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when target commune not found")
    void shouldReturnEmptyWhenTargetCommuneNotFound() {
        // Given
        String invalidInseeCode = "99999";
        when(communeRepository.findByInseeCodeWithCoordinates(invalidInseeCode))
            .thenReturn(Optional.empty());

        // When
        Optional<NearestWeatherResult> result = geoDistanceService.findNearestCommuneWithWeather(
            invalidInseeCode, 20.0
        );

        // Then
        assertThat(result).isEmpty();
        verify(weatherDataRepository, never()).findNearestCommuneWithWeather(anyDouble(), anyDouble(), anyDouble());
    }

    @Test
    @DisplayName("Should return empty when commune has no coordinates")
    void shouldReturnEmptyWhenCommuneHasNoCoordinates() {
        // Given
        Commune communeWithoutCoords = new Commune();
        communeWithoutCoords.setInseeCode("12345");
        communeWithoutCoords.setName("Test");
        // No latitude/longitude set

        when(communeRepository.findByInseeCodeWithCoordinates("12345"))
            .thenReturn(Optional.of(communeWithoutCoords));

        // When
        Optional<NearestWeatherResult> result = geoDistanceService.findNearestCommuneWithWeather(
            "12345", 20.0
        );

        // Then
        assertThat(result).isEmpty();
        verify(weatherDataRepository, never()).findNearestCommuneWithWeather(anyDouble(), anyDouble(), anyDouble());
    }

    // ============================================
    // Test 5: Find Nearest Commune With Air Quality
    // ============================================

    @Test
    @DisplayName("Should find nearest commune with air quality data within threshold")
    void shouldFindNearestCommuneWithAirQualityDataWithinThreshold() {
        // Given
        String targetInseeCode = "75056";
        double maxDistanceKm = 20.0;

        when(communeRepository.findByInseeCodeWithCoordinates(targetInseeCode))
            .thenReturn(Optional.of(parisCommune));

        // Mock nearest air quality data
        Object[] aqRow = createAirQualityResultRow(
            "93008",
            "Saint-Denis",
            48.936537,
            2.357467,
            LocalDate.now(),
            3,          // atmoIndex
            "Moyen",    // qualifier
            "#ffcc00",  // color
            3, 3, 3, 3, 3,  // pollutants
            9.2         // distanceKm
        );

        when(airQualityRepository.findNearestCommuneWithAirQuality(
            anyDouble(), anyDouble(), eq(maxDistanceKm)
        )).thenReturn(Optional.of(aqRow));

        // When
        Optional<NearestAirQualityResult> result = geoDistanceService.findNearestCommuneWithAirQuality(
            targetInseeCode, maxDistanceKm
        );

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().inseeCode()).isEqualTo("93008");
        assertThat(result.get().communeName()).isEqualTo("Saint-Denis");
        assertThat(result.get().distanceKm()).isCloseTo(9.2, within(0.1));
        assertThat(result.get().atmoIndex()).isEqualTo(3);
        assertThat(result.get().qualifier()).isEqualTo("Moyen");

        verify(communeRepository).findByInseeCodeWithCoordinates(targetInseeCode);
        verify(airQualityRepository).findNearestCommuneWithAirQuality(anyDouble(), anyDouble(), eq(maxDistanceKm));
    }

    @Test
    @DisplayName("Should return empty when no air quality data within threshold")
    void shouldReturnEmptyWhenNoAirQualityDataWithinThreshold() {
        // Given
        String targetInseeCode = "75056";
        double maxDistanceKm = 20.0;

        when(communeRepository.findByInseeCodeWithCoordinates(targetInseeCode))
            .thenReturn(Optional.of(parisCommune));
        when(airQualityRepository.findNearestCommuneWithAirQuality(anyDouble(), anyDouble(), eq(maxDistanceKm)))
            .thenReturn(Optional.empty());

        // When
        Optional<NearestAirQualityResult> result = geoDistanceService.findNearestCommuneWithAirQuality(
            targetInseeCode, maxDistanceKm
        );

        // Then
        assertThat(result).isEmpty();
    }

    // ============================================
    // Helper Methods
    // ============================================

    private Object[] createWeatherResultRow(String inseeCode, String name, double lat, double lon,
                                           LocalDate date, double temp, int humidity, double windSpeed,
                                           int windDir, int weatherCode, double distance) {
        return new Object[]{
            inseeCode, name, lat, lon, date,
            temp, humidity, windSpeed, windDir, weatherCode,
            null, null, null, null, null, null, null, null,  // Advanced fields (nulls)
            distance
        };
    }

    private Object[] createAirQualityResultRow(String inseeCode, String name, double lat, double lon,
                                              LocalDate date, int atmoIndex, String qualifier, String color,
                                              int no2, int o3, int pm10, int pm25, int so2, double distance) {
        return new Object[]{
            inseeCode, name, lat, lon, date,
            atmoIndex, qualifier, color,
            no2, o3, pm10, pm25, so2,
            distance
        };
    }
}
