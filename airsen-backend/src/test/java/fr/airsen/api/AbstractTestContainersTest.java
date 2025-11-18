package fr.airsen.api;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Base class for integration tests using TestContainers.
 *
 * Tests extending this class will automatically use real databases
 * instead of H2, matching the production environment.
 *
 * Benefits:
 * - Matches production database (MariaDB vs H2)
 * - No SQL dialect differences
 * - Automatic container lifecycle management
 * - Isolated test environment
 */
@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractTestContainersTest {

    @Container
    protected static MariaDBContainer<?> mariadb = new MariaDBContainer<>(
            DockerImageName.parse("mariadb:11.6")
    )
            .withDatabaseName("airsen_test")
            .withUsername("airsen_test")
            .withPassword("test_password")
            .withReuse(true); // Reuse container for faster test execution


    @Container
    protected static RedisContainer redis = new RedisContainer(
            DockerImageName.parse("redis:7-alpine")
    )
            .withExposedPorts(6379)
            .withReuse(true);

    /**
     * Dynamically inject TestContainers connection details into Spring context.
     * This overrides static configuration in application-test.properties.
     */
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // MariaDB configuration
        registry.add("spring.datasource.url", mariadb::getJdbcUrl);
        registry.add("spring.datasource.username", mariadb::getUsername);
        registry.add("spring.datasource.password", mariadb::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.mariadb.jdbc.Driver");

        // Redis configuration
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());

        // Enable Redis caching for integration tests
        registry.add("spring.cache.type", () -> "redis");

        // HikariCP connection pool tuning (prevent pool exhaustion)
        registry.add("spring.datasource.hikari.maximum-pool-size", () -> "20");
        registry.add("spring.datasource.hikari.minimum-idle", () -> "5");
        registry.add("spring.datasource.hikari.connection-timeout", () -> "30000");
        registry.add("spring.datasource.hikari.leak-detection-threshold", () -> "60000");

        // Enable connection leak detection logging
        registry.add("logging.level.com.zaxxer.hikari", () -> "DEBUG");
    }
}
