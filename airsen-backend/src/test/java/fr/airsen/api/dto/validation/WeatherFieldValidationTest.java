package fr.airsen.api.dto.validation;

import fr.airsen.api.entity.WeatherData;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import jakarta.validation.Validation;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation tests for weather field constraints.
 *
 * Tests Bean Validation annotations on the new weather fields
 * to ensure proper constraint validation for:
 * - Apparent temperature bounds
 * - Precipitation non-negative values
 * - Cloud cover percentage bounds
 * - Pressure atmospheric range
 * - Wind gusts non-negative values
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Weather Field Validation Tests")
class WeatherFieldValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("Should validate apparent temperature within bounds")
    void shouldValidateApparentTemperatureWithinBounds() {
        // Valid apparent temperature
        WeatherData validWeatherData = new WeatherData();
        validWeatherData.setApparentTemperature(20.0);

        Set<ConstraintViolation<WeatherData>> violations = validator.validateProperty(validWeatherData, "apparentTemperature");
        assertTrue(violations.isEmpty(), "Valid apparent temperature should not cause violations");

        // Lower bound test
        validWeatherData.setApparentTemperature(-60.0);
        violations = validator.validateProperty(validWeatherData, "apparentTemperature");
        assertTrue(violations.isEmpty(), "Lower bound apparent temperature should be valid");

        // Upper bound test
        validWeatherData.setApparentTemperature(70.0);
        violations = validator.validateProperty(validWeatherData, "apparentTemperature");
        assertTrue(violations.isEmpty(), "Upper bound apparent temperature should be valid");

        // Invalid - below minimum
        validWeatherData.setApparentTemperature(-61.0);
        violations = validator.validateProperty(validWeatherData, "apparentTemperature");
        assertFalse(violations.isEmpty(), "Apparent temperature below -60°C should cause violation");

        // Invalid - above maximum
        validWeatherData.setApparentTemperature(71.0);
        violations = validator.validateProperty(validWeatherData, "apparentTemperature");
        assertFalse(violations.isEmpty(), "Apparent temperature above 70°C should cause violation");
    }

    @Test
    @DisplayName("Should validate precipitation non-negative values")
    void shouldValidatePrecipitationNonNegative() {
        WeatherData weatherData = new WeatherData();

        // Valid precipitation values
        weatherData.setPrecipitation(0.0);
        Set<ConstraintViolation<WeatherData>> violations = validator.validateProperty(weatherData, "precipitation");
        assertTrue(violations.isEmpty(), "Zero precipitation should be valid");

        weatherData.setPrecipitation(100.5);
        violations = validator.validateProperty(weatherData, "precipitation");
        assertTrue(violations.isEmpty(), "Positive precipitation should be valid");

        // Invalid - negative precipitation
        weatherData.setPrecipitation(-1.0);
        violations = validator.validateProperty(weatherData, "precipitation");
        assertFalse(violations.isEmpty(), "Negative precipitation should cause violation");
    }

    @Test
    @DisplayName("Should validate rain non-negative values")
    void shouldValidateRainNonNegative() {
        WeatherData weatherData = new WeatherData();

        // Valid rain values
        weatherData.setRain(0.0);
        Set<ConstraintViolation<WeatherData>> violations = validator.validateProperty(weatherData, "rain");
        assertTrue(violations.isEmpty(), "Zero rain should be valid");

        weatherData.setRain(50.5);
        violations = validator.validateProperty(weatherData, "rain");
        assertTrue(violations.isEmpty(), "Positive rain should be valid");

        // Invalid - negative rain
        weatherData.setRain(-0.1);
        violations = validator.validateProperty(weatherData, "rain");
        assertFalse(violations.isEmpty(), "Negative rain should cause violation");
    }

    @Test
    @DisplayName("Should validate cloud cover percentage bounds")
    void shouldValidateCloudCoverPercentageBounds() {
        WeatherData weatherData = new WeatherData();

        // Valid cloud cover values
        weatherData.setCloudCover(0);
        Set<ConstraintViolation<WeatherData>> violations = validator.validateProperty(weatherData, "cloudCover");
        assertTrue(violations.isEmpty(), "0% cloud cover should be valid");

        weatherData.setCloudCover(50);
        violations = validator.validateProperty(weatherData, "cloudCover");
        assertTrue(violations.isEmpty(), "50% cloud cover should be valid");

        weatherData.setCloudCover(100);
        violations = validator.validateProperty(weatherData, "cloudCover");
        assertTrue(violations.isEmpty(), "100% cloud cover should be valid");

        // Invalid - below 0%
        weatherData.setCloudCover(-1);
        violations = validator.validateProperty(weatherData, "cloudCover");
        assertFalse(violations.isEmpty(), "Negative cloud cover should cause violation");

        // Invalid - above 100%
        weatherData.setCloudCover(101);
        violations = validator.validateProperty(weatherData, "cloudCover");
        assertFalse(violations.isEmpty(), "Cloud cover above 100% should cause violation");
    }

    @Test
    @DisplayName("Should validate pressure within atmospheric range")
    void shouldValidatePressureWithinAtmosphericRange() {
        WeatherData weatherData = new WeatherData();

        // Valid pressure values
        weatherData.setPressureMsl(870.0);
        Set<ConstraintViolation<WeatherData>> violations = validator.validateProperty(weatherData, "pressureMsl");
        assertTrue(violations.isEmpty(), "Lower bound pressure should be valid");

        weatherData.setPressureMsl(1013.25);
        violations = validator.validateProperty(weatherData, "pressureMsl");
        assertTrue(violations.isEmpty(), "Standard pressure should be valid");

        weatherData.setPressureMsl(1085.0);
        violations = validator.validateProperty(weatherData, "pressureMsl");
        assertTrue(violations.isEmpty(), "Upper bound pressure should be valid");

        // Invalid - below atmospheric range
        weatherData.setPressureMsl(869.0);
        violations = validator.validateProperty(weatherData, "pressureMsl");
        assertFalse(violations.isEmpty(), "Pressure below 870 hPa should cause violation");

        // Invalid - above atmospheric range
        weatherData.setPressureMsl(1086.0);
        violations = validator.validateProperty(weatherData, "pressureMsl");
        assertFalse(violations.isEmpty(), "Pressure above 1085 hPa should cause violation");
    }

    @Test
    @DisplayName("Should validate wind gusts non-negative values")
    void shouldValidateWindGustsNonNegative() {
        WeatherData weatherData = new WeatherData();

        // Valid wind gust values
        weatherData.setWindGusts(0.0);
        Set<ConstraintViolation<WeatherData>> violations = validator.validateProperty(weatherData, "windGusts");
        assertTrue(violations.isEmpty(), "Zero wind gusts should be valid");

        weatherData.setWindGusts(150.5);
        violations = validator.validateProperty(weatherData, "windGusts");
        assertTrue(violations.isEmpty(), "Positive wind gusts should be valid");

        // Invalid - negative wind gusts
        weatherData.setWindGusts(-1.0);
        violations = validator.validateProperty(weatherData, "windGusts");
        assertFalse(violations.isEmpty(), "Negative wind gusts should cause violation");
    }

    @Test
    @DisplayName("Should validate snowfall non-negative values")
    void shouldValidateSnowfallNonNegative() {
        WeatherData weatherData = new WeatherData();

        // Valid snowfall values
        weatherData.setSnowfall(0.0);
        Set<ConstraintViolation<WeatherData>> violations = validator.validateProperty(weatherData, "snowfall");
        assertTrue(violations.isEmpty(), "Zero snowfall should be valid");

        weatherData.setSnowfall(25.5);
        violations = validator.validateProperty(weatherData, "snowfall");
        assertTrue(violations.isEmpty(), "Positive snowfall should be valid");

        // Invalid - negative snowfall
        weatherData.setSnowfall(-0.5);
        violations = validator.validateProperty(weatherData, "snowfall");
        assertFalse(violations.isEmpty(), "Negative snowfall should cause violation");
    }

    @Test
    @DisplayName("Should validate showers non-negative values")
    void shouldValidateShowersNonNegative() {
        WeatherData weatherData = new WeatherData();

        // Valid shower values
        weatherData.setShowers(0.0);
        Set<ConstraintViolation<WeatherData>> violations = validator.validateProperty(weatherData, "showers");
        assertTrue(violations.isEmpty(), "Zero showers should be valid");

        weatherData.setShowers(10.2);
        violations = validator.validateProperty(weatherData, "showers");
        assertTrue(violations.isEmpty(), "Positive showers should be valid");

        // Invalid - negative showers
        weatherData.setShowers(-1.5);
        violations = validator.validateProperty(weatherData, "showers");
        assertFalse(violations.isEmpty(), "Negative showers should cause violation");
    }

    @Test
    @DisplayName("Should allow null values for optional weather fields")
    void shouldAllowNullValuesForOptionalWeatherFields() {
        WeatherData weatherData = new WeatherData();

        // Set all new fields to null
        weatherData.setApparentTemperature(null);
        weatherData.setPrecipitation(null);
        weatherData.setRain(null);
        weatherData.setShowers(null);
        weatherData.setSnowfall(null);
        weatherData.setCloudCover(null);
        weatherData.setWindGusts(null);
        weatherData.setPressureMsl(null);

        // Validate each field individually
        Set<ConstraintViolation<WeatherData>> violations;

        violations = validator.validateProperty(weatherData, "apparentTemperature");
        assertTrue(violations.isEmpty(), "Null apparent temperature should be valid");

        violations = validator.validateProperty(weatherData, "precipitation");
        assertTrue(violations.isEmpty(), "Null precipitation should be valid");

        violations = validator.validateProperty(weatherData, "rain");
        assertTrue(violations.isEmpty(), "Null rain should be valid");

        violations = validator.validateProperty(weatherData, "showers");
        assertTrue(violations.isEmpty(), "Null showers should be valid");

        violations = validator.validateProperty(weatherData, "snowfall");
        assertTrue(violations.isEmpty(), "Null snowfall should be valid");

        violations = validator.validateProperty(weatherData, "cloudCover");
        assertTrue(violations.isEmpty(), "Null cloud cover should be valid");

        violations = validator.validateProperty(weatherData, "windGusts");
        assertTrue(violations.isEmpty(), "Null wind gusts should be valid");

        violations = validator.validateProperty(weatherData, "pressureMsl");
        assertTrue(violations.isEmpty(), "Null pressure should be valid");
    }

    @Test
    @DisplayName("Should validate multiple constraint violations simultaneously")
    void shouldValidateMultipleConstraintViolationsSimultaneously() {
        WeatherData weatherData = new WeatherData();

        // Set multiple invalid values
        weatherData.setApparentTemperature(-100.0);  // Below minimum
        weatherData.setPrecipitation(-5.0);          // Negative
        weatherData.setCloudCover(150);              // Above maximum
        weatherData.setPressureMsl(500.0);           // Below minimum

        Set<ConstraintViolation<WeatherData>> violations = validator.validate(weatherData);

        // Should have at least 4 violations (one for each invalid field)
        assertTrue(violations.size() >= 4,
            String.format("Expected at least 4 violations, but got %d: %s",
                violations.size(),
                violations.stream().map(ConstraintViolation::getMessage).toList()));

        // Verify specific constraint messages exist
        boolean hasApparentTempViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("apparentTemperature"));
        assertTrue(hasApparentTempViolation, "Should have apparent temperature violation");

        boolean hasPrecipitationViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("precipitation"));
        assertTrue(hasPrecipitationViolation, "Should have precipitation violation");

        boolean hasCloudCoverViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("cloudCover"));
        assertTrue(hasCloudCoverViolation, "Should have cloud cover violation");

        boolean hasPressureViolation = violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("pressureMsl"));
        assertTrue(hasPressureViolation, "Should have pressure violation");
    }
}
