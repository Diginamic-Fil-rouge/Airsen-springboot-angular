package fr.airsen.api.dto.cacheData;

import fr.airsen.api.entity.cacheData.CacheMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CachedEntry.
 *
 * Tests the generic cache entry wrapper including:
 * - Data encapsulation
 * - Freshness checks
 * - Stale fallback usability
 */
@DisplayName("CachedEntry Unit Tests")
class CachedEntryTest {

    private static final String TEST_KEY = "test:export:75056";

    @Test
    @DisplayName("CachedEntry wraps data and metadata correctly")
    void testCachedEntryWrapping() {
        // Arrange
        String testData = "export-data-123";
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Act
        CachedEntry<String> entry = new CachedEntry<>(testData, metadata);

        // Assert
        assertThat(entry.getData()).isEqualTo(testData);
        assertThat(entry.getMetadata()).isEqualTo(metadata);
    }

    @Test
    @DisplayName("isFresh() returns true for fresh cache")
    void testIsFreshForFreshCache() {
        // Arrange
        String testData = "weather-data";
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);
        CachedEntry<String> entry = new CachedEntry<>(testData, metadata);

        // Act & Assert
        assertThat(entry.isFresh()).isTrue();
    }

    @Test
    @DisplayName("isUsableAsStale() returns true for non-expired cache")
    void testIsUsableAsStaleForNonExpiredCache() {
        // Arrange
        String testData = "weather-data";
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);
        CachedEntry<String> entry = new CachedEntry<>(testData, metadata);

        // Act & Assert
        assertThat(entry.isUsableAsStale()).isTrue();
    }

    @Test
    @DisplayName("Generic type handling with different data types")
    void testGenericTypeHandling() {
        // Arrange
        class WeatherDTO {
            public String temperature;
            public String humidity;

            WeatherDTO(String temp, String humidity) {
                this.temperature = temp;
                this.humidity = humidity;
            }
        }

        WeatherDTO weatherData = new WeatherDTO("25C", "65%");
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Act
        CachedEntry<WeatherDTO> entry = new CachedEntry<>(weatherData, metadata);

        // Assert
        assertThat(entry.getData()).isEqualTo(weatherData);
        assertThat(entry.getData().temperature).isEqualTo("25C");
        assertThat(entry.getData().humidity).isEqualTo("65%");
    }

    @Test
    @DisplayName("CachedEntry works with Integer data type")
    void testCachedEntryWithIntegerType() {
        // Arrange
        Integer count = 42;
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Act
        CachedEntry<Integer> entry = new CachedEntry<>(count, metadata);

        // Assert
        assertThat(entry.getData()).isEqualTo(42);
        assertThat(entry.isFresh()).isTrue();
    }

    @Test
    @DisplayName("CachedEntry works with Boolean data type")
    void testCachedEntryWithBooleanType() {
        // Arrange
        Boolean value = true;
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Act
        CachedEntry<Boolean> entry = new CachedEntry<>(value, metadata);

        // Assert
        assertThat(entry.getData()).isEqualTo(true);
        assertThat(entry.isUsableAsStale()).isTrue();
    }

    @Test
    @DisplayName("Metadata is accessible from CachedEntry")
    void testMetadataAccessibility() {
        // Arrange
        String testData = "test";
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER1);
        CachedEntry<String> entry = new CachedEntry<>(testData, metadata);

        // Act & Assert
        assertThat(entry.getMetadata().getSource())
            .isEqualTo(CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER1);
        assertThat(entry.getMetadata().getKey()).isEqualTo(TEST_KEY);
        assertThat(entry.getMetadata().getTtlSeconds()).isEqualTo(2 * 3600);
    }
}
