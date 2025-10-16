package fr.airsen.api.service;

import fr.airsen.api.external.client.InseeApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler for INSEE data synchronization.
 * 
 * Performs yearly bulk synchronization of French administrative data
 * including communes, departments, and regions from INSEE API.
 */
@Component
@EnableScheduling
public class InseeDataScheduler {

    private static final Logger log = LoggerFactory.getLogger(InseeDataScheduler.class);

    private final CommuneService communeService;
    private final InseeApiClient inseeApiClient;

    public InseeDataScheduler(CommuneService communeService, InseeApiClient inseeApiClient) {
        this.communeService = communeService;
        this.inseeApiClient = inseeApiClient;
    }

    /**
     * Yearly INSEE data synchronization.
     * 
     * Executes every January 1st at 2:00 AM to sync all French communes
     * with the latest INSEE data including:
     * - code commune, name commune, coordinates
     * - code department, name department  
     * - code region, name region
     * - population
     */
    @Scheduled(cron = "0 0 2 1 1 *") // January 1st at 2:00 AM every year
    public void yearlyInseeSync() {
        log.info("=== Starting yearly INSEE data synchronization ===");
        
        try {
            // Use existing InseeApiClient to get all communes with bulk retrieval
            inseeApiClient.getMajorCommunes(0, Integer.MAX_VALUE) // All communes, no population threshold
                .flatMap(inseeResponse -> {
                    log.debug("Processing commune: {} ({}) - Population: {}", 
                            inseeResponse.name(), inseeResponse.inseeCode(), inseeResponse.population());
                    
                    // Use existing CommuneService business logic to save
                    return communeService.saveInseeDataToDatabase(inseeResponse);
                })
                .doOnNext(communeDTO -> log.debug("Successfully synced commune: {} ({})", 
                        communeDTO.getName(), communeDTO.getInseeCode()))
                .doOnError(error -> log.error("Failed to sync commune data", error))
                .doOnComplete(() -> log.info("=== Completed yearly INSEE data synchronization ==="))
                .subscribe();
                
        } catch (Exception e) {
            log.error("Error during yearly INSEE data synchronization", e);
        }
    }

    /**
     * Manual trigger for INSEE data synchronization (for testing/admin purposes).
     * 
     * Can be called programmatically if needed for immediate sync.
     */
    public void triggerManualSync() {
        log.info("=== Manual INSEE data synchronization triggered ===");
        yearlyInseeSync();
    }
}