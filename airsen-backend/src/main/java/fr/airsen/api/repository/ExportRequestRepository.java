package fr.airsen.api.repository;

import fr.airsen.api.entity.ExportRequest;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.ExportStatus;
import fr.airsen.api.entity.enums.ExportType;
import fr.airsen.api.entity.enums.FileFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Repository for managing ExportRequest entities.
 * 
 * Provides data access methods for export requests with user limit validation
 * and status management according to Airsen export system specifications.
 */
@Repository
public interface ExportRequestRepository extends JpaRepository<ExportRequest, Long> {

    // ==================== BASIC QUERIES ====================

    Page<ExportRequest> findByUser(User user, Pageable pageable);

    Page<ExportRequest> findByUserAndStatus(User user, ExportStatus status, Pageable pageable);

    Page<ExportRequest> findByExportTypeAndFileFormat(ExportType exportType, FileFormat fileFormat, Pageable pageable);

    // ==================== EXPORT LIMITS VALIDATION ====================

    /**
     * Counts user export requests created in the last 24 hours.
     * Limit: 5 requests per day.
     *
     * @param user  user
     * @param since cutoff date (24h ago)
     * @return number of requests in the last 24h
     */
    @Query("SELECT COUNT(er) FROM ExportRequest er WHERE er.user = :user AND er.createdDate >= :since")
    long countByUserAndCreatedDateAfter(@Param("user") User user, @Param("since") LocalDateTime since);

    /**
     * Counts user export requests created in the current month.
     * Limit: 10 requests per month.
     *
     * @param user       user
     * @param monthStart start of current month
     * @return number of requests in current month
     */
    @Query("SELECT COUNT(er) FROM ExportRequest er WHERE er.user = :user AND er.createdDate >= :monthStart")
    long countByUserAndCreatedDateInCurrentMonth(@Param("user") User user, @Param("monthStart") LocalDateTime monthStart);

    /**
     * Counts user export requests created in the current year.
     * Limit: 15 requests per year.
     *
     * @param user      user
     * @param yearStart start of current year
     * @return number of requests in current year
     */
    @Query("SELECT COUNT(er) FROM ExportRequest er WHERE er.user = :user AND er.createdDate >= :yearStart")
    long countByUserAndCreatedDateInCurrentYear(@Param("user") User user, @Param("yearStart") LocalDateTime yearStart);

    /**
     * Checks if a user can create a new export request.
     * Validates all limits: daily, monthly, yearly.
     *
     * @param userId     user identifier
     * @param dayLimit   start of day (for daily limit)
     * @param monthLimit start of month (for monthly limit)
     * @param yearLimit  start of year (for yearly limit)
     * @return true if user can create a request
     */
    @Query("""

            SELECT CASE 
            WHEN (SELECT COUNT(er1) FROM ExportRequest er1 WHERE er1.user.id = :userId AND er1.createdDate >= :dayLimit) >= 5 THEN false
            WHEN (SELECT COUNT(er2) FROM ExportRequest er2 WHERE er2.user.id = :userId AND er2.createdDate >= :monthLimit) >= 10 THEN false
            WHEN (SELECT COUNT(er3) FROM ExportRequest er3 WHERE er3.user.id = :userId AND er3.createdDate >= :yearLimit) >= 15 THEN false
            ELSE true
        END
        """)
    boolean canUserCreateNewExport(@Param("userId") Long userId, 
                                  @Param("dayLimit") LocalDateTime dayLimit,
                                  @Param("monthLimit") LocalDateTime monthLimit, 
                                  @Param("yearLimit") LocalDateTime yearLimit);

    // ==================== STATUS AND FILTERING QUERIES ====================

    @Query("SELECT er FROM ExportRequest er WHERE er.user = :user AND er.status = 'COMPLETED' AND er.generatedFile IS NOT NULL")
    Page<ExportRequest> findSuccessfulExportsByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT er FROM ExportRequest er WHERE er.startDate >= :startDate AND er.endDate <= :endDate")
    Page<ExportRequest> findByDateRange(@Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate, 
                                       Pageable pageable);
}