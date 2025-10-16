package fr.airsen.api.dto.response;

import fr.airsen.api.entity.enums.AlertStatus;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for AlertHistory entity responses.
 * 
 * This DTO represents alert delivery history information for API responses,
 * including delivery status and timing details.
 */
public class AlertHistoryDTO {

    private Long id;

    private Long alertId;

    private Long userId;

    private String userEmail;

    private Long airQualityId;

    private LocalDateTime sendDate;

    private AlertStatus status;

    private String statusDisplayName;

    private String errorMessage;

    private AlertSummaryDTO alertSummary;


    public AlertHistoryDTO() {}

    public AlertHistoryDTO(Long id, Long alertId, Long userId, String userEmail,
                          Long airQualityId, LocalDateTime sendDate, AlertStatus status,
                          String errorMessage) {
        this.id = id;
        this.alertId = alertId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.airQualityId = airQualityId;
        this.sendDate = sendDate;
        this.status = status;
        this.statusDisplayName = status != null ? status.getDisplayName() : null;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAlertId() {
        return alertId;
    }

    public void setAlertId(Long alertId) {
        this.alertId = alertId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public Long getAirQualityId() {
        return airQualityId;
    }

    public void setAirQualityId(Long airQualityId) {
        this.airQualityId = airQualityId;
    }

    public LocalDateTime getSendDate() {
        return sendDate;
    }

    public void setSendDate(LocalDateTime sendDate) {
        this.sendDate = sendDate;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
        this.statusDisplayName = status != null ? status.getDisplayName() : null;
    }

    public String getStatusDisplayName() {
        return statusDisplayName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public AlertSummaryDTO getAlertSummary() {
        return alertSummary;
    }

    public void setAlertSummary(AlertSummaryDTO alertSummary) {
        this.alertSummary = alertSummary;
    }

    /**
     * Checks if the delivery was successful.
     * 
     * @return true if status is SENT
     */
    public boolean wasSuccessful() {
        return status == AlertStatus.SENT;
    }

    /**
     * Checks if the delivery failed.
     * 
     * @return true if status is FAILED
     */
    public boolean hasFailed() {
        return status == AlertStatus.FAILED;
    }

    /**
     * Checks if the delivery is still pending.
     * 
     * @return true if status is PENDING
     */
    public boolean isPending() {
        return status == AlertStatus.PENDING;
    }

    /**
     * Gets a description of the delivery status.
     * 
     * @return status description with error details if available
     */
    public String getStatusDescription() {
        if (status == null) {
            return "Unknown status";
        }

        String description = statusDisplayName;
        if (hasFailed() && errorMessage != null && !errorMessage.trim().isEmpty()) {
            description += ": " + errorMessage;
        }

        return description;
    }

    @Override
    public String toString() {
        return "AlertHistoryDTO{" +
                "id=" + id +
                ", alertId=" + alertId +
                ", userId=" + userId +
                ", airQualityId=" + airQualityId +
                ", sendDate=" + sendDate +
                ", status=" + status +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}