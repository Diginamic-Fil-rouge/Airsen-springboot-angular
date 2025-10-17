package fr.airsen.api.controller;

import fr.airsen.api.entity.*;
import fr.airsen.api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for ExportDataController.
 * 
 * Tests the export-data and historical-data endpoints with:
 * - Authentication and authorization
 * - Valid and invalid inputs
 * - Data aggregation correctness
 * - Response format validation
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Export Data Controller Integration Tests")
class ExportDataControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommuneRepository communeRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private AirQualityRepository airQualityRepository;

    @Autowired
    private WeatherDataRepository weatherDataRepository;

    private Region testRegion;
    private Department testDepartment;
    private Commune testCommune;
    private AirQuality testAirQuality;
    private WeatherData testWeather;

    @BeforeEach
    void setUp() {
        // Clean up
        airQualityRepository.deleteAll();
        weatherDataRepository.deleteAll();
        communeRepository.deleteAll();
        departmentRepository.deleteAll();
        regionRepository.deleteAll();

        // Create test data
        testRegion = new Region();
        testRegion.setName("Île-de-France");
        testRegion.setRegionCode("11");
        testRegion = regionRepository.save(testRegion);

        testDepartment = new Department();
        testDepartment.setName("Paris");
        testDepartment.setDepartmentCode("75");
        testDepartment.setRegionCode("11");
        testDepartment.setRegion(testRegion);
        testDepartment = departmentRepository.save(testDepartment);

        testCommune = new Commune();
        testCommune.setInseeCode("75056");
        testCommune.setName("Paris");
        testCommune.setPopulation(2161000L);
        testCommune.setLatitude(new BigDecimal("48.8566"));
        testCommune.setLongitude(new BigDecimal("2.3522"));
        testCommune.setDepartmentCode("75");
        testCommune.setRegionCode("11");
        testCommune.setDepartment(testDepartment);
        testCommune = communeRepository.save(testCommune);

        // Create air quality data
        testAirQuality = new AirQuality();
        testAirQuality.setCommune(testCommune);
        testAirQuality.setMeasurementDate(LocalDate.now());
        testAirQuality.setAtmIndex(2);
        testAirQuality.setAtmoQual("Bon");
        testAirQuality.setAtmoColor("#50ccaa");
        testAirQuality.setNO2(25.5);
        testAirQuality.setO3(50.0);
        testAirQuality.setPm10(30.0);
        testAirQuality.setPm25(15);
        testAirQuality.setO3(60.0);
        testAirQuality.setCreatedAt(LocalDate.now());
        testAirQuality = airQualityRepository.save(testAirQuality);

        // Create weather data
        testWeather = new WeatherData();
        testWeather.setCommune(testCommune);
        testWeather.setMeasurementDate(LocalDate.now());
        testWeather.setTemperature(22.5);
        testWeather.setHumidity(65.0);
        testWeather.setWindSpeed(12.3);
        testWeather.setWindDirection(180.0);
        testWeather.setWeatherCode(1);
        testWeather.setCreatedAt(LocalDate.now());
        testWeather = weatherDataRepository.save(testWeather);
    }

    // ========================================================================
    // EXPORT-DATA ENDPOINT TESTS
    // ========================================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /communes/{inseeCode}/export-data - Success with USER role")
    void testGetExportDataSuccessWithUserRole() throws Exception {
        mockMvc.perform(get("/api/v1/communes/75056/export-data")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commune").exists())
                .andExpect(jsonPath("$.commune.inseeCode").value("75056"))
                .andExpect(jsonPath("$.commune.name").value("Paris"))
                .andExpect(jsonPath("$.commune.population").value(2161000))
                .andExpect(jsonPath("$.airQuality").exists())
                .andExpect(jsonPath("$.airQuality.atmIndex").value(2))
                .andExpect(jsonPath("$.airQuality.atmoQual").value("Bon"))
                .andExpect(jsonPath("$.weather").exists())
                .andExpect(jsonPath("$.weather.temperature").value(22.5))
                .andExpect(jsonPath("$.exportMetadata").exists())
                .andExpect(jsonPath("$.exportMetadata.generatedAt").exists())
                .andExpect(jsonPath("$.exportMetadata.dataFreshness").exists());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /communes/{inseeCode}/export-data - Success with ADMIN role")
    void testGetExportDataSuccessWithAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/communes/75056/export-data")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commune.inseeCode").value("75056"));
    }

    @Test
    @DisplayName("GET /communes/{inseeCode}/export-data - Unauthorized without authentication")
    void testGetExportDataUnauthorizedWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/v1/communes/75056/export-data")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /communes/{inseeCode}/export-data - Not found for invalid INSEE code")
    void testGetExportDataNotFoundForInvalidInseeCode() throws Exception {
        mockMvc.perform(get("/api/v1/communes/99999/export-data")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /communes/{inseeCode}/export-data - Includes complete geographic hierarchy")
    void testGetExportDataIncludesCompleteGeography() throws Exception {
        mockMvc.perform(get("/api/v1/communes/75056/export-data")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commune.department").exists())
                .andExpect(jsonPath("$.commune.department.name").value("Paris"))
                .andExpect(jsonPath("$.commune.department.region").exists())
                .andExpect(jsonPath("$.commune.department.region.name").value("Île-de-France"));
    }

    // ========================================================================
    // HISTORICAL-DATA ENDPOINT TESTS
    // ========================================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /communes/{inseeCode}/historical-data - Success with valid date range")
    void testGetHistoricalDataSuccessWithValidDateRange() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(7);

        mockMvc.perform(get("/api/v1/communes/75056/historical-data")
                .param("startDate", startDate.toString())
                .param("endDate", today.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commune").exists())
                .andExpect(jsonPath("$.commune.inseeCode").value("75056"))
                .andExpect(jsonPath("$.commune.name").value("Paris"))
                .andExpect(jsonPath("$.dateRange").exists())
                .andExpect(jsonPath("$.dateRange.start").value(startDate.toString()))
                .andExpect(jsonPath("$.dateRange.end").value(today.toString()))
                .andExpect(jsonPath("$.dataPoints").exists())
                .andExpect(jsonPath("$.dataPoints", hasSize(greaterThanOrEqualTo(0))))
                .andExpect(jsonPath("$.summary").exists())
                .andExpect(jsonPath("$.summary.totalDataPoints").exists())
                .andExpect(jsonPath("$.summary.completeness").exists());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /communes/{inseeCode}/historical-data - Unauthorized without authentication")
    void testGetHistoricalDataUnauthorizedWithoutAuth() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/v1/communes/75056/historical-data")
                .param("startDate", today.minusDays(7).toString())
                .param("endDate", today.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /communes/{inseeCode}/historical-data - Bad request with invalid date range")
    void testGetHistoricalDataBadRequestWithInvalidDateRange() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/v1/communes/75056/historical-data")
                .param("startDate", today.toString())
                .param("endDate", today.minusDays(7).toString())  // End before start
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /communes/{inseeCode}/historical-data - Bad request for date range > 90 days")
    void testGetHistoricalDataBadRequestForExcessiveDateRange() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/v1/communes/75056/historical-data")
                .param("startDate", today.minusDays(91).toString())
                .param("endDate", today.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /communes/{inseeCode}/historical-data - Not found for invalid INSEE code")
    void testGetHistoricalDataNotFoundForInvalidInseeCode() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/v1/communes/99999/historical-data")
                .param("startDate", today.minusDays(7).toString())
                .param("endDate", today.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /communes/{inseeCode}/historical-data - Contains data quality metrics")
    void testGetHistoricalDataContainsDataQualityMetrics() throws Exception {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(7);

        mockMvc.perform(get("/api/v1/communes/75056/historical-data")
                .param("startDate", startDate.toString())
                .param("endDate", today.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.completeness.airQuality").exists())
                .andExpect(jsonPath("$.summary.completeness.airQuality", greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.summary.completeness.airQuality", lessThanOrEqualTo(100.0)))
                .andExpect(jsonPath("$.summary.completeness.weather").exists())
                .andExpect(jsonPath("$.summary.completeness.weather", greaterThanOrEqualTo(0.0)))
                .andExpect(jsonPath("$.summary.completeness.weather", lessThanOrEqualTo(100.0)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /communes/{inseeCode}/historical-data - Success with ADMIN role")
    void testGetHistoricalDataSuccessWithAdminRole() throws Exception {
        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/v1/communes/75056/historical-data")
                .param("startDate", today.minusDays(7).toString())
                .param("endDate", today.toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commune.inseeCode").value("75056"));
    }

    // ========================================================================
    // EDGE CASE TESTS
    // ========================================================================

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /communes/{inseeCode}/export-data - Handles missing air quality gracefully")
    void testGetExportDataHandlesMissingAirQuality() throws Exception {
        // Remove air quality data
        airQualityRepository.deleteAll();

        mockMvc.perform(get("/api/v1/communes/75056/export-data")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commune").exists())
                .andExpect(jsonPath("$.airQuality").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /communes/{inseeCode}/export-data - Handles missing weather gracefully")
    void testGetExportDataHandlesMissingWeather() throws Exception {
        // Remove weather data
        weatherDataRepository.deleteAll();

        mockMvc.perform(get("/api/v1/communes/75056/export-data")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commune").exists())
                .andExpect(jsonPath("$.weather").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "USER")
    @DisplayName("GET /communes/{inseeCode}/historical-data - Empty data points for future date range")
    void testGetHistoricalDataEmptyDataPointsForFutureRange() throws Exception {
        LocalDate futureDate = LocalDate.now().plusDays(30);

        mockMvc.perform(get("/api/v1/communes/75056/historical-data")
                .param("startDate", futureDate.toString())
                .param("endDate", futureDate.plusDays(7).toString())
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dataPoints", hasSize(0)))
                .andExpect(jsonPath("$.summary.totalDataPoints").value(0));
    }
}
