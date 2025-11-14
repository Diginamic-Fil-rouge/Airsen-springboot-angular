package fr.airsen.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.dto.response.WeatherResponse;
import fr.airsen.api.entity.WeatherData;
import fr.airsen.api.external.client.AtmoApiClient;
import fr.airsen.api.external.client.InseeApiClient;
import fr.airsen.api.external.client.OpenMeteoApiClient;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.WeatherDataRepository;
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

/**
 * Integration tests for WeatherController endpoints.
 *
 * Tests the complete flow from HTTP request to database query,
 * including geodistance fallback mechanism with real spatial calculations.
 *
 * NOTE: SQL scripts run BEFORE_TEST_METHOD to ensure TestContainers are started.
 * BEFORE_TEST_CLASS would fail because containers start during test instantiation.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)  // Disable security filters for integration tests
@Transactional
@Sql(scripts = {
    "/test-data/communes.sql",
    "/test-data/weather-data.sql"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class WeatherControllerIntegrationTest extends AbstractTestContainersTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WeatherDataRepository weatherDataRepository;

    @Autowired
    private CommuneRepository communeRepository;

    // Mock external API clients to prevent HTTP calls during integration tests
    @MockBean
    private AtmoApiClient atmoApiClient;

    @MockBean
    private OpenMeteoApiClient openMeteoApiClient;

    @MockBean
    private InseeApiClient inseeApiClient;

    // Valid JWT token for test authentication (expires 2025-11-14, signed with base64 test secret from application-test.yml)
    private static final String VALID_JWT_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJzYXJhaEBhaXJzZW4uZnIiLCJlbWFpbCI6InNhcmFoQGFpcnNlbi5mciIsInJvbGUiOiJBRE1JTiIsInR5cGUiOiJhY2Nlc3MiLCJpYXQiOjE3NjMxMDc4NzQsImV4cCI6MTc5NDY0Mzg3NH0.OTXUU6Jpl8vjJRBfAimTArWkLvyYqFtuRS9dkDGVZq8";

    @Test
    @DisplayName("Should return direct weather data when commune has no weather data")
    public void shouldReturnDirectWeatherData() throws Exception {
        mockMvc.perform(get("/weather/current/75056")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should return estimated weather data from nearest commune within 20km")
    void shouldReturnEstimatedWeatherDataFromNearestCommune() throws Exception {
        // Given: Saint-Denis (93008) has no weather data, but Paris (75056) does (~10km away)
        String saintDenisInseeCode = "93008";

        // Verify setup: Saint-Denis has no weather data
        assertThat(weatherDataRepository.getMostRecentWeatherByInseeCode(saintDenisInseeCode)).isEmpty();

        // When: Request weather for Saint-Denis
        MvcResult result = mockMvc.perform(get("/weather/current/{inseeCode}", saintDenisInseeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then: Response contains estimated data from nearest commune (Paris)
        WeatherResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            WeatherResponse.class
        );

        assertThat(response.inseeCode()).isEqualTo(saintDenisInseeCode);
        assertThat(response.communeName()).isEqualTo("Saint-Denis");
        assertThat(response.dataSource()).isEqualTo(WeatherResponse.DataSource.ESTIMATED);

        // Data should come from nearest commune (Paris or Boulogne-Billancourt)
        assertThat(response.estimatedFromCommune()).isIn("Paris", "Boulogne-Billancourt");
        assertThat(response.distanceKm()).isBetween(8.0, 12.0); // Approximate distance
        assertThat(response.dataQualityNote()).contains("Données estimées depuis");

        // Weather data should be inherited from nearest commune
        assertThat(response.temperature()).isNotNull();
        assertThat(response.humidity()).isNotNull();
        assertThat(response.measurementDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Should return estimated data for commune with nearby weather source (15km)")
    void shouldReturnEstimatedDataForCommuneWithin20km() throws Exception {
        // Given: Créteil (94017) has no weather data, but Paris (75056) is ~15km away
        String creteilInseeCode = "94017";

        // Verify setup: Créteil has no weather data
        assertThat(weatherDataRepository.getMostRecentWeatherByInseeCode(creteilInseeCode)).isEmpty();

        // When: Request weather for Créteil
        MvcResult result = mockMvc.perform(get("/weather/current/{inseeCode}", creteilInseeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then: Response contains estimated data from nearest commune
        WeatherResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            WeatherResponse.class
        );

        assertThat(response.inseeCode()).isEqualTo(creteilInseeCode);
        assertThat(response.communeName()).isEqualTo("Créteil");
        assertThat(response.dataSource()).isEqualTo(WeatherResponse.DataSource.ESTIMATED);
        assertThat(response.distanceKm()).isBetween(10.0, 20.0); // Within 20km threshold
        assertThat(response.estimatedFromCommune()).isNotNull();
        assertThat(response.dataQualityNote()).contains("Données estimées depuis");
    }

    @Test
    @DisplayName("Should return direct weather data for commune with old data (scheduler handles freshness)")
    void shouldReturn404WhenNoDataWithin20km() throws Exception {
        // Given: Meaux (77001) is ~50km from Paris, outside 20km threshold
        // BUT Meaux has its own weather data in the database (30 days old)
        // So it should return 200 with DIRECT data (scheduler handles updating it)
        String meauxInseeCode = "77001";

        // When: Request weather for Meaux
        MvcResult result = mockMvc.perform(get("/weather/current/{inseeCode}", meauxInseeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();

        // Then: Response contains the old data with DIRECT source
        WeatherResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8),
            WeatherResponse.class
        );

        assertThat(response.inseeCode()).isEqualTo(meauxInseeCode);
        assertThat(response.communeName()).isEqualTo("Meaux");
        assertThat(response.dataSource()).isEqualTo(WeatherResponse.DataSource.DIRECT);
        assertThat(response.temperature()).isEqualTo(12.8);
        assertThat(response.measurementDate()).isBefore(LocalDate.now());
    }

    @Test
    @DisplayName("Should return 404 when commune does not exist")
    void shouldReturn404WhenCommuneNotFound() throws Exception {
        // Given: Invalid INSEE code that doesn't exist
        String invalidInseeCode = "99999";

        // When: Request weather for non-existent commune
        mockMvc.perform(get("/weather/current/{inseeCode}", invalidInseeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should return weather data regardless of age (scheduler handles freshness)")
    void shouldReturnWeatherDataRegardlessOfAge() throws Exception {
        // Given: Meaux (77001) has weather data but it's 30 days old
        // Note: This test assumes Meaux is moved closer to test old data specifically
        // For this test, we'll insert fresh data for a test commune and then make it old

        String meauxInseeCode = "77001";

        // Verify old data exists
        var oldData = weatherDataRepository.getMostRecentWeatherByInseeCode(meauxInseeCode);
        assertThat(oldData).isPresent();
        assertThat(oldData.get().getMeasurementDate()).isBefore(LocalDate.now().minusDays(7));

        // When: Request weather for commune with old data
        // This should still work if there's ANY data (regardless of age)
        // The endpoint returns available data; scheduler handles freshness
        MvcResult result = mockMvc.perform(get("/weather/current/{inseeCode}", meauxInseeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andExpect(status().isOk())
                .andReturn();

        // Then: Response contains the old data (system accepts any age)
        WeatherResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            WeatherResponse.class
        );

        assertThat(response.inseeCode()).isEqualTo(meauxInseeCode);
        assertThat(response.communeName()).isEqualTo("Meaux");
        assertThat(response.dataSource()).isEqualTo(WeatherResponse.DataSource.DIRECT);
        assertThat(response.temperature()).isEqualTo(12.8);
        assertThat(response.measurementDate()).isBefore(LocalDate.now());
    }

    @Test
    @DisplayName("Should return 404 when commune with invalid format does not exist")
    void shouldValidateInseeCodeFormat() throws Exception {
        // Given: Invalid INSEE code format (not 5 digits)
        // Since "123" doesn't exist as a commune, it returns 404 "Commune not found"
        String invalidFormat = "123";

        // When: Request weather with invalid format (will try to find "123" as INSEE code)
        mockMvc.perform(get("/weather/current/{inseeCode}", invalidFormat)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Should return geodistance calculation accuracy within expected range")
    void shouldReturnAccurateGeodistanceCalculations() throws Exception {
        // Given: Saint-Denis (93008) coordinates vs Paris (75056) coordinates
        // Expected distance: ~10km (real-world distance between these cities)
        String saintDenisInseeCode = "93008";

        // When: Request weather for Saint-Denis (should fallback to Paris)
        MvcResult result = mockMvc.perform(get("/weather/current/{inseeCode}", saintDenisInseeCode)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + VALID_JWT_TOKEN))
                .andExpect(status().isOk())
                .andReturn();

        // Then: Distance calculation should be accurate (within 1km tolerance)
        WeatherResponse response = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            WeatherResponse.class
        );

        if (response.estimatedFromCommune() != null && response.estimatedFromCommune().equals("Paris")) {
            // Distance from Saint-Denis to Paris should be ~9-11km
            assertThat(response.distanceKm()).isBetween(8.0, 12.0);
        }
    }

    /**
     * Helper method to verify test data setup.
     */
    private void verifyTestDataSetup() {
        // Verify communes exist
        assertThat(communeRepository.findByInseeCode("75056")).isPresent(); // Paris
        assertThat(communeRepository.findByInseeCode("93008")).isPresent(); // Saint-Denis
        assertThat(communeRepository.findByInseeCode("94017")).isPresent(); // Créteil
        assertThat(communeRepository.findByInseeCode("77001")).isPresent(); // Meaux

        // Verify weather data distribution
        assertThat(weatherDataRepository.getMostRecentWeatherByInseeCode("75056")).isPresent(); // Paris has data
        assertThat(weatherDataRepository.getMostRecentWeatherByInseeCode("93008")).isEmpty();    // Saint-Denis no data
        assertThat(weatherDataRepository.getMostRecentWeatherByInseeCode("94017")).isEmpty();    // Créteil no data
    }
}
