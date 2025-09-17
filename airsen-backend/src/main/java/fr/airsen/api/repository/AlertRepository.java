package fr.airsen.api.repository;

import fr.airsen.api.entity.Alert;
import fr.airsen.api.entity.enums.Pollutant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for managing Alert entities.
 * 
 * Provides data access methods for user-defined air quality alerts
 * with custom queries for threshold monitoring and alert management
 * according to Airsens data model specifications.
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    /**
     * Finds all active alerts for a specific user.
     * 
     * @param userId user identifier
     * @param pageable pagination parameters
     * @return page of active alerts for the user
     */
    @Query("SELECT a FROM Alert a WHERE a.user.id = :userId AND a.active = true ORDER BY a.createdDate DESC")
    Page<Alert> findActiveAlertsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Finds all active alerts for a specific commune and pollutant.
     * Used for threshold checking when new air quality data arrives.
     * 
     * @param communeId commune identifier
     * @param pollutant pollutant type
     * @return list of active alerts to check
     */
    @Query("SELECT a FROM Alert a WHERE a.commune.id = :communeId AND a.pollutant = :pollutant AND a.active = true")
    List<Alert> findActiveAlertsByCommuneAndPollutant(@Param("communeId") Long communeId, @Param("pollutant") Pollutant pollutant);

    /**
     * Finds active alerts that should be triggered by a specific pollutant value.
     * 
     * @param communeId commune identifier
     * @param pollutant pollutant type
     * @param currentValue current pollutant measurement
     * @return list of alerts that should be triggered
     */
    @Query("SELECT a FROM Alert a WHERE a.commune.id = :communeId AND a.pollutant = :pollutant AND a.active = true AND a.thresholdValue < :currentValue")
    List<Alert> findTriggeredAlerts(@Param("communeId") Long communeId, @Param("pollutant") Pollutant pollutant, @Param("currentValue") BigDecimal currentValue);

    /**
     * Finds all alerts for a specific commune.
     * 
     * @param communeId commune identifier
     * @param pageable pagination parameters
     * @return page of alerts for the commune
     */
    Page<Alert> findByCommuneId(Long communeId, Pageable pageable);

    /**
     * Finds alerts by user and commune.
     * 
     * @param userId user identifier
     * @param communeId commune identifier
     * @return list of alerts for the user in the specific commune
     */
    List<Alert> findByUserIdAndCommuneId(Long userId, Long communeId);

    /**
     * Finds alerts by user, commune, and pollutant.
     * Used to prevent duplicate alerts for the same combination.
     * 
     * @param userId user identifier
     * @param communeId commune identifier
     * @param pollutant pollutant type
     * @return optional alert if exists
     */
    Optional<Alert> findByUserIdAndCommuneIdAndPollutant(Long userId, Long communeId, Pollutant pollutant);

    /**
     * Counts active alerts for a user.
     * 
     * @param userId user identifier
     * @return number of active alerts
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.user.id = :userId AND a.active = true")
    long countActiveAlertsByUserId(@Param("userId") Long userId);

    /**
     * Counts all alerts for a commune.
     * 
     * @param communeId commune identifier
     * @return number of alerts for the commune
     */
    long countByCommuneId(Long communeId);

    /**
     * Activates an alert.
     * 
     * @param alertId alert identifier
     */
    @Modifying
    @Query("UPDATE Alert a SET a.active = true WHERE a.id = :alertId")
    void activateAlert(@Param("alertId") Long alertId);

    /**
     * Deactivates an alert.
     * 
     * @param alertId alert identifier
     */
    @Modifying
    @Query("UPDATE Alert a SET a.active = false WHERE a.id = :alertId")
    void deactivateAlert(@Param("alertId") Long alertId);

    /**
     * Updates the threshold value for an alert.
     * 
     * @param alertId alert identifier
     * @param newThreshold new threshold value
     */
    @Modifying
    @Query("UPDATE Alert a SET a.thresholdValue = :newThreshold WHERE a.id = :alertId")
    void updateThreshold(@Param("alertId") Long alertId, @Param("newThreshold") BigDecimal newThreshold);

    /**
     * Deactivates all alerts for a user.
     * Used when user account is deactivated.
     * 
     * @param userId user identifier
     */
    @Modifying
    @Query("UPDATE Alert a SET a.active = false WHERE a.user.id = :userId")
    void deactivateAllUserAlerts(@Param("userId") Long userId);

    /**
     * Deletes alerts created before a certain date.
     * Used for data retention policies.
     * 
     * @param cutoffDate date before which alerts should be deleted
     * @return number of deleted alerts
     */
    @Modifying
    @Query("DELETE FROM Alert a WHERE a.createdDate < :cutoffDate")
    int deleteByCreatedDateBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Finds all active alerts across the system.
     * Used for system-wide alert monitoring.
     * 
     * @return list of all active alerts
     */
    @Query("SELECT a FROM Alert a WHERE a.active = true")
    List<Alert> findAllActiveAlerts();

    /**
     * Finds active alerts created after a specific date.
     * Used for monitoring recent alert activity.
     * 
     * @param since date from which to find alerts
     * @return list of recent active alerts
     */
    @Query("SELECT a FROM Alert a WHERE a.active = true AND a.createdDate > :since ORDER BY a.createdDate DESC")
    List<Alert> findActiveAlertsSince(@Param("since") LocalDateTime since);

    /**
     * Checks if a user has reached the maximum number of alerts.
     * Business rule: users can have a maximum number of active alerts.
     * 
     * @param userId user identifier
     * @param maxAlerts maximum allowed alerts per user
     * @return true if user has reached the limit
     */
    @Query("SELECT COUNT(a) >= :maxAlerts FROM Alert a WHERE a.user.id = :userId AND a.active = true")
    boolean hasUserReachedAlertLimit(@Param("userId") Long userId, @Param("maxAlerts") long maxAlerts);
}