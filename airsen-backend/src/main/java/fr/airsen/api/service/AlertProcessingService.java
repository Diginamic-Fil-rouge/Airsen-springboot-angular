package fr.airsen.api.service;

import fr.airsen.api.entity.Alert;
import fr.airsen.api.entity.AlertHistory;
import fr.airsen.api.entity.AirQuality;
import fr.airsen.api.entity.enums.AlertStatus;
import fr.airsen.api.entity.enums.Pollutant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Service for automated alert processing and threshold monitoring.
 * 
 * This service provides background processing functionality for monitoring
 * new air quality data, detecting threshold violations, triggering alerts,
 * and coordinating with notification services for the Airsens alert system.
 */
@Service
@Transactional
public class AlertProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AlertProcessingService.class);

    private final AlertService alertService;
    private final AlertHistoryService alertHistoryService;
    private final NotificationService notificationService;

    /**
     * Constructor for AlertProcessingService.
     * 
     * @param alertService alert management service
     * @param alertHistoryService alert history tracking service
     * @param notificationService notification delivery service
     */
    @Autowired
    public AlertProcessingService(AlertService alertService,
                                 AlertHistoryService alertHistoryService,
                                 NotificationService notificationService) {
        this.alertService = alertService;
        this.alertHistoryService = alertHistoryService;
        this.notificationService = notificationService;
    }

    /**
     * Processes new air quality data update for alerts (alias for external integration).
     * 
     * @param airQuality new air quality measurement
     * @return number of alerts triggered
     */
    public int processAirQualityUpdate(AirQuality airQuality) {
        return processAirQualityForAlerts(airQuality);
    }

    /**
     * Processes new air quality data to check for alert thresholds.
     * 
     * @param airQuality new air quality measurement
     * @return number of alerts triggered
     */
    public int processAirQualityForAlerts(AirQuality airQuality) {
        logger.debug("Processing air quality data for commune {} on {}", 
                    airQuality.getCommune().getId(), airQuality.getMeasurementDate());

        int alertsTriggered = 0;
        Long communeId = airQuality.getCommune().getId();

        // Check each pollutant value against alerts
        alertsTriggered += checkPollutantAlerts(communeId, Pollutant.NO2, 
                                              BigDecimal.valueOf(airQuality.getNO2()), airQuality);
        alertsTriggered += checkPollutantAlerts(communeId, Pollutant.O3, 
                                              BigDecimal.valueOf(airQuality.getO3()), airQuality);
        alertsTriggered += checkPollutantAlerts(communeId, Pollutant.PM10, 
                                              BigDecimal.valueOf(airQuality.getPm10()), airQuality);
        alertsTriggered += checkPollutantAlerts(communeId, Pollutant.PM25, 
                                              BigDecimal.valueOf(airQuality.getPm25()), airQuality);
        alertsTriggered += checkPollutantAlerts(communeId, Pollutant.SO2, 
                                              BigDecimal.valueOf(airQuality.getSO2()), airQuality);

        if (alertsTriggered > 0) {
            logger.info("Triggered {} alerts for commune {} air quality data", 
                       alertsTriggered, communeId);
        }

        return alertsTriggered;
    }

    /**
     * Checks alerts for a specific pollutant and value.
     * 
     * @param communeId commune identifier
     * @param pollutant pollutant type
     * @param currentValue current pollutant measurement
     * @param airQuality air quality measurement that triggered alerts
     * @return number of alerts triggered
     */
    private int checkPollutantAlerts(Long communeId, Pollutant pollutant, 
                                   BigDecimal currentValue, AirQuality airQuality) {
        List<Alert> triggeredAlerts = alertService.findTriggeredAlerts(communeId, pollutant, currentValue);
        
        int alertsTriggered = 0;
        for (Alert alert : triggeredAlerts) {
            try {
                triggerAlert(alert, airQuality, currentValue);
                alertsTriggered++;
            } catch (Exception e) {
                logger.error("Error triggering alert {}: {}", alert.getId(), e.getMessage(), e);
            }
        }

        return alertsTriggered;
    }

    /**
     * Triggers a specific alert and creates notification.
     * 
     * @param alert alert to trigger
     * @param airQuality air quality measurement that triggered the alert
     * @param currentValue current pollutant value
     */
    private void triggerAlert(Alert alert, AirQuality airQuality, BigDecimal currentValue) {
        logger.debug("Triggering alert {} for user {} - {} exceeds threshold {}", 
                    alert.getId(), alert.getUser().getId(), currentValue, alert.getThresholdValue());

        // Create alert history record
        AlertHistory alertHistory = alertHistoryService.createAlertHistory(
            alert.getId(), airQuality.getId(), AlertStatus.PENDING);

        // Create notification content
        String title = createAlertNotificationTitle(alert, currentValue);
        String message = createAlertNotificationMessage(alert, airQuality, currentValue);

        // Create notification
        Long notificationId = notificationService.createSystemNotification(
            alert.getUser().getId(), title, message).getId();

        // Send notification asynchronously
        sendAlertNotificationAsync(alertHistory.getId(), notificationId);
    }

    /**
     * Sends alert notification asynchronously.
     * 
     * @param alertHistoryId alert history identifier
     * @param notificationId notification identifier
     * @return future with sending result
     */
    @Async
    public CompletableFuture<Boolean> sendAlertNotificationAsync(Long alertHistoryId, Long notificationId) {
        try {
            // Send email notification
            CompletableFuture<Boolean> emailResult = notificationService.sendEmailNotificationAsync(notificationId);
            
            boolean success = emailResult.get(); // Wait for email sending result
            
            if (success) {
                alertHistoryService.markAsSent(alertHistoryId);
                logger.info("Alert notification sent successfully: history={}, notification={}", 
                           alertHistoryId, notificationId);
            } else {
                alertHistoryService.markAsFailed(alertHistoryId, "Email notification delivery failed");
                logger.warn("Alert notification failed: history={}, notification={}", 
                           alertHistoryId, notificationId);
            }
            
            return CompletableFuture.completedFuture(success);
            
        } catch (Exception e) {
            String error = "Failed to send alert notification: " + e.getMessage();
            alertHistoryService.markAsFailed(alertHistoryId, error);
            logger.error("Error sending alert notification: history={}, error={}", 
                        alertHistoryId, e.getMessage(), e);
            
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Batch processes multiple air quality measurements.
     * 
     * @param airQualityMeasurements list of air quality measurements
     * @return total number of alerts triggered
     */
    public int processBatchAirQuality(List<AirQuality> airQualityMeasurements) {
        logger.info("Processing batch of {} air quality measurements", airQualityMeasurements.size());
        
        int totalAlertsTriggered = 0;
        for (AirQuality airQuality : airQualityMeasurements) {
            try {
                totalAlertsTriggered += processAirQualityForAlerts(airQuality);
            } catch (Exception e) {
                logger.error("Error processing air quality measurement {}: {}", 
                           airQuality.getId(), e.getMessage(), e);
            }
        }
        
        logger.info("Batch processing completed: {} alerts triggered from {} measurements", 
                   totalAlertsTriggered, airQualityMeasurements.size());
        
        return totalAlertsTriggered;
    }

    /**
     * Scheduled task to process pending alert deliveries.
     * Runs every 5 minutes to retry failed deliveries.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void processPendingAlertDeliveries() {
        try {
            List<AlertHistory> pendingDeliveries = alertHistoryService.getPendingDeliveries();
            
            if (!pendingDeliveries.isEmpty()) {
                logger.info("Processing {} pending alert deliveries", pendingDeliveries.size());
                
                for (AlertHistory alertHistory : pendingDeliveries) {
                    try {
                        // Create new notification for pending delivery
                        Alert alert = alertHistory.getAlert();
                        AirQuality airQuality = alertHistory.getAirQuality();
                        
                        String title = "Air Quality Alert - " + alert.getPollutant().getDisplayName();
                        String message = String.format("Alert for %s in %s - threshold exceeded",
                            alert.getPollutant().getDisplayName(), 
                            alert.getCommune().getName());
                        
                        Long notificationId = notificationService.createSystemNotification(
                            alert.getUser().getId(), title, message).getId();
                        
                        sendAlertNotificationAsync(alertHistory.getId(), notificationId);
                        
                    } catch (Exception e) {
                        logger.error("Error processing pending alert delivery {}: {}", 
                                   alertHistory.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in scheduled pending alert delivery processing: {}", e.getMessage(), e);
        }
    }

    /**
     * Scheduled task to retry failed alert deliveries.
     * Runs every hour to retry failed deliveries.
     */
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void retryFailedAlertDeliveries() {
        try {
            List<AlertHistory> failedDeliveries = alertHistoryService.getFailedDeliveries();
            
            if (!failedDeliveries.isEmpty()) {
                logger.info("Retrying {} failed alert deliveries", failedDeliveries.size());
                
                for (AlertHistory alertHistory : failedDeliveries) {
                    try {
                        // Reset to pending and retry
                        alertHistoryService.markAsPending(alertHistory.getId());
                        
                        // Create retry notification
                        Alert alert = alertHistory.getAlert();
                        String title = "Air Quality Alert (Retry) - " + alert.getPollutant().getDisplayName();
                        String message = "This is a retry of a previously failed alert notification.";
                        
                        Long notificationId = notificationService.createSystemNotification(
                            alert.getUser().getId(), title, message).getId();
                        
                        sendAlertNotificationAsync(alertHistory.getId(), notificationId);
                        
                    } catch (Exception e) {
                        logger.error("Error retrying failed alert delivery {}: {}", 
                                   alertHistory.getId(), e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error in scheduled failed alert delivery retry: {}", e.getMessage(), e);
        }
    }

    /**
     * Creates alert notification title.
     * 
     * @param alert triggered alert
     * @param currentValue current pollutant value
     * @return notification title
     */
    private String createAlertNotificationTitle(Alert alert, BigDecimal currentValue) {
        return String.format("Air Quality Alert: %s level exceeded in %s",
            alert.getPollutant().getDisplayName(),
            alert.getCommune().getName());
    }

    /**
     * Creates alert notification message.
     * 
     * @param alert triggered alert
     * @param airQuality air quality measurement
     * @param currentValue current pollutant value
     * @return notification message
     */
    private String createAlertNotificationMessage(Alert alert, AirQuality airQuality, BigDecimal currentValue) {
        return String.format("""
            Air Quality Alert - Threshold Exceeded
            
            Location: %s
            Pollutant: %s
            Current Level: %.2f %s
            Your Threshold: %.2f %s
            Measurement Date: %s
            
            The air quality in your monitored area has exceeded your configured threshold.
            Please take appropriate precautions.
            
            This is an automated alert from the Airsens Air Quality Monitoring System.
            """,
            alert.getCommune().getName(),
            alert.getPollutant().getDisplayName(),
            currentValue,
            alert.getPollutant().getUnit(),
            alert.getThresholdValue(),
            alert.getPollutant().getUnit(),
            airQuality.getMeasurementDate()
        );
    }

    /**
     * Gets processing statistics.
     * 
     * @return processing statistics
     */
    @Transactional(readOnly = true)
    public ProcessingStatistics getProcessingStatistics() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        
        List<Alert> recentActiveAlerts = alertService.getActiveAlertsSince(last24Hours);
        List<AlertHistory> recentHistory = alertHistoryService.getPendingDeliveries();
        
        return new ProcessingStatistics(
            recentActiveAlerts.size(),
            recentHistory.size(),
            alertHistoryService.getFailedDeliveries().size()
        );
    }

    /**
     * Inner class for processing statistics.
     */
    public static class ProcessingStatistics {
        private final long recentActiveAlerts;
        private final long pendingDeliveries;
        private final long failedDeliveries;

        public ProcessingStatistics(long recentActiveAlerts, long pendingDeliveries, long failedDeliveries) {
            this.recentActiveAlerts = recentActiveAlerts;
            this.pendingDeliveries = pendingDeliveries;
            this.failedDeliveries = failedDeliveries;
        }

        public long getRecentActiveAlerts() { return recentActiveAlerts; }
        public long getPendingDeliveries() { return pendingDeliveries; }
        public long getFailedDeliveries() { return failedDeliveries; }
    }
}