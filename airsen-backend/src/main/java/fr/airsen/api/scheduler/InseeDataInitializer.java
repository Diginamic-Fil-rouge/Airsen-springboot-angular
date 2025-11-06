package fr.airsen.api.scheduler;

import fr.airsen.api.external.client.InseeApiClient;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.service.CommuneService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Initializes commune data from INSEE API on application startup.
 *
 * This component loads all French communes with coordinates from INSEE API
 * when the Spring application context is fully initialized. It ensures
 * that the database is populated with commune data for testing phase
 * where communes are needed immediately on frontend startup.
 *
 * Features:
 * - Loads on ContextRefreshedEvent (after all Spring beans are initialized)
 * - Guard flag prevents duplicate loading on Spring DevTools restarts
 * - Uses existing CommuneService business logic for data integrity
 * - Non-blocking error handling (logs errors without stopping startup)
 * - Comprehensive logging for monitoring initialization progress
 */
@Component
public class InseeDataInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(InseeDataInitializer.class);

    private final InseeApiClient inseeApiClient;
    private final CommuneRepository communeRepository;
    private final CommuneService communeService;

    /**
     * Guard flag to prevent reload on Spring DevTools restart.
     * Ensures initialization only runs once per application lifecycle.
     */
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * Constructor for dependency injection.
     *
     * @param inseeApiClient INSEE API client for fetching commune data
     * @param communeRepository repository for checking existing communes
     * @param communeService service for saving commune data with business logic
     */
    public InseeDataInitializer(InseeApiClient inseeApiClient,
                              CommuneRepository communeRepository,
                              CommuneService communeService) {
        this.inseeApiClient = inseeApiClient;
        this.communeRepository = communeRepository;
        this.communeService = communeService;
    }

    /**
     * Handles application context refresh events to initialize commune data.
     *
     * This method is called when Spring application context is fully initialized.
     * It loads all communes from INSEE API and saves them to the database
     * using existing business logic for proper geographic hierarchy handling.
     *
     * @param event the context refresh event
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Check if already initialized (prevents duplicate runs)
        if (!initialized.compareAndSet(false, true)) {
            log.debug("Commune data initialization already completed, skipping");
            return;
        }

        log.info("Starting commune data initialization on application startup");

        try {
            // Check if communes already exist in database
            long existingCommuneCount = communeRepository.count();
            if (existingCommuneCount > 0) {
                log.info("Database already contains {} communes, skipping initialization", existingCommuneCount);
                return;
            }

            log.info("Database is empty, fetching all communes from INSEE API...");

            // Fetch all communes from INSEE API (no population threshold, no limit)
            inseeApiClient.getMajorCommunes(0, Integer.MAX_VALUE)
                .flatMap(inseeResponse -> {
                    log.debug("Processing commune: {} ({}) - Population: {}",
                            inseeResponse.name(), inseeResponse.inseeCode(), inseeResponse.population());

                    // Use existing CommuneService business logic to save with proper hierarchy
                    return communeService.saveInseeDataToDatabase(inseeResponse);
                })
                .doOnNext(communeDTO -> log.debug("Successfully initialized commune: {} ({})",
                        communeDTO.name(), communeDTO.inseeCode()))
                .doOnError(error -> log.error("Failed to initialize commune data", error))
                .doOnComplete(() -> {
                    long finalCount = communeRepository.count();
                    log.info("Commune data initialization completed: {} communes loaded", finalCount);
                })
                .subscribe();

        } catch (Exception e) {
            log.error("Error during commune data initialization on startup", e);
            // Don't throw exception - allow application to start even if initialization fails
        }
    }
}
