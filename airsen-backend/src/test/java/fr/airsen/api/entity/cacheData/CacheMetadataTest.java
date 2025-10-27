package fr.airsen.api.entity.cacheData;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for CacheMetadata.
 *
 * Tests cache metadata tracking including:
 * - TTL selection based on data source
 * - Staleness calculation
 * - Expiration detection
 * - Age description formatting
 */
@DisplayName("CacheMetadata Unit Tests")
class CacheMetadataTest {

    private static final String TEST_KEY = "test:export:75056";

    @Test
    @DisplayName("Different data sources have correct TTL values")
    void testDataSourceTTLVariation() {
        // Test USER_UPDATE: 12 hours
        CacheMetadata userUpdate = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.USER_UPDATE);
        assertThat(userUpdate.getTtlSeconds()).isEqualTo(12 * 3600);

        // Test SCHEDULED_UPDATE_TIER1: 2 hours
        CacheMetadata tier1 = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER1);
        assertThat(tier1.getTtlSeconds()).isEqualTo(2 * 3600);

        // Test SCHEDULED_UPDATE_TIER2: 6 hours
        CacheMetadata tier2 = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER2);
        assertThat(tier2.getTtlSeconds()).isEqualTo(6 * 3600);

        // Test SCHEDULED_UPDATE_TIER3: 24 hours
        CacheMetadata tier3 = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER3);
        assertThat(tier3.getTtlSeconds()).isEqualTo(24 * 3600);

        // Test ON_DEMAND_FETCH: 6 hours
        CacheMetadata onDemand = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);
        assertThat(onDemand.getTtlSeconds()).isEqualTo(6 * 3600);

        // Test EXPORT_SNAPSHOT: 15 minutes
        CacheMetadata export = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.EXPORT_SNAPSHOT);
        assertThat(export.getTtlSeconds()).isEqualTo(15 * 60);

        // Test HISTORICAL_DATA: 24 hours
        CacheMetadata historical = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.HISTORICAL_DATA);
        assertThat(historical.getTtlSeconds()).isEqualTo(24 * 3600);
    }

    @Test
    @DisplayName("Metadata tracks source and TTL correctly")
    void testMetadataTrackingSourceAndTTL() {
        // Arrange & Act
        CacheMetadata metadata = new CacheMetadata(
            TEST_KEY,
            CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER2
        );

        // Assert
        assertThat(metadata.getSource()).isEqualTo(CacheMetadata.DataSource.SCHEDULED_UPDATE_TIER2);
        assertThat(metadata.getTtlSeconds()).isEqualTo(6 * 3600);
        assertThat(metadata.getKey()).isEqualTo(TEST_KEY);
        assertThat(metadata.getCachedAt()).isNotNull();
        assertThat(metadata.getExpiresAt()).isNotNull();
    }

    @Test
    @DisplayName("Fresh cache should not be expired")
    void testFreshCacheNotExpired() {
        // Arrange
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Assert - freshly created should not be expired
        assertThat(metadata.isExpired()).isFalse();
    }

    @Test
    @DisplayName("getStaleness calculates age ratio correctly for fresh cache")
    void testGetStalenessCalculationForFreshCache() {
        // Arrange
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Assert - very fresh cache should have low staleness
        double staleness = metadata.getStaleness();
        assertThat(staleness).isGreaterThanOrEqualTo(0.0);
        assertThat(staleness).isLessThan(0.1); // Less than 10% staleness for just-created cache
    }

    @Test
    @DisplayName("shouldRefresh returns false for fresh cache")
    void testShouldRefreshReturnsFalseForFreshCache() {
        // Arrange
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Assert
        assertThat(metadata.shouldRefresh()).isFalse();
    }

    @Test
    @DisplayName("getAgeDescription provides human-readable age")
    void testGetAgeDescriptionFormatting() {
        // Arrange
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Assert
        String ageDescription = metadata.getAgeDescription();
        assertThat(ageDescription).isNotNull();
        assertThat(ageDescription).isNotEmpty();
        // Fresh cache should say "just now"
        assertThat(ageDescription).isEqualTo("just now");
    }

    @Test
    @DisplayName("Version is set correctly")
    void testVersionIsSet() {
        // Arrange
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Assert
        assertThat(metadata.getVersion()).isEqualTo("1.0");
    }

    @Test
    @DisplayName("Expiration time is after cached time")
    void testExpirationAfterCachedTime() {
        // Arrange
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Assert
        assertThat(metadata.getExpiresAt()).isAfter(metadata.getCachedAt());
    }

    @Test
    @DisplayName("Staleness is bounded between 0.0 and 1.0 for valid cache")
    void testStalenessBoundedForValidCache() {
        // Arrange
        CacheMetadata metadata = new CacheMetadata(TEST_KEY, CacheMetadata.DataSource.ON_DEMAND_FETCH);

        // Assert
        double staleness = metadata.getStaleness();
        assertThat(staleness).isGreaterThanOrEqualTo(0.0);
        assertThat(staleness).isLessThanOrEqualTo(1.0);
    }
}
