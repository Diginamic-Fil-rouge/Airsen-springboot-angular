package fr.airsen.api.repository;

import fr.airsen.api.entity.NotificationCampaign;
import fr.airsen.api.entity.enums.GeographicScopeType;
import fr.airsen.api.entity.enums.NotificationCampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationCampaignRepository extends JpaRepository<NotificationCampaign, Long> {

    /**
     * Finds campaigns by status with pagination.
     * Used for filtering campaigns in admin dashboard.
     * 
     * @param status campaign status (DRAFT, SENDING, COMPLETED, FAILED)
     * @param pageable pagination parameters
     * @return page of campaigns with the specified status
     */
    Page<NotificationCampaign> findByStatus(NotificationCampaignStatus status, Pageable pageable);

    /**
     * Finds campaigns created by a specific admin user.
     * 
     * @param userId admin user identifier
     * @param pageable pagination parameters
     * @return page of campaigns created by the user
     */
    @Query("SELECT c FROM NotificationCampaign c WHERE c.createdBy.id = :userId ORDER BY c.createdAt DESC")
    Page<NotificationCampaign> findByCreatedById(@Param("userId") Long userId, Pageable pageable);

    /**
     * Finds campaigns by geographic scope.
     * 
     * @param scopeType geographic scope type (FRANCE, REGION, DEPARTMENT, COMMUNE)
     * @param pageable pagination parameters
     * @return page of campaigns for the specified scope
     */
    Page<NotificationCampaign> findByScopeType(GeographicScopeType scopeType, Pageable pageable);

    /**
     * Finds campaigns by scope type and scope ID.
     * 
     * @param scopeType geographic scope type
     * @param scopeId identifier of the geographic entity
     * @param pageable pagination parameters
     * @return page of campaigns for the specified scope
     */
    Page<NotificationCampaign> findByScopeTypeAndScopeId(GeographicScopeType scopeType, Long scopeId, Pageable pageable);

    /**
     * Finds campaigns created within a date range.
     * 
     * @param start start of date range
     * @param end end of date range
     * @param pageable pagination parameters
     * @return page of campaigns created in the range
     */
    @Query("SELECT c FROM NotificationCampaign c WHERE c.createdAt BETWEEN :start AND :end ORDER BY c.createdAt DESC")
    Page<NotificationCampaign> findByCreatedAtBetween(
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end,
        Pageable pageable
    );

    /**
     * Finds all draft campaigns.
     * Used for admin to resume editing campaigns.
     * 
     * @return list of draft campaigns
     */
    List<NotificationCampaign> findByStatus(NotificationCampaignStatus status);

    /**
     * Finds campaigns currently being sent.
     * Used for monitoring active broadcast operations.
     * 
     * @return list of campaigns with SENDING status
     */
    @Query("SELECT c FROM NotificationCampaign c WHERE c.status = 'SENDING' ORDER BY c.createdAt ASC")
    List<NotificationCampaign> findActiveCampaigns();

    /**
     * Finds campaigns linked to a specific alert signal.
     * 
     * @param alertSignalId alert signal identifier
     * @return list of campaigns created from the alert signal
     */
    @Query("SELECT c FROM NotificationCampaign c WHERE c.alertSignal.id = :alertSignalId ORDER BY c.createdAt DESC")
    List<NotificationCampaign> findByAlertSignalId(@Param("alertSignalId") Long alertSignalId);

    /**
     * Counts campaigns by status.
     * Used for dashboard statistics.
     * 
     * @param status campaign status
     * @return count of campaigns with the status
     */
    Long countByStatus(NotificationCampaignStatus status);

    /**
     * Counts campaigns created by a specific user.
     * 
     * @param userId admin user identifier
     * @return count of campaigns created by the user
     */
    @Query("SELECT COUNT(c) FROM NotificationCampaign c WHERE c.createdBy.id = :userId")
    Long countByCreatedById(@Param("userId") Long userId);

    /**
     * Counts campaigns created after a specific date.
     * Used for statistics and analytics.
     * 
     * @param since date from which to count
     * @return count of campaigns created since the date
     */
    Long countByCreatedAtAfter(LocalDateTime since);

    /**
     * Finds campaign statistics by status.
     * Returns aggregated data for reporting.
     * 
     * @param since date from which to gather statistics
     * @return list of statistics [status, count]
     */
    @Query("SELECT c.status, COUNT(c) FROM NotificationCampaign c WHERE c.createdAt >= :since GROUP BY c.status")
    List<Object[]> findCampaignStatsSince(@Param("since") LocalDateTime since);

    /**
     * Finds campaign delivery statistics by scope type.
     * 
     * @param since date from which to gather statistics
     * @return list of statistics [scopeType, totalRecipients, sentCount, failedCount]
     */
    @Query("SELECT c.scopeType, SUM(c.totalRecipients), SUM(c.sentCount), SUM(c.failedCount) " +
           "FROM NotificationCampaign c WHERE c.createdAt >= :since " +
           "GROUP BY c.scopeType")
    List<Object[]> findDeliveryStatsByScopeTypeSince(@Param("since") LocalDateTime since);

    /**
     * Finds campaigns with failed deliveries.
     * Used for retry or investigation.
     * 
     * @param threshold minimum number of failures to consider
     * @param pageable pagination parameters
     * @return page of campaigns with failures above threshold
     */
    @Query("SELECT c FROM NotificationCampaign c WHERE c.failedCount >= :threshold ORDER BY c.failedCount DESC")
    Page<NotificationCampaign> findCampaignsWithFailures(@Param("threshold") Integer threshold, Pageable pageable);

    /**
     * Finds campaigns with low delivery rate.
     * Used for quality monitoring.
     * 
     * @param minRate minimum delivery rate percentage (0-100)
     * @param pageable pagination parameters
     * @return page of campaigns below the rate
     */
    @Query("SELECT c FROM NotificationCampaign c WHERE " +
           "c.totalRecipients > 0 AND " +
           "(c.sentCount * 100.0 / c.totalRecipients) < :minRate " +
           "ORDER BY (c.sentCount * 100.0 / c.totalRecipients) ASC")
    Page<NotificationCampaign> findCampaignsWithLowDeliveryRate(@Param("minRate") Double minRate, Pageable pageable);

    /**
     * Finds recent completed campaigns.
     * 
     * @param limit maximum number of campaigns to return
     * @return list of recent completed campaigns
     */
    @Query("SELECT c FROM NotificationCampaign c WHERE c.status = 'COMPLETED' ORDER BY c.createdAt DESC LIMIT :limit")
    List<NotificationCampaign> findRecentCompletedCampaigns(@Param("limit") Integer limit);

    /**
     * Calculates total recipients across all campaigns in a date range.
     * 
     * @param start start of date range
     * @param end end of date range
     * @return total number of recipients
     */
    @Query("SELECT SUM(c.totalRecipients) FROM NotificationCampaign c WHERE c.createdAt BETWEEN :start AND :end")
    Long calculateTotalRecipients(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Calculates successful deliveries across all campaigns in a date range.
     * 
     * @param start start of date range
     * @param end end of date range
     * @return total number of successful deliveries
     */
    @Query("SELECT SUM(c.sentCount) FROM NotificationCampaign c WHERE c.createdAt BETWEEN :start AND :end")
    Long calculateTotalSent(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * Checks if a campaign exists for a specific alert signal.
     * Used to avoid duplicate campaigns.
     * 
     * @param alertSignalId alert signal identifier
     * @return true if campaign exists for the signal
     */
    @Query("SELECT COUNT(c) > 0 FROM NotificationCampaign c WHERE c.alertSignal.id = :alertSignalId")
    boolean existsByAlertSignalId(@Param("alertSignalId") Long alertSignalId);
}
