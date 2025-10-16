package fr.airsen.api.service;

import fr.airsen.api.entity.Alert;
import fr.airsen.api.entity.Commune;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.NotificationType;
import fr.airsen.api.entity.enums.Pollutant;
import fr.airsen.api.repository.AlertRepository;
import fr.airsen.api.repository.CommuneRepository;
import fr.airsen.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing Alert entities and alert-related business logic.
 * 
 * This service provides comprehensive alert management functionality including
 * CRUD operations, threshold checking, activation management, and business rule enforcement
 * for the Airsens air quality monitoring system.
 */
@Service
@Transactional
public class AlertService {

    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final CommuneRepository communeRepository;

    @Value("${airsen.alert.max-alerts-per-user:10}")
    private int maxAlertsPerUser;

    /**
     * Constructor for AlertService.
     * 
     * @param alertRepository alert data access repository
     * @param userRepository user data access repository
     * @param communeRepository commune data access repository
     */
    @Autowired
    public AlertService(AlertRepository alertRepository, 
                       UserRepository userRepository,
                       CommuneRepository communeRepository) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.communeRepository = communeRepository;
    }

    /**
     * Creates a new alert for a user.
     * 
     * @param userId user identifier
     * @param communeId commune identifier  
     * @param pollutant pollutant to monitor
     * @param thresholdValue threshold for alert triggering
     * @param notificationType notification delivery method
     * @return created alert
     * @throws IllegalArgumentException if user/commune not found or business rules violated
     */
    public Alert createAlert(Long userId, Long communeId, Pollutant pollutant,
                           BigDecimal thresholdValue, NotificationType notificationType) {
        
        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Validate commune exists
        Commune commune = communeRepository.findById((long) communeId.intValue())
            .orElseThrow(() -> new IllegalArgumentException("Commune not found with id: " + communeId));

        // Check if user has reached alert limit
        if (hasUserReachedAlertLimit(userId)) {
            throw new IllegalArgumentException("User has reached maximum number of alerts: " + maxAlertsPerUser);
        }

        // Check for duplicate alert
        Optional<Alert> existingAlert = alertRepository.findByUserIdAndCommuneIdAndPollutant(
            userId, communeId, pollutant);
        if (existingAlert.isPresent()) {
            throw new IllegalArgumentException("Alert already exists for this user, commune, and pollutant combination");
        }

        // Validate threshold value
        validateThresholdValue(pollutant, thresholdValue);

        // Create and save alert
        Alert alert = new Alert(user, commune, pollutant, thresholdValue, notificationType);
        return alertRepository.save(alert);
    }

    /**
     * Retrieves active alerts for a specific user.
     * 
     * @param userId user identifier
     * @param pageable pagination parameters
     * @return page of active alerts
     */
    @Transactional(readOnly = true)
    public Page<Alert> getActiveAlertsByUserId(Long userId, Pageable pageable) {
        return alertRepository.findActiveAlertsByUserId(userId, pageable);
    }

    /**
     * Retrieves all alerts for a specific user.
     * 
     * @param userId user identifier
     * @param pageable pagination parameters
     * @return page of all alerts
     */
    @Transactional(readOnly = true)
    public Page<Alert> getAllAlertsByUserId(Long userId, Pageable pageable) {
        return alertRepository.findAllAlertsByUserId(userId, pageable);
    }

    /**
     * Retrieves a specific alert by ID.
     * 
     * @param alertId alert identifier
     * @return optional alert
     */
    @Transactional(readOnly = true)
    public Optional<Alert> getAlertById(Long alertId) {
        return alertRepository.findById(alertId);
    }

    /**
     * Updates an existing alert.
     * 
     * @param alertId alert identifier
     * @param thresholdValue new threshold value
     * @param notificationType new notification type
     * @return updated alert
     * @throws IllegalArgumentException if alert not found
     */
    public Alert updateAlert(Long alertId, BigDecimal thresholdValue, NotificationType notificationType) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found with id: " + alertId));

        if (thresholdValue != null) {
            validateThresholdValue(alert.getPollutant(), thresholdValue);
            alert.updateThreshold(thresholdValue);
        }

        if (notificationType != null) {
            alert.updateNotificationType(notificationType);
        }

        return alertRepository.save(alert);
    }

    /**
     * Updates only the threshold value for an alert.
     * 
     * @param alertId alert identifier
     * @param newThreshold new threshold value
     * @return updated alert
     */
    public Alert updateAlertThreshold(Long alertId, BigDecimal newThreshold) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found with id: " + alertId));

        validateThresholdValue(alert.getPollutant(), newThreshold);
        alert.updateThreshold(newThreshold);
        
        return alertRepository.save(alert);
    }

    /**
     * Activates an alert.
     * 
     * @param alertId alert identifier
     * @return activated alert
     */
    public Alert activateAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found with id: " + alertId));

        alert.activate();
        return alertRepository.save(alert);
    }

    /**
     * Deactivates an alert.
     * 
     * @param alertId alert identifier
     * @return deactivated alert
     */
    public Alert deactivateAlert(Long alertId) {
        Alert alert = alertRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found with id: " + alertId));

        alert.deactivate();
        return alertRepository.save(alert);
    }

    /**
     * Deletes an alert.
     * 
     * @param alertId alert identifier
     * @throws IllegalArgumentException if alert not found
     */
    public void deleteAlert(Long alertId) {
        if (!alertRepository.existsById(alertId)) {
            throw new IllegalArgumentException("Alert not found with id: " + alertId);
        }
        alertRepository.deleteById(alertId);
    }

    /**
     * Finds active alerts that should be triggered by current pollutant values.
     * 
     * @param communeId commune identifier
     * @param pollutant pollutant type
     * @param currentValue current pollutant measurement
     * @return list of alerts to trigger
     */
    @Transactional(readOnly = true)
    public List<Alert> findTriggeredAlerts(Long communeId, Pollutant pollutant, BigDecimal currentValue) {
        return alertRepository.findTriggeredAlerts(communeId, pollutant, currentValue);
    }

    /**
     * Gets alert statistics for a user.
     * 
     * @param userId user identifier
     * @return alert count information
     */
    @Transactional(readOnly = true)
    public AlertStatistics getUserAlertStatistics(Long userId) {
        long activeAlerts = alertRepository.countActiveAlertsByUserId(userId);
        // Count all alerts for the user (simple count query)
        long totalAlerts = alertRepository.countByUserId(userId);
        
        return new AlertStatistics(activeAlerts, totalAlerts, maxAlertsPerUser);
    }

    /**
     * Checks if a user has reached the maximum number of alerts.
     * 
     * @param userId user identifier
     * @return true if user has reached the limit
     */
    @Transactional(readOnly = true)
    public boolean hasUserReachedAlertLimit(Long userId) {
        return alertRepository.hasUserReachedAlertLimit(userId, maxAlertsPerUser);
    }

    /**
     * Deactivates all alerts for a user.
     * Used when user account is deactivated.
     * 
     * @param userId user identifier
     */
    public void deactivateAllUserAlerts(Long userId) {
        alertRepository.deactivateAllUserAlerts(userId);
    }

    /**
     * Gets all active alerts across the system.
     * Used for system-wide monitoring.
     * 
     * @return list of all active alerts
     */
    @Transactional(readOnly = true)
    public List<Alert> getAllActiveAlerts() {
        return alertRepository.findAllActiveAlerts();
    }

    /**
     * Gets recent active alerts since a specific date.
     * 
     * @param since date from which to find alerts
     * @return list of recent active alerts
     */
    @Transactional(readOnly = true)
    public List<Alert> getActiveAlertsSince(LocalDateTime since) {
        return alertRepository.findActiveAlertsSince(since);
    }

    /**
     * Validates threshold value for a specific pollutant type.
     * 
     * @param pollutant pollutant type
     * @param thresholdValue threshold value to validate
     * @throws IllegalArgumentException if threshold is invalid
     */
    private void validateThresholdValue(Pollutant pollutant, BigDecimal thresholdValue) {
        if (thresholdValue == null || thresholdValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Threshold value must be positive");
        }

        // Define reasonable maximum values for each pollutant type
        BigDecimal maxValue = switch (pollutant) {
            case NO2 -> new BigDecimal("1000"); // µg/m³
            case O3 -> new BigDecimal("500");   // µg/m³
            case PM10 -> new BigDecimal("500"); // µg/m³
            case PM25 -> new BigDecimal("300"); // µg/m³
            case SO2 -> new BigDecimal("1000"); // µg/m³
        };

        if (thresholdValue.compareTo(maxValue) > 0) {
            throw new IllegalArgumentException(
                "Threshold value too high for " + pollutant.getDisplayName() + 
                ". Maximum allowed: " + maxValue + " " + pollutant.getUnit());
        }
    }

    /**
     * Inner class for alert statistics.
     */
    public static class AlertStatistics {
        private final long activeAlerts;
        private final long totalAlerts;
        private final int maxAllowedAlerts;

        public AlertStatistics(long activeAlerts, long totalAlerts, int maxAllowedAlerts) {
            this.activeAlerts = activeAlerts;
            this.totalAlerts = totalAlerts;
            this.maxAllowedAlerts = maxAllowedAlerts;
        }

        public long getActiveAlerts() { return activeAlerts; }
        public long getTotalAlerts() { return totalAlerts; }
        public int getMaxAllowedAlerts() { return maxAllowedAlerts; }
        public boolean isAtLimit() { return activeAlerts >= maxAllowedAlerts; }
    }
}