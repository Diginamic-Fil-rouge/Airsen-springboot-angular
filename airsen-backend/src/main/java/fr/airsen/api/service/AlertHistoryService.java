package fr.airsen.api.service;

import fr.airsen.api.entity.AirQuality;
import fr.airsen.api.entity.Alert;
import fr.airsen.api.entity.AlertHistory;
import fr.airsen.api.entity.enums.AlertStatus;
import fr.airsen.api.repository.AirQualityRepository;
import fr.airsen.api.repository.AlertHistoryRepository;
import fr.airsen.api.repository.AlertSignalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing AlertHistory entities and alert delivery tracking.
 *
 * This service provides functionality for tracking alert delivery history,
 * managing delivery status updates, generating reports, and handling
 * failed delivery retry logic for the Airsens alert system.
 */
@Service
@Transactional
public class AlertHistoryService {

    private final AlertHistoryRepository alertHistoryRepository;
    private final AlertSignalRepository alertSignalRepository;
    private final AirQualityRepository airQualityRepository;

    /**
     * Constructor for AlertHistoryService.
     *
     * @param alertHistoryRepository alert history data access repository
     * @param alertSignalRepository alert data access repository
     * @param airQualityRepository air quality data access repository
     */
    @Autowired
    public AlertHistoryService(AlertHistoryRepository alertHistoryRepository,
                              AlertSignalRepository alertSignalRepository,
                              AirQualityRepository airQualityRepository) {
        this.alertHistoryRepository = alertHistoryRepository;
        this.alertSignalRepository = alertSignalRepository;
        this.airQualityRepository = airQualityRepository;
    }

    /**
     * Creates a new alert history record when an alert is triggered.
     *
     * @param alertId alert identifier
     * @param airQualityId air quality measurement identifier
     * @return created alert history record
     * @throws IllegalArgumentException if alert or air quality not found
     */
    public AlertHistory createAlertHistory(Long alertId, Integer airQualityId) {
        Alert alert = alertSignalRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found with id: " + alertId));

        AirQuality airQuality = airQualityRepository.findById(Long.valueOf(airQualityId))
            .orElseThrow(() -> new IllegalArgumentException("AirQuality not found with id: " + airQualityId));

        AlertHistory alertHistory = new AlertHistory(alert, airQuality);
        return alertHistoryRepository.save(alertHistory);
    }

    /**
     * Creates alert history with initial status.
     *
     * @param alertId alert identifier
     * @param airQualityId air quality measurement identifier
     * @param initialStatus initial delivery status
     * @return created alert history record
     */
    public AlertHistory createAlertHistory(Long alertId, Long airQualityId, AlertStatus initialStatus) {
        Alert alert = alertSignalRepository.findById(alertId)
            .orElseThrow(() -> new IllegalArgumentException("Alert not found with id: " + alertId));

        AirQuality airQuality = airQualityRepository.findById(Long.valueOf(airQualityId))
            .orElseThrow(() -> new IllegalArgumentException("AirQuality not found with id: " + airQualityId));

        AlertHistory alertHistory = new AlertHistory(alert, airQuality, initialStatus);
        return alertHistoryRepository.save(alertHistory);
    }

    /**
     * Retrieves alert history for a specific alert.
     *
     * @param alertId alert identifier
     * @param pageable pagination parameters
     * @return page of alert history records
     */
    @Transactional(readOnly = true)
    public Page<AlertHistory> getAlertHistoryByAlertId(Long alertId, Pageable pageable) {
        return alertHistoryRepository.findByAlertId(alertId, pageable);
    }

    /**
     * Retrieves alert history for a specific user.
     *
     * @param userId user identifier
     * @param pageable pagination parameters
     * @return page of alert history records
     */
    @Transactional(readOnly = true)
    public Page<AlertHistory> getAlertHistoryByUserId(Long userId, Pageable pageable) {
        return alertHistoryRepository.findByUserId(userId, pageable);
    }

    /**
     * Retrieves alert history by delivery status.
     *
     * @param status delivery status
     * @param pageable pagination parameters
     * @return page of alert history records with specified status
     */
    @Transactional(readOnly = true)
    public Page<AlertHistory> getAlertHistoryByStatus(AlertStatus status, Pageable pageable) {
        return alertHistoryRepository.findByStatus(status, pageable);
    }

    /**
     * Retrieves alert history within a date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination parameters
     * @return page of alert history records
     */
    @Transactional(readOnly = true)
    public Page<AlertHistory> getAlertHistoryByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return alertHistoryRepository.findByDateRange(startDate, endDate, pageable);
    }

    /**
     * Retrieves recent alert history for a user.
     *
     * @param userId user identifier
     * @param since date from which to find history
     * @return list of recent alert history
     */
    @Transactional(readOnly = true)
    public List<AlertHistory> getRecentAlertHistoryByUserId(Long userId, LocalDateTime since) {
        return alertHistoryRepository.findRecentByUserId(userId, since);
    }

    /**
     * Marks an alert history record as successfully sent.
     *
     * @param historyId alert history identifier
     * @return updated alert history record
     */
    public AlertHistory markAsSent(Long historyId) {
        AlertHistory alertHistory = getAlertHistoryById(historyId);
        alertHistory.markAsSent();
        return alertHistoryRepository.save(alertHistory);
    }

    /**
     * Marks an alert history record as failed with error message.
     *
     * @param historyId alert history identifier
     * @param errorMessage error description
     * @return updated alert history record
     */
    public AlertHistory markAsFailed(Long historyId, String errorMessage) {
        AlertHistory alertHistory = getAlertHistoryById(historyId);
        alertHistory.markAsFailed(errorMessage);
        return alertHistoryRepository.save(alertHistory);
    }

    /**
     * Marks an alert history record as pending.
     *
     * @param historyId alert history identifier
     * @return updated alert history record
     */
    public AlertHistory markAsPending(Long historyId) {
        AlertHistory alertHistory = getAlertHistoryById(historyId);
        alertHistory.markAsPending();
        return alertHistoryRepository.save(alertHistory);
    }

    /**
     * Updates the status of an alert history record.
     *
     * @param historyId alert history identifier
     * @param status new delivery status
     * @return updated alert history record
     */
    public AlertHistory updateStatus(Long historyId, AlertStatus status) {
        AlertHistory alertHistory = getAlertHistoryById(historyId);
        alertHistory.setStatus(status);
        return alertHistoryRepository.save(alertHistory);
    }

    /**
     * Updates status with error message.
     *
     * @param historyId alert history identifier
     * @param status new delivery status
     * @param errorMessage error description
     * @return updated alert history record
     */
    public AlertHistory updateStatusWithError(Long historyId, AlertStatus status, String errorMessage) {
        AlertHistory alertHistory = getAlertHistoryById(historyId);
        alertHistory.setStatus(status);
        alertHistory.setErrorMessage(errorMessage);
        return alertHistoryRepository.save(alertHistory);
    }

    /**
     * Gets failed alert deliveries for retry processing.
     *
     * @return list of failed alert deliveries
     */
    @Transactional(readOnly = true)
    public List<AlertHistory> getFailedDeliveries() {
        return alertHistoryRepository.findFailedDeliveries();
    }

    /**
     * Gets pending alert deliveries for processing.
     *
     * @return list of pending alert deliveries
     */
    @Transactional(readOnly = true)
    public List<AlertHistory> getPendingDeliveries() {
        return alertHistoryRepository.findPendingDeliveries();
    }

    /**
     * Gets delivery statistics for a user within a date range.
     *
     * @param userId user identifier
     * @param startDate start of date range
     * @param endDate end of date range
     * @return delivery statistics
     */
    @Transactional(readOnly = true)
    public DeliveryStatistics getUserDeliveryStatistics(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> stats = alertHistoryRepository.findDeliveryStatsByUserAndDateRange(userId, startDate, endDate);

        long totalDeliveries = 0;
        long successfulDeliveries = 0;
        long failedDeliveries = 0;
        long pendingDeliveries = 0;

        for (Object[] stat : stats) {
            AlertStatus status = (AlertStatus) stat[0];
            Long count = (Long) stat[1];

            totalDeliveries += count;
            switch (status) {
                case SENT -> successfulDeliveries += count;
                case FAILED -> failedDeliveries += count;
                case PENDING -> pendingDeliveries += count;
            }
        }

        return new DeliveryStatistics(totalDeliveries, successfulDeliveries, failedDeliveries, pendingDeliveries);
    }

    /**
     * Gets system-wide delivery statistics within a date range.
     *
     * @param startDate start of date range
     * @param endDate end of date range
     * @return system delivery statistics
     */
    @Transactional(readOnly = true)
    public DeliveryStatistics getSystemDeliveryStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> stats = alertHistoryRepository.findSystemDeliveryStats(startDate, endDate);

        long totalDeliveries = 0;
        long successfulDeliveries = 0;
        long failedDeliveries = 0;
        long pendingDeliveries = 0;

        for (Object[] stat : stats) {
            AlertStatus status = (AlertStatus) stat[0];
            Long count = (Long) stat[1];

            totalDeliveries += count;
            switch (status) {
                case SENT -> successfulDeliveries += count;
                case FAILED -> failedDeliveries += count;
                case PENDING -> pendingDeliveries += count;
            }
        }

        return new DeliveryStatistics(totalDeliveries, successfulDeliveries, failedDeliveries, pendingDeliveries);
    }

    /**
     * Counts alert deliveries by status for a user.
     *
     * @param userId user identifier
     * @param status delivery status
     * @return count of deliveries with specified status
     */
    @Transactional(readOnly = true)
    public long countByUserIdAndStatus(Long userId, AlertStatus status) {
        return alertHistoryRepository.countByUserIdAndStatus(userId, status);
    }

    /**
     * Deletes old alert history records before a specified date.
     *
     * @param cutoffDate date before which records should be deleted
     * @return number of deleted records
     */
    public int deleteOldAlertHistory(LocalDateTime cutoffDate) {
        return alertHistoryRepository.deleteByDateBefore(cutoffDate);
    }

    /**
     * Gets alert history for a specific air quality measurement.
     *
     * @param airQualityId air quality measurement identifier
     * @return list of alert history records
     */
    @Transactional(readOnly = true)
    public List<AlertHistory> getAlertHistoryByAirQualityId(Long airQualityId) {
        return alertHistoryRepository.findByAirQualityId(airQualityId);
    }

    /**
     * Gets alert history by ID.
     *
     * @param historyId alert history identifier
     * @return alert history record
     * @throws IllegalArgumentException if not found
     */
    @Transactional(readOnly = true)
    public AlertHistory getAlertHistoryById(Long historyId) {
        return alertHistoryRepository.findById(historyId)
            .orElseThrow(() -> new IllegalArgumentException("AlertHistory not found with id: " + historyId));
    }

    /**
     * Inner class for delivery statistics.
     */
    public static class DeliveryStatistics {
        private final long totalDeliveries;
        private final long successfulDeliveries;
        private final long failedDeliveries;
        private final long pendingDeliveries;

        public DeliveryStatistics(long totalDeliveries, long successfulDeliveries,
                                 long failedDeliveries, long pendingDeliveries) {
            this.totalDeliveries = totalDeliveries;
            this.successfulDeliveries = successfulDeliveries;
            this.failedDeliveries = failedDeliveries;
            this.pendingDeliveries = pendingDeliveries;
        }

        public long getTotalDeliveries() { return totalDeliveries; }
        public long getSuccessfulDeliveries() { return successfulDeliveries; }
        public long getFailedDeliveries() { return failedDeliveries; }
        public long getPendingDeliveries() { return pendingDeliveries; }

        public double getSuccessRate() {
            return totalDeliveries > 0 ? (double) successfulDeliveries / totalDeliveries : 0.0;
        }

        public double getFailureRate() {
            return totalDeliveries > 0 ? (double) failedDeliveries / totalDeliveries : 0.0;
        }
    }
}
