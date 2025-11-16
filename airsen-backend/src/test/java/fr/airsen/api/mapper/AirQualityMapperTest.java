package fr.airsen.api.mapper;

import fr.airsen.api.dto.response.AirQualityResponse;
import fr.airsen.api.dto.response.NearestAirQualityResult;
import fr.airsen.api.entity.AirQuality;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.Department;
import fr.airsen.api.entity.enums.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for AirQualityMapper.
 *
 * CRITICAL TESTS - Data Transformation Accuracy:
 * These tests ensure air quality data is correctly mapped between entities and DTOs.
 * Incorrect mapping could result in:
 * - Showing wrong air quality data to users (public health risk)
 * - Incorrect pollutant concentrations
 * - Wrong commune attribution (GDPR/accuracy issue)
 * - Missing data source indication (DIRECT vs ESTIMATED)
 */
@DisplayName("AirQualityMapper Unit Tests")
class AirQualityMapperTest {

    private AirQualityMapper mapper;

    private Commune parisCommune;
    private Commune saintDenisCommune;
    private AirQuality parisAirQuality;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(AirQualityMapper.class);
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
        saintDenisCommune.setLatitude(new BigDecimal("48.936537"));
        saintDenisCommune.setLongitude(new BigDecimal("2.357467"));

        // Paris air quality data
        parisAirQuality = new AirQuality();
        parisAirQuality.setCommune(parisCommune);
        parisAirQuality.setMeasurementDate(LocalDate.of(2025, 11, 16));
        parisAirQuality.setAtmIndex(3);
        parisAirQuality.setAtmoQual("Moyen");
        parisAirQuality.setAtmoColor("#ffcc00");
        parisAirQuality.setNo2Concentration(45);
        parisAirQuality.setO3Concentration(55);
        parisAirQuality.setPm10Concentration(30);
        parisAirQuality.setPm25Concentration(20);
        parisAirQuality.setSo2Concentration(10);
    }

    // ============================================
    // CRITICAL Test 1: Direct Response Mapping
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should map AirQuality entity to DIRECT response correctly")
    void shouldMapAirQualityEntityToDirectResponse() {
        // When
        AirQualityResponse response = mapper.toDirectResponse(parisAirQuality);

        // Then
        assertThat(response).isNotNull();

        // Verify commune information
        assertThat(response.inseeCode()).isEqualTo("75056");
        assertThat(response.communeName()).isEqualTo("Paris");

        // Verify air quality data
        assertThat(response.measurementDate()).isEqualTo(LocalDate.of(2025, 11, 16));
        assertThat(response.atmoIndex()).isEqualTo(3);
        assertThat(response.qualifier()).isEqualTo("Moyen");
        assertThat(response.color()).isEqualTo("#ffcc00");

        // Verify pollutants
        Map<String, Integer> pollutants = response.pollutants();
        assertThat(pollutants).isNotNull();
        assertThat(pollutants.get("NO2")).isEqualTo(45);
        assertThat(pollutants.get("O3")).isEqualTo(55);
        assertThat(pollutants.get("PM10")).isEqualTo(30);
        assertThat(pollutants.get("PM25")).isEqualTo(20);
        assertThat(pollutants.get("SO2")).isEqualTo(10);

        // Verify data source metadata
        assertThat(response.dataSource()).isEqualTo(DataSource.DIRECT);
        assertThat(response.estimatedFromCommune()).isNull();
        assertThat(response.distanceKm()).isNull();
        assertThat(response.dataQualityNote()).isEqualTo("Données mesurées pour cette commune");
    }

    @Test
    @DisplayName("Should map pollutants to HashMap with correct keys")
    void shouldMapPollutantsToHashMapWithCorrectKeys() {
        // When
        Map<String, Integer> pollutants = mapper.mapPollutants(parisAirQuality);

        // Then
        assertThat(pollutants).isNotNull();
        assertThat(pollutants).hasSize(5);
        assertThat(pollutants).containsOnlyKeys("NO2", "O3", "PM10", "PM25", "SO2");
    }

    @Test
    @DisplayName("Should handle null pollutant values gracefully")
    void shouldHandleNullPollutantValuesGracefully() {
        // Given: Air quality with some null pollutants
        AirQuality aqWithNulls = new AirQuality();
        aqWithNulls.setCommune(parisCommune);
        aqWithNulls.setMeasurementDate(LocalDate.now());
        aqWithNulls.setAtmIndex(2);
        aqWithNulls.setAtmoQual("Bon");
        aqWithNulls.setAtmoColor("#50ccaa");
        // NO2, O3, PM10, PM25, SO2 are all null

        // When
        Map<String, Integer> pollutants = mapper.mapPollutants(aqWithNulls);

        // Then
        assertThat(pollutants).isNotNull();
        assertThat(pollutants).hasSize(5);
        assertThat(pollutants.values()).containsOnly((Integer) null);
    }

    // ============================================
    // CRITICAL Test 2: Estimated Response Mapping
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should map NearestAirQualityResult to ESTIMATED response correctly")
    void shouldMapNearestAirQualityResultToEstimatedResponse() {
        // Given: Air quality data from Saint-Denis (nearest commune)
        NearestAirQualityResult nearestResult = new NearestAirQualityResult(
            "93008",                    // inseeCode (source)
            "Saint-Denis",              // communeName (source)
            48.936537,                  // latitude
            2.357467,                   // longitude
            LocalDate.of(2025, 11, 16), // measurementDate
            4,                          // atmoIndex (Bad)
            "Mauvais",                  // qualifier
            "#ff0000",                  // color
            60,                         // NO2
            70,                         // O3
            40,                         // PM10
            30,                         // PM25
            15,                         // SO2
            9.2                         // distanceKm
        );

        // When: Map to response for requested commune (Paris)
        AirQualityResponse response = mapper.toEstimatedResponse(nearestResult, parisCommune);

        // Then
        assertThat(response).isNotNull();

        // Verify requested commune information (NOT source commune)
        assertThat(response.inseeCode()).isEqualTo("75056");  // Paris
        assertThat(response.communeName()).isEqualTo("Paris"); // Paris

        // Verify air quality data FROM nearest commune
        assertThat(response.measurementDate()).isEqualTo(LocalDate.of(2025, 11, 16));
        assertThat(response.atmoIndex()).isEqualTo(4);
        assertThat(response.qualifier()).isEqualTo("Mauvais");
        assertThat(response.color()).isEqualTo("#ff0000");

        // Verify pollutants FROM nearest commune
        Map<String, Integer> pollutants = response.pollutants();
        assertThat(pollutants).isNotNull();
        assertThat(pollutants.get("NO2")).isEqualTo(60);
        assertThat(pollutants.get("O3")).isEqualTo(70);
        assertThat(pollutants.get("PM10")).isEqualTo(40);
        assertThat(pollutants.get("PM25")).isEqualTo(30);
        assertThat(pollutants.get("SO2")).isEqualTo(15);

        // Verify data source metadata
        assertThat(response.dataSource()).isEqualTo(DataSource.ESTIMATED);
        assertThat(response.estimatedFromCommune()).isEqualTo("Saint-Denis");
        assertThat(response.distanceKm()).isEqualTo(9.2);
        assertThat(response.dataQualityNote())
            .isEqualTo("Données estimées depuis Saint-Denis (9,2 km)");
    }

    @Test
    @DisplayName("CRITICAL: Should correctly attribute commune in estimated response")
    void shouldCorrectlyAttributeCommuneInEstimatedResponse() {
        // Given: Requesting data for Paris, but using Saint-Denis data
        NearestAirQualityResult saintDenisData = new NearestAirQualityResult(
            "93008", "Saint-Denis",
            48.936537, 2.357467,
            LocalDate.now(),
            5, "Très mauvais", "#960032",
            80, 90, 50, 40, 20,
            9.2
        );

        // When
        AirQualityResponse response = mapper.toEstimatedResponse(saintDenisData, parisCommune);

        // Then: Response should show Paris as the commune (requested)
        assertThat(response.inseeCode()).isEqualTo("75056");
        assertThat(response.communeName()).isEqualTo("Paris");

        // But metadata should indicate data is from Saint-Denis
        assertThat(response.dataSource()).isEqualTo(DataSource.ESTIMATED);
        assertThat(response.estimatedFromCommune()).isEqualTo("Saint-Denis");
        assertThat(response.distanceKm()).isCloseTo(9.2, within(0.1));
    }

    @Test
    @DisplayName("Should map pollutants from NearestAirQualityResult correctly")
    void shouldMapPollutantsFromNearestAirQualityResultCorrectly() {
        // Given
        NearestAirQualityResult result = new NearestAirQualityResult(
            "93008", "Saint-Denis",
            48.936537, 2.357467,
            LocalDate.now(),
            3, "Moyen", "#ffcc00",
            50, 60, 35, 25, 12,
            9.2
        );

        // When
        Map<String, Integer> pollutants = mapper.mapPollutants(result);

        // Then
        assertThat(pollutants).isNotNull();
        assertThat(pollutants).hasSize(5);
        assertThat(pollutants.get("NO2")).isEqualTo(50);
        assertThat(pollutants.get("O3")).isEqualTo(60);
        assertThat(pollutants.get("PM10")).isEqualTo(35);
        assertThat(pollutants.get("PM25")).isEqualTo(25);
        assertThat(pollutants.get("SO2")).isEqualTo(12);
    }

    // ============================================
    // Test 3: Data Quality Note Formatting
    // ============================================

    @Test
    @DisplayName("Should format data quality note with correct distance precision")
    void shouldFormatDataQualityNoteWithCorrectDistancePrecision() {
        // Given: Different distances
        NearestAirQualityResult result1 = createNearestResult("Aubervilliers", 3.5);
        NearestAirQualityResult result2 = createNearestResult("Créteil", 14.8);
        NearestAirQualityResult result3 = createNearestResult("Versailles", 19.9);

        // When
        AirQualityResponse response1 = mapper.toEstimatedResponse(result1, parisCommune);
        AirQualityResponse response2 = mapper.toEstimatedResponse(result2, parisCommune);
        AirQualityResponse response3 = mapper.toEstimatedResponse(result3, parisCommune);

        // Then: Notes should show distances with 1 decimal place
        assertThat(response1.dataQualityNote()).isEqualTo("Données estimées depuis Aubervilliers (3,5 km)");
        assertThat(response2.dataQualityNote()).isEqualTo("Données estimées depuis Créteil (14,8 km)");
        assertThat(response3.dataQualityNote()).isEqualTo("Données estimées depuis Versailles (19,9 km)");
    }

    @Test
    @DisplayName("Should create different data quality notes for DIRECT vs ESTIMATED")
    void shouldCreateDifferentDataQualityNotesForDirectVsEstimated() {
        // Given
        NearestAirQualityResult estimatedResult = createNearestResult("Saint-Denis", 9.2);

        // When
        AirQualityResponse directResponse = mapper.toDirectResponse(parisAirQuality);
        AirQualityResponse estimatedResponse = mapper.toEstimatedResponse(estimatedResult, parisCommune);

        // Then
        assertThat(directResponse.dataQualityNote())
            .isEqualTo("Données mesurées pour cette commune");
        assertThat(estimatedResponse.dataQualityNote())
            .contains("Données estimées depuis")
            .contains("Saint-Denis")
            .contains("9,2 km");
    }

    // ============================================
    // Test 4: All Pollutants Present
    // ============================================

    @Test
    @DisplayName("Should always include all 5 pollutant types in map")
    void shouldAlwaysIncludeAllFivePollutantTypesInMap() {
        // When
        Map<String, Integer> pollutants = mapper.mapPollutants(parisAirQuality);

        // Then: All 5 pollutants must be present
        assertThat(pollutants.keySet())
            .containsExactlyInAnyOrder("NO2", "O3", "PM10", "PM25", "SO2");
    }

    // ============================================
    // Helper Methods
    // ============================================

    private NearestAirQualityResult createNearestResult(String communeName, double distanceKm) {
        return new NearestAirQualityResult(
            "99999",
            communeName,
            48.0,
            2.0,
            LocalDate.now(),
            3,
            "Moyen",
            "#ffcc00",
            50, 60, 35, 25, 12,
            distanceKm
        );
    }
}
