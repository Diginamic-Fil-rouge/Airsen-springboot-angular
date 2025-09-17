package fr.airsen.api.repository;

import fr.airsen.api.entity.AlertHistory;
import fr.airsen.api.entity.enums.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing AlertHistory entities.
 * 
 * Provides data access methods for tracking alert delivery history
 * with custom queries for monitoring alert performance and delivery status
 * according to Airsens data model specifications.
 */
@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistory, Long> {

    /**
     * Finds alert history for a specific alert.
     * 
     * @param alertId alert identifier
     * @param pageable pagination parameters
     * @return page of alert history records
     */
    @Query("SELECT ah FROM AlertHistory ah WHERE ah.alert.id = :alertId ORDER BY ah.sendDate DESC")
    Page<AlertHistory> findByAlertId(@Param("alertId") Long alertId, Pageable pageable);

    /**
     * Finds alert history for a specific user.
     * 
     * @param userId user identifier
     * @param pageable pagination parameters
     * @return page of alert history records for the user
     */
    @Query("SELECT ah FROM AlertHistory ah WHERE ah.alert.user.id = :userId ORDER BY ah.sendDate DESC")
    Page<AlertHistory> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Finds alert history by delivery status.
     * 
     * @param status delivery status
     * @param pageable pagination parameters
     * @return page of alert history records with the specified status
     */
    Page<AlertHistory> findByStatus(AlertStatus status, Pageable pageable);

    /**
     * Finds alert history within a date range.
     * 
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination parameters
     * @return page of alert history records within the date range
     */
    @Query("SELECT ah FROM AlertHistory ah WHERE ah.sendDate BETWEEN :startDate AND :endDate ORDER BY ah.sendDate DESC")
    Page<AlertHistory> findByDateRange(@Param("startDate") LocalDateTime startDate, 
                                      @Param("endDate") LocalDateTime endDate, 
                                      Pageable pageable);

    /**
     * Finds alert history for a specific user within a date range.
     * 
     * @param userId user identifier
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination parameters
     * @return page of alert history records for the user within the date range
     */
    @Query("SELECT ah FROM AlertHistory ah WHERE ah.alert.user.id = :userId AND ah.sendDate BETWEEN :startDate AND :endDate ORDER BY ah.sendDate DESC")
    Page<AlertHistory> findByUserIdAndDateRange(@Param("userId") Long userId,
                                               @Param("startDate") LocalDateTime startDate,
                                               @Param("endDate") LocalDateTime endDate,
                                               Pageable pageable);

    /**
     * Finds recent alert history for a user.
     * 
     * @param userId user identifier
     * @param since date from which to find history
     * @return list of recent alert history
     */
    @Query("SELECT ah FROM AlertHistory ah WHERE ah.alert.user.id = :userId AND ah.sendDate > :since ORDER BY ah.sendDate DESC")
    List<AlertHistory> findRecentByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Counts alert deliveries by status.
     * 
     * @param status delivery status
     * @return count of deliveries with the specified status
     */
    long countByStatus(AlertStatus status);

    /**
     * Counts alert deliveries for a user by status.
     * 
     * @param userId user identifier
     * @param status delivery status
     * @return count of deliveries for the user with the specified status
     */
    @Query("SELECT COUNT(ah) FROM AlertHistory ah WHERE ah.alert.user.id = :userId AND ah.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") AlertStatus status);

    /**
     * Counts successful alert deliveries for a user.
     * 
     * @param userId user identifier
     * @return count of successful deliveries
     */
    @Query("SELECT COUNT(ah) FROM AlertHistory ah WHERE ah.alert.user.id = :userId AND ah.status = 'SENT'")
    long countSuccessfulDeliveriesByUserId(@Param("userId") Long userId);

    /**
     * Counts failed alert deliveries for a user.
     * 
     * @param userId user identifier
     * @return count of failed deliveries
     */
    @Query("SELECT COUNT(ah) FROM AlertHistory ah WHERE ah.alert.user.id = :userId AND ah.status = 'FAILED'")
    long countFailedDeliveriesByUserId(@Param("userId") Long userId);

    /**
     * Updates the status of an alert history record.
     * 
     * @param historyId alert history identifier
     * @param status new delivery status
     */
    @Modifying
    @Query("UPDATE AlertHistory ah SET ah.status = :status WHERE ah.id = :historyId")
    void updateStatus(@Param("historyId") Long historyId, @Param("status") AlertStatus status);

    /**
     * Updates the status and error message of an alert history record.
     * 
     * @param historyId alert history identifier
     * @param status new delivery status
     * @param errorMessage error message description
     */
    @Modifying
    @Query("UPDATE AlertHistory ah SET ah.status = :status, ah.errorMessage = :errorMessage WHERE ah.id = :historyId")
    void updateStatusWithError(@Param("historyId") Long historyId, 
                              @Param("status") AlertStatus status, 
                              @Param("errorMessage") String errorMessage);

    /**
     * Marks an alert history record as successfully sent.
     * 
     * @param historyId alert history identifier
     */
    @Modifying
    @Query("UPDATE AlertHistory ah SET ah.status = 'SENT', ah.errorMessage = null WHERE ah.id = :historyId")
    void markAsSent(@Param("historyId") Long historyId);

    /**
     * Deletes alert history records older than a specified date.
     * Used for data retention policies.
     * 
     * @param cutoffDate date before which records should be deleted
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM AlertHistory ah WHERE ah.sendDate < :cutoffDate")
    int deleteByDateBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Finds alert delivery statistics for a user within a date range.
     * Returns aggregated data for reporting.
     * 
     * @param userId user identifier
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of statistics [status, count]
     */
    @Query("SELECT ah.status, COUNT(ah) FROM AlertHistory ah WHERE ah.alert.user.id = :userId AND ah.sendDate BETWEEN :startDate AND :endDate GROUP BY ah.status")
    List<Object[]> findDeliveryStatsByUserAndDateRange(@Param("userId") Long userId,
                                                       @Param("startDate") LocalDateTime startDate,
                                                       @Param("endDate") LocalDateTime endDate);

    /**
     * Finds system-wide alert delivery statistics within a date range.
     * 
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of statistics [status, count]
     */
    @Query("SELECT ah.status, COUNT(ah) FROM AlertHistory ah WHERE ah.sendDate BETWEEN :startDate AND :endDate GROUP BY ah.status")
    List<Object[]> findSystemDeliveryStats(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Finds alert history for a specific air quality measurement.
     * Used to track which alerts were triggered by a specific measurement.
     * 
     * @param airQualityId air quality measurement identifier
     * @return list of alert history records
     */
    @Query("SELECT ah FROM AlertHistory ah WHERE ah.airQuality.id = :airQualityId ORDER BY ah.sendDate DESC")
    List<AlertHistory> findByAirQualityId(@Param("airQualityId") Long airQualityId);
}