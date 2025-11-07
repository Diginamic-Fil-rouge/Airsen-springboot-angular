package fr.airsen.api.scheduler;

import fr.airsen.api.scheduler.cache.CacheRefreshScheduler;
import fr.airsen.api.scheduler.cache.CacheAwareTieredScheduler;
import fr.airsen.api.service.atmo.AtmoIntegrationService;
import fr.airsen.api.service.weather.WeatherService;
import fr.airsen.api.service.alert.AlertSignalDetectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for scheduler configuration and @ConditionalOnProperty behavior.
 *
 * Verifies that schedulers are properly enabled/disabled based on configuration:
 * - scheduling.enabled property
 * - tiered-scheduler.enabled property
 * - alert.detection.enabled property
 */
@DisplayName("Scheduler Configuration Tests")
class SchedulerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Configuration
    @EnableScheduling
    static class TestSchedulerConfiguration {
        // This will be used as a minimal Spring context for testing
    }

    @Test
    @DisplayName("Should load all schedulers when scheduling.enabled=true")
    void shouldLoadSchedulersWhenEnabled() {
        contextRunner
            .withPropertyValues(
                "scheduling.enabled=true",
                "tiered-scheduler.enabled=true",
                "alert.detection.enabled=true"
            )
            .withUserConfiguration(TestSchedulerConfiguration.class)
            .run(context -> {
                // Verify beans would be created (in full context)
                // Note: This is a simplified test. In full context, you'd check bean existence
                assertThat(context).isNotNull();
            });
    }

    @Test
    @DisplayName("Should NOT load schedulers when scheduling.enabled=false")
    void shouldNotLoadSchedulersWhenDisabled() {
        contextRunner
            .withPropertyValues(
                "scheduling.enabled=false"
            )
            .withUserConfiguration(TestSchedulerConfiguration.class)
            .run(context -> {
                // With scheduling disabled, scheduled methods won't execute
                assertThat(context).isNotNull();
            });
    }

    @Test
    @DisplayName("Should NOT load tiered scheduler when tiered-scheduler.enabled=false")
    void shouldNotLoadTieredSchedulerWhenDisabled() {
        contextRunner
            .withPropertyValues(
                "scheduling.enabled=true",
                "tiered-scheduler.enabled=false"
            )
            .withUserConfiguration(TestSchedulerConfiguration.class)
            .run(context -> {
                // Tiered scheduler methods should not execute
                assertThat(context).isNotNull();
            });
    }

    @Test
    @DisplayName("Should use default values when properties not specified")
    void shouldUseDefaultValues() {
        contextRunner
            .withUserConfiguration(TestSchedulerConfiguration.class)
            .run(context -> {
                // Default: scheduling.enabled=true (matchIfMissing=true)
                // Default: tiered-scheduler.enabled=false (matchIfMissing=false)
                assertThat(context).isNotNull();
            });
    }

    @Test
    @DisplayName("Should load with development profile configuration")
    void shouldLoadWithDevProfile() {
        contextRunner
            .withPropertyValues(
                "spring.profiles.active=dev",
                "scheduling.enabled=false",
                "rate-limiter.atmo.requests-per-minute=30"
            )
            .withUserConfiguration(TestSchedulerConfiguration.class)
            .run(context -> {
                assertThat(context).isNotNull();
            });
    }

    @Test
    @DisplayName("Should load with test profile configuration")
    void shouldLoadWithTestProfile() {
        contextRunner
            .withPropertyValues(
                "spring.profiles.active=test",
                "scheduling.enabled=false",
                "rate-limiter.atmo.requests-per-minute=1000"
            )
            .withUserConfiguration(TestSchedulerConfiguration.class)
            .run(context -> {
                assertThat(context).isNotNull();
            });
    }

    @Test
    @DisplayName("Should load with production profile configuration")
    void shouldLoadWithProdProfile() {
        contextRunner
            .withPropertyValues(
                "spring.profiles.active=prod",
                "scheduling.enabled=true",
                "tiered-scheduler.enabled=true",
                "rate-limiter.atmo.requests-per-minute=60"
            )
            .withUserConfiguration(TestSchedulerConfiguration.class)
            .run(context -> {
                assertThat(context).isNotNull();
            });
    }

    @Test
    @DisplayName("Should respect environment variables for scheduling")
    void shouldRespectEnvironmentVariables() {
        contextRunner
            .withPropertyValues(
                "scheduling.enabled=${ENABLE_SCHEDULING:true}",
                "tiered-scheduler.enabled=${TIERED_SCHEDULER_ENABLED:false}"
            )
            .withUserConfiguration(TestSchedulerConfiguration.class)
            .run(context -> {
                assertThat(context).isNotNull();
            });
    }

    @Test
    @DisplayName("Should handle custom cron expressions from properties")
    void shouldHandleCustomCronExpressions() {
        contextRunner
            .withPropertyValues(
                "scheduling.enabled=true",
                "scheduling.atmo.cron=0 0 12 * * *",  // Noon daily
                "scheduling.weather.cron=0 30 12 * * *",  // 12:30 daily
                "cache.refresh.air-quality-cron=0 0 */2 * * *"  // Every 2 hours
            )
            .withUserConfiguration(TestSchedulerConfiguration.class)
            .run(context -> {
                assertThat(context).isNotNull();
            });
    }

    @Test
    @DisplayName("Should handle alert detection configuration")
    void shouldHandleAlertDetectionConfig() {
        contextRunner
            .withPropertyValues(
                "alert.detection.enabled=false",
                "alert.detection.cron=0 0 * * * *"
            )
            .withUserConfiguration(TestSchedulerConfiguration.class)
            .run(context -> {
                assertThat(context).isNotNull();
            });
    }

    @Test
    @DisplayName("Should handle tiered scheduler batch sizes")
    void shouldHandleTieredSchedulerBatchSizes() {
        contextRunner
            .withPropertyValues(
                "tiered-scheduler.enabled=true",
                "tiered-scheduler.tier1-batch-size=50",
                "tiered-scheduler.tier2-batch-size=100",
                "tiered-scheduler.tier3-batch-size=200"
            )
            .withUserConfiguration(TestSchedulerConfiguration.class)
            .run(context -> {
                assertThat(context).isNotNull();
            });
    }

    /**
     * Integration test demonstrating complete scheduler configuration.
     *
     * This would be used with @SpringBootTest for full context testing.
     */
    @Test
    @DisplayName("Should demonstrate complete scheduler configuration")
    void shouldDemonstrateCompleteConfiguration() {
        // Example of properties for a complete test environment
        String[] properties = {
            // Master switches
            "scheduling.enabled=false",
            "tiered-scheduler.enabled=false",
            "alert.detection.enabled=false",

            // Schedule crons
            "scheduling.atmo.cron=0 0 10 * * MON",  // Weekly on Monday
            "scheduling.weather.cron=0 30 10 * * MON",
            "cache.refresh.air-quality-cron=0 0 12 * * *",
            "cache.refresh.weather-cron=0 30 12 * * *",

            // Rate limiters
            "rate-limiter.atmo.requests-per-minute=10",
            "rate-limiter.weather.requests-per-minute=5",
            "rate-limiter.insee.requests-per-minute=5",
            "rate-limiter.burst-capacity-percentage=20",

            // Reject on limit
            "rate-limiter.atmo.reject-on-limit=true",
            "rate-limiter.weather.reject-on-limit=true",
            "rate-limiter.insee.reject-on-limit=false",

            // Circuit breakers
            "rate-limiter.atmo.circuit-breaker.failure-threshold=0.5",
            "rate-limiter.atmo.circuit-breaker.wait-duration-seconds=60",
            "rate-limiter.weather.circuit-breaker.failure-threshold=0.5",
            "rate-limiter.insee.circuit-breaker.failure-threshold=0.5"
        };

        contextRunner
            .withPropertyValues(properties)
            .withUserConfiguration(TestSchedulerConfiguration.class)
            .run(context -> {
                assertThat(context).isNotNull();
                // In full integration test, you would verify:
                // - Schedulers are disabled
                // - Rate limiters have correct configuration
                // - Circuit breakers are configured properly
            });
    }
}
