package fr.airsen.api.scheduler;

import fr.airsen.api.AbstractTestContainersTest;
import fr.airsen.api.dto.CommuneDTO;
import fr.airsen.api.external.client.InseeApiClient;
import fr.airsen.api.external.dto.insee.InseeCommuneResponse;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.service.CommuneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration tests for InseeDataInitializer.
 *
 * Tests the complete application lifecycle behavior including:
 * - Commune data initialization on ContextRefreshedEvent
 * - Guard flag preventing duplicate initialization
 * - Error handling when INSEE API fails
 * - Database interaction verification
 * - Skip logic when communes already exist
 */
@ExtendWith(MockitoExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@DisplayName("InseeDataInitializer Integration Tests")
class InseeDataInitializerTest extends AbstractTestContainersTest {

    @Mock
    private InseeApiClient inseeApiClient;

    @Mock
    private CommuneRepository communeRepository;

    @Mock
    private CommuneService communeService;

    @Mock
    private ApplicationContext applicationContext;

    private InseeDataInitializer inseeDataInitializer;

    private List<InseeCommuneResponse> testCommunes;
    private List<CommuneDTO> testCommuneDTOs;

    @BeforeEach
    void setUp() {
        // Create InseeDataInitializer instance with mocked dependencies
        inseeDataInitializer = new InseeDataInitializer(
            inseeApiClient,
            communeRepository,
            communeService
        );

        // Create test INSEE commune data following AIRSEN's InseeCommuneResponse structure
        testCommunes = List.of(
            createTestInseeCommune("75056", "Paris", "75", "11", 2161000, 48.856614, 2.352222),
            createTestInseeCommune("69123", "Lyon", "69", "84", 518635, 45.764043, 4.835659),
            createTestInseeCommune("13055", "Marseille", "13", "93", 870731, 43.296482, 5.36978)
        );

        // Create corresponding CommuneDTO responses
        testCommuneDTOs = List.of(
            new CommuneDTO(1L, "75056", "Paris", "75", "11", 2161000L,
                          new BigDecimal("48.856614"), new BigDecimal("2.352222"), null, null, null),
            new CommuneDTO(2L, "69123", "Lyon", "69", "84", 518635L,
                          new BigDecimal("45.764043"), new BigDecimal("4.835659"), null, null, null),
            new CommuneDTO(3L, "13055", "Marseille", "13", "93", 870731L,
                          new BigDecimal("43.296482"), new BigDecimal("5.36978"), null, null, null)
        );
    }

    /**
     * Test 1: Successful commune initialization on application startup.
     * Verifies that communes are loaded when database is empty.
     */
    @Test
    @DisplayName("Should initialize communes on ContextRefreshedEvent when database is empty")
    void shouldInitializeCommunesOnStartupWhenDatabaseEmpty() {
        // Given
        when(communeRepository.count()).thenReturn(0L, 3L); // Empty initially, 3 after loading
        when(inseeApiClient.getMajorCommunes(0, Integer.MAX_VALUE))
            .thenReturn(Flux.fromIterable(testCommunes));

        // Mock CommuneService.saveInseeDataToDatabase for each commune
        when(communeService.saveInseeDataToDatabase(testCommunes.get(0)))
            .thenReturn(Mono.just(testCommuneDTOs.get(0)));
        when(communeService.saveInseeDataToDatabase(testCommunes.get(1)))
            .thenReturn(Mono.just(testCommuneDTOs.get(1)));
        when(communeService.saveInseeDataToDatabase(testCommunes.get(2)))
            .thenReturn(Mono.just(testCommuneDTOs.get(2)));

        // When
        ContextRefreshedEvent event = new ContextRefreshedEvent(applicationContext);
        inseeDataInitializer.onApplicationEvent(event);

        // Allow reactive stream to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then
        verify(communeRepository, times(2)).count(); // Initial check + final count
        verify(inseeApiClient, times(1)).getMajorCommunes(0, Integer.MAX_VALUE);
        verify(communeService, times(3)).saveInseeDataToDatabase(any(InseeCommuneResponse.class));
    }

    /**
     * Test 2: Guard flag prevents duplicate initialization.
     * Verifies that second ContextRefreshedEvent is ignored.
     */
    @Test
    @DisplayName("Should skip initialization on second ContextRefreshedEvent (guard flag)")
    void shouldSkipInitializationOnSecondEvent() {
        // Given - First initialization
        when(communeRepository.count()).thenReturn(0L, 3L);
        when(inseeApiClient.getMajorCommunes(0, Integer.MAX_VALUE))
            .thenReturn(Flux.fromIterable(testCommunes));
        when(communeService.saveInseeDataToDatabase(any(InseeCommuneResponse.class)))
            .thenReturn(Mono.just(testCommuneDTOs.get(0)));

        // When - First event
        ContextRefreshedEvent firstEvent = new ContextRefreshedEvent(applicationContext);
        inseeDataInitializer.onApplicationEvent(firstEvent);

        // Allow first initialization to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Reset mocks for second event
        reset(communeRepository, inseeApiClient, communeService);

        // When - Second event (should be ignored)
        ContextRefreshedEvent secondEvent = new ContextRefreshedEvent(applicationContext);
        inseeDataInitializer.onApplicationEvent(secondEvent);

        // Then - No interactions should occur
        verifyNoInteractions(communeRepository);
        verifyNoInteractions(inseeApiClient);
        verifyNoInteractions(communeService);
    }

    /**
     * Test 3: Skip initialization when communes already exist in database.
     * Verifies that initialization is skipped if communes are already populated.
     */
    @Test
    @DisplayName("Should skip initialization when communes already exist in database")
    void shouldSkipInitializationWhenCommunesExist() {
        // Given
        when(communeRepository.count()).thenReturn(1000L); // Database already has communes

        // When
        ContextRefreshedEvent event = new ContextRefreshedEvent(applicationContext);
        inseeDataInitializer.onApplicationEvent(event);

        // Then
        verify(communeRepository, times(1)).count();
        verifyNoInteractions(inseeApiClient);
        verifyNoInteractions(communeService);
    }

    /**
     * Test 4: Graceful error handling when INSEE API fails.
     * Verifies that application startup continues even if INSEE API is unavailable.
     */
    @Test
    @DisplayName("Should handle INSEE API errors gracefully without breaking startup")
    void shouldHandleInseeApiErrorsGracefully() {
        // Given
        when(communeRepository.count()).thenReturn(0L);
        when(inseeApiClient.getMajorCommunes(0, Integer.MAX_VALUE))
            .thenReturn(Flux.error(new RuntimeException("INSEE API unavailable")));

        // When - Should not throw exception
        ContextRefreshedEvent event = new ContextRefreshedEvent(applicationContext);
        inseeDataInitializer.onApplicationEvent(event);

        // Allow error handling to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - Repository count was checked, API was called, but no communes saved
        verify(communeRepository, times(1)).count();
        verify(inseeApiClient, times(1)).getMajorCommunes(0, Integer.MAX_VALUE);
        verifyNoInteractions(communeService);
    }

    /**
     * Test 5: Verify CommuneService integration for proper data saving.
     * Tests that the business logic layer is properly invoked for each commune.
     */
    @Test
    @DisplayName("Should use CommuneService.saveInseeDataToDatabase for each commune")
    void shouldUseCommuneServiceForEachCommune() {
        // Given
        when(communeRepository.count()).thenReturn(0L, 2L);
        // Return only 2 communes for focused testing
        List<InseeCommuneResponse> twoCommunes = testCommunes.subList(0, 2);
        when(inseeApiClient.getMajorCommunes(0, Integer.MAX_VALUE))
            .thenReturn(Flux.fromIterable(twoCommunes));

        // Mock specific responses for each commune
        when(communeService.saveInseeDataToDatabase(twoCommunes.get(0)))
            .thenReturn(Mono.just(testCommuneDTOs.get(0)));
        when(communeService.saveInseeDataToDatabase(twoCommunes.get(1)))
            .thenReturn(Mono.just(testCommuneDTOs.get(1)));

        // When
        ContextRefreshedEvent event = new ContextRefreshedEvent(applicationContext);
        inseeDataInitializer.onApplicationEvent(event);

        // Allow processing to complete
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Then - Verify each commune was processed through CommuneService
        verify(communeService, times(1)).saveInseeDataToDatabase(twoCommunes.get(0));
        verify(communeService, times(1)).saveInseeDataToDatabase(twoCommunes.get(1));
        verify(communeService, times(2)).saveInseeDataToDatabase(any(InseeCommuneResponse.class));
    }

    /**
     * Helper method to create test INSEE commune data.
     *
     * @param inseeCode INSEE code (5 digits)
     * @param name commune name
     * @param departmentCode department code
     * @param regionCode region code
     * @param population commune population
     * @param latitude latitude coordinate
     * @param longitude longitude coordinate
     * @return InseeCommuneResponse test object
     */
    private InseeCommuneResponse createTestInseeCommune(String inseeCode, String name,
                                                       String departmentCode, String regionCode,
                                                       Integer population, Double latitude, Double longitude) {

        // Create geographic center with coordinates
        InseeCommuneResponse.GeographicCenter center = new InseeCommuneResponse.GeographicCenter(
            List.of(longitude, latitude) // INSEE format: [longitude, latitude]
        );

        // Create department info
        InseeCommuneResponse.DepartmentInfo departmentInfo =
            new InseeCommuneResponse.DepartmentInfo(departmentCode, "Department " + departmentCode);

        // Create region info
        InseeCommuneResponse.RegionInfo regionInfo =
            new InseeCommuneResponse.RegionInfo(regionCode, "Region " + regionCode);

        return new InseeCommuneResponse(
            inseeCode,
            name,
            departmentCode,
            regionCode,
            List.of(inseeCode.substring(0, 2) + "000"), // Mock postal codes
            population,
            100.0, // Mock surface area
            center,
            departmentInfo,
            regionInfo
        );
    }
}
