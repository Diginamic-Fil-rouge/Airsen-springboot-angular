package fr.airsen.api.config;

import fr.airsen.api.repository.RegionRepository;
import fr.airsen.api.repository.DepartmentRepository;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.ForumCategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Data Initialization Configuration
 * 
 * This component runs once at application startup to verify and log the status
 * of reference data in the database. It checks if geographic reference data
 * (regions, departments, communes) and forum categories are populated.
 * 
 * The actual data population should be done via SQL scripts (mock-data.sql)
 * or database migration tools. This component only performs verification.
 */
@Component
public class DataInitializerConfig implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializerConfig.class);

    @Autowired
    private RegionRepository regionRepository;
    
    @Autowired
    private DepartmentRepository departmentRepository;
    
    @Autowired
    private CommuneRepository communeRepository;
    
    @Autowired
    private ForumCategoryRepository forumCategoryRepository;

    @Value("${airsen.data.initialize:true}")
    private boolean initializeData;

    /**
     * Executes at application startup to verify reference data population.
     * 
     * This method checks the count of reference data entities and logs warnings
     * if any are missing. It does not create data automatically.
     * 
     * @param args Command line arguments (unused)
     */
    @Override
    public void run(String... args) {
        if (!initializeData) {
            log.info("Data initialization check disabled by configuration");
            return;
        }

        log.info("=================================================================");
        log.info("Verifying Reference Data Population");
        log.info("=================================================================");

        // Check geographic reference data
        long regionCount = regionRepository.count();
        long departmentCount = departmentRepository.count();
        long communeCount = communeRepository.count();
        long categoryCount = forumCategoryRepository.count();

        log.info("Reference Data Status:");
        log.info("  Regions: {} entries", regionCount);
        log.info("  Departments: {} entries", departmentCount);
        log.info("  Communes: {} entries", communeCount);
        log.info("  Forum Categories: {} entries", categoryCount);

        // Warn if data is missing
        if (regionCount == 0) {
            log.warn("⚠️  No regions found! Please execute mock-data.sql to populate reference data.");
            log.warn("   Command: docker exec airsen-mariadb mariadb -u airsen_dev -pdev_password -D airsen_dev -e \"source /tmp/mock-data.sql\"");
        }

        if (departmentCount == 0) {
            log.warn("⚠️  No departments found! Geographic filtering will not work.");
        }

        if (communeCount == 0) {
            log.warn("⚠️  No communes found! Air quality and weather data cannot be associated.");
        }

        if (categoryCount == 0) {
            log.warn("⚠️  No forum categories found! Forum system will appear empty.");
        }

        if (regionCount > 0 && departmentCount > 0 && communeCount > 0 && categoryCount > 0) {
            log.info("✅ All reference data is present and ready!");
        }

        log.info("=================================================================");
    }
}
