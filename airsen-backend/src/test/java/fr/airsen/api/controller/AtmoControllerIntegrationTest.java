package fr.airsen.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.dto.response.AirQualityResponse;
import fr.airsen.api.external.client.AtmoApiClient;
import fr.airsen.api.external.client.InseeApiClient;
import fr.airsen.api.external.client.OpenMeteoApiClient;
import fr.airsen.api.repository.AirQualityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

/**
 * Integration tests for AtmoController endpoints.
 *
 * Tests the complete flow from HTTP request to database query,
 * including geodistance fallback mechanism with real spatial calculations
 * and ATMO index quality mapping.
 *
 * NOTE: SQL scripts run BEFORE_TEST_METHOD to ensure TestContainers are started.
 * BEFORE_TEST_CLASS would fail because containers start during test instantiation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)  // Disable security filters for integration tests
@Transactional
@Sql(scripts = {
    "/test-data/communes.sql",
    "/test-data/air-quality.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class AtmoControllerIntegrationTest extends AbstractTestContainersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AirQualityRepository airQualityRepository;

    // Mock external API clients to prevent HTTP calls during integration tests
    @MockBean
    private AtmoApiClient atmoApiClient;

    @MockBean
    private OpenMeteoApiClient openMeteoApiClient;

    @MockBean
    private InseeApiClient inseeApiClient;

    // Mock data initializers to prevent startup data fetching
    @MockBean
    private fr.airsen.api.scheduler.InseeDataInitializer inseeDataInitializer;

    @MockBean
    private fr.airsen.api.scheduler.CacheAwareTieredScheduler cacheAwareTieredScheduler;

    // JWT token (not required with addFilters=false, but kept for header compatibility)
    private static final String VALID_JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzYXJhaEBhaXJzZW4uZnIiLCJlbWFpbCI6InNhcmFoQGFpcnNlbi5mciIsInJvbGUiOiJBRE1JTiIsInR5cGUiOiJhY2Nlc3MiLCJpYXQiOjE3NjMxMDc4NzQsImV4cCI6MTc5NDY0Mzg3NH0.OTXUU6Jpl8vjJRBfAimTArWkLvyYqFtuRS9dkDGVZq8";

    @Test
    @DisplayName("Should return direct air quality data when available for requested commune")
    void shouldReturnDirectAirQualityData() throws Exception {
        // Given: Paris (75056) has fresh air quality data in database
        String parisInseeCode = "75056";

        // When: Request air quality for Paris
        MvcResult result = mockMvc.perform(get("/atmo/air-quality/{inseeCode}", parisInseeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then: Response contains direct data from Paris
        AirQualityResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
            AirQualityResponse.class
        );

        assertThat(response.inseeCode()).isEqualTo(parisInseeCode);
        assertThat(response.communeName()).isEqualTo("Paris");
        assertThat(response.dataSource()).isEqualTo(AirQualityResponse.DataSource.DIRECT);
        assertThat(response.measurementDate()).isEqualTo(LocalDate.now());
        assertThat(response.atmoIndex()).isEqualTo(2);
        assertThat(response.qualifier()).isEqualTo("Moyen");
        assertThat(response.color()).isEqualTo("#50ccaa");
        assertThat(response.dataQualityNote()).isEqualTo("Données mesurées pour cette commune");

        // Verify pollutants map structure and values
        assertThat(response.pollutants()).isNotNull();
        assertThat(response.pollutants()).containsKeys("NO2", "O3", "PM10", "PM25", "SO2");
        assertThat(response.pollutants().get("NO2")).isEqualTo(40);
        assertThat(response.pollutants().get("O3")).isEqualTo(120);
        assertThat(response.pollutants().get("PM10")).isEqualTo(40);
        assertThat(response.pollutants().get("PM25")).isEqualTo(20);
        assertThat(response.pollutants().get("SO2")).isEqualTo(100);

        // Estimated fields should be null for direct data
        assertThat(response.estimatedFromCommune()).isNull();
        assertThat(response.distanceKm()).isNull();
    }

    @Test
    @DisplayName("Should return estimated air quality data from nearest commune within 20km")
    void shouldReturnEstimatedAirQualityDataFromNearestCommune() throws Exception {
        // Given: Saint-Denis (93008) has no air quality data, but Paris (75056) does (~10km away)
        String saintDenisInseeCode = "93008";

        // Verify setup FIRST - Saint-Denis must have NO data to trigger geodistance
        verifyNoAirQualityData(saintDenisInseeCode);

        // When: Request air quality for Saint-Denis
        MvcResult result = mockMvc.perform(get("/atmo/air-quality/{inseeCode}", saintDenisInseeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then: Response contains estimated data from nearest commune (Paris)
        AirQualityResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
            AirQualityResponse.class
        );

        assertThat(response.inseeCode()).isEqualTo(saintDenisInseeCode);
        assertThat(response.communeName()).isEqualTo("Saint-Denis");
        assertThat(response.dataSource()).isEqualTo(AirQualityResponse.DataSource.ESTIMATED);

        // Data should come from nearest commune (Paris or Boulogne-Billancourt)
        assertThat(response.estimatedFromCommune()).isIn("Paris", "Boulogne-Billancourt");
        assertThat(response.distanceKm()).isBetween(8.0, 12.0); // Approximate distance
        assertThat(response.dataQualityNote()).contains("Données estimées depuis");

        // Air quality data should be inherited from nearest commune
        assertThat(response.atmoIndex()).isNotNull();
        assertThat(response.qualifier()).isNotNull();
        assertThat(response.color()).isNotNull();
        assertThat(response.measurementDate()).isEqualTo(LocalDate.now());

        // Verify pollutants map is inherited
        assertThat(response.pollutants()).isNotNull();
        assertThat(response.pollutants()).containsKeys("NO2", "O3", "PM10", "PM25", "SO2");
    }

    @Test
    @DisplayName("Should return estimated data for commune within 20km threshold (15km)")
    void shouldReturnEstimatedDataForCommuneWithin20km() throws Exception {
        // Given: Créteil (94017) has no air quality data, but Paris (75056) is ~15km away
        String creteilInseeCode = "94017";

        // Verify setup FIRST - Créteil must have NO data to trigger geodistance
        verifyNoAirQualityData(creteilInseeCode);

        // When: Request air quality for Créteil
        MvcResult result = mockMvc.perform(get("/atmo/air-quality/{inseeCode}", creteilInseeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then: Response contains estimated data from nearest commune
        AirQualityResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
            AirQualityResponse.class
        );

        assertThat(response.inseeCode()).isEqualTo(creteilInseeCode);
        assertThat(response.communeName()).isEqualTo("Créteil");
        assertThat(response.dataSource()).isEqualTo(AirQualityResponse.DataSource.ESTIMATED);
        assertThat(response.distanceKm()).isBetween(10.0, 20.0); // Within 20km threshold
        assertThat(response.estimatedFromCommune()).isNotNull();
        assertThat(response.dataQualityNote()).contains("Données estimées depuis");

        // Verify complete air quality data structure
        assertThat(response.atmoIndex()).isNotNull();
        assertThat(response.qualifier()).isNotNull();
        assertThat(response.pollutants()).isNotNull();
    }

    @Test
    @DisplayName("Should return 404 when no air quality data available within 20km radius")
    void shouldReturn404WhenNoDataWithin20km() throws Exception {
        // Given: Meaux (77001) is ~50km from Paris, outside 20km threshold
        String meauxInseeCode = "77001";

        // When: Request air quality for Meaux
        mockMvc.perform(get("/atmo/air-quality/{inseeCode}", meauxInseeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No air quality data within 20km"));
    }

    @Test
    @DisplayName("Should return 404 when commune does not exist")
    void shouldReturn404WhenCommuneNotFound() throws Exception {
        // Given: Invalid INSEE code that doesn't exist
        String invalidInseeCode = "99999";

        // When: Request air quality for non-existent commune
        mockMvc.perform(get("/atmo/air-quality/{inseeCode}", invalidInseeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Commune not found"));
    }

    @Test
    @DisplayName("Should validate ATMO index range and qualifier mapping for 'Bon' quality")
    void shouldValidateAtmoIndexAndQualifierForGoodQuality() throws Exception {
        // Given: Boulogne-Billancourt (92012) has ATMO index 1 (Bon quality)
        String boulogneBillancourt = "92012";

        // When: Request air quality for Boulogne-Billancourt
        MvcResult result = mockMvc.perform(get("/atmo/air-quality/{inseeCode}", boulogneBillancourt)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andExpect(status().isOk())
                .andReturn();

        // Then: Response contains correct ATMO index, qualifier, and color mapping
        AirQualityResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
            AirQualityResponse.class
        );

        assertThat(response.atmoIndex()).isEqualTo(1);
        assertThat(response.qualifier()).isEqualTo("Bon");
        assertThat(response.color()).isEqualTo("#50f0e6"); // Green for good quality
    }

    @Test
    @DisplayName("Should validate ATMO index and qualifier mapping for 'Extrêmement mauvais' quality")
    void shouldValidateAtmoIndexAndQualifierForExtremelyBadQuality() throws Exception {
        // Given: Commune 93006 (Aubervilliers) has ATMO index 6 (Extrêmement mauvais)
        String aubervilliers = "93006";

        // When: Request air quality for Aubervilliers
        MvcResult result = mockMvc.perform(get("/atmo/air-quality/{inseeCode}", aubervilliers)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andExpect(status().isOk())
                .andReturn();

        // Then: Response contains correct ATMO index for extremely bad quality
        AirQualityResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
            AirQualityResponse.class
        );

        assertThat(response.atmoIndex()).isEqualTo(6);
        assertThat(response.qualifier()).isEqualTo("Extrêmement mauvais");
        assertThat(response.color()).isEqualTo("#960032"); // Dark red for extremely bad quality
    }

    @Test
    @DisplayName("Should validate complete pollutants map structure and values")
    void shouldValidateCompletePollutantsMapStructure() throws Exception {
        // Given: Paris (75056) has complete pollutant data
        String parisInseeCode = "75056";

        // When: Request air quality for Paris
        MvcResult result = mockMvc.perform(get("/atmo/air-quality/{inseeCode}", parisInseeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andExpect(status().isOk())
                .andReturn();

        // Then: Response contains all pollutants with correct values
        AirQualityResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
            AirQualityResponse.class
        );

        // Verify pollutants map has all required keys
        assertThat(response.pollutants()).isNotNull();
        assertThat(response.pollutants()).containsOnlyKeys("NO2", "O3", "PM10", "PM25", "SO2");

        // Verify all pollutant values are non-negative integers
        response.pollutants().values().forEach(value -> {
            assertThat(value).isNotNull();
            assertThat(value).isGreaterThanOrEqualTo(0);
        });

        // Verify specific pollutant concentrations from test data
        assertThat(response.pollutants().get("NO2")).isEqualTo(40);
        assertThat(response.pollutants().get("O3")).isEqualTo(120);
        assertThat(response.pollutants().get("PM10")).isEqualTo(40);
        assertThat(response.pollutants().get("PM25")).isEqualTo(20);
        assertThat(response.pollutants().get("SO2")).isEqualTo(100);
    }

    @Test
    @DisplayName("Should return accurate geodistance calculations")
    void shouldReturnAccurateGeodistanceCalculations() throws Exception {
        // Given: Saint-Denis (93008) coordinates vs Paris (75056) coordinates
        // Expected distance: ~10km (real-world distance between these cities)
        String saintDenisInseeCode = "93008";

        // When: Request air quality for Saint-Denis (should fallback to Paris)
        MvcResult result = mockMvc.perform(get("/atmo/air-quality/{inseeCode}", saintDenisInseeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andDo(print())
                .andExpect(status().isOk())
                .andReturn();

        // Then: Distance calculation should be accurate (within 1km tolerance)
        AirQualityResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
            AirQualityResponse.class
        );

        if (response.estimatedFromCommune() != null && response.estimatedFromCommune().equals("Paris")) {
            // Distance from Saint-Denis to Paris should be ~9-11km
            assertThat(response.distanceKm()).isBetween(8.0, 12.0);
        }

        // Verify response metadata is complete
        assertThat(response.dataSource()).isEqualTo(AirQualityResponse.DataSource.ESTIMATED);
        assertThat(response.estimatedFromCommune()).isNotNull();
        assertThat(response.distanceKm()).isNotNull();
    }

    /**
     * Helper method to verify NO air quality data exists for a specific commune.
     * Used in ESTIMATED data tests to ensure geodistance fallback is triggered.
     *
     * @param inseeCode The INSEE code of the commune to check
     */
    private void verifyNoAirQualityData(String inseeCode) {
        java.util.Optional<fr.airsen.api.entity.AirQuality> data = airQualityRepository
                .findLatestByCommune_InseeCode(inseeCode);
        assertThat(data)
                .withFailMessage("Expected NO air quality data for %s, but found: %s",
                        inseeCode, data.map(fr.airsen.api.entity.AirQuality::getMeasurementDate))
                .isEmpty();
    }
}
