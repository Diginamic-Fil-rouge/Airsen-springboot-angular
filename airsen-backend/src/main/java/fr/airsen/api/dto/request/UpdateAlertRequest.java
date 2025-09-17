package fr.airsen.api.dto.request;

import fr.airsen.api.entity.enums.NotificationType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Data Transfer Object for updating existing alerts.
 * 
 * This DTO represents the request payload for updating an existing air quality alert.
 * All fields are optional to allow partial updates.
 */
public class UpdateAlertRequest {

    /**
     * New threshold value for the alert.
     * If provided, must be positive and within reasonable limits.
     */
    @DecimalMin(value = "0.01", message = "Threshold value must be positive")
    @DecimalMax(value = "2000.00", message = "Threshold value is too high")
    @Digits(integer = 6, fraction = 2, message = "Threshold value must have at most 6 digits before decimal and 2 after")
    private BigDecimal thresholdValue;

    /**
     * New notification delivery method for the alert.
     */
    private NotificationType notificationType;

    /**
     * Whether the alert should be active or inactive.
     */
    private Boolean active;

    /**
     * Updated description or notes for the alert.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    /**
     * Default constructor.
     */
    public UpdateAlertRequest() {}

    /**
     * Constructor with threshold and notification type.
     * 
     * @param thresholdValue new threshold value
     * @param notificationType new notification type
     */
    public UpdateAlertRequest(BigDecimal thresholdValue, NotificationType notificationType) {
        this.thresholdValue = thresholdValue;
        this.notificationType = notificationType;
    }

    /**
     * Constructor for activation/deactivation only.
     * 
     * @param active new active status
     */
    public UpdateAlertRequest(Boolean active) {
        this.active = active;
    }

    // Getters and Setters

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Checks if any field has been provided for update.
     * 
     * @return true if at least one field is set for update
     */
    public boolean hasUpdates() {
        return thresholdValue != null || 
               notificationType != null || 
               active != null || 
               (description != null && !description.trim().isEmpty());
    }

    /**
     * Checks if only the active status is being updated.
     * 
     * @return true if only active field is set
     */
    public boolean isOnlyActivationUpdate() {
        return active != null && 
               thresholdValue == null && 
               notificationType == null && 
               (description == null || description.trim().isEmpty());
    }

    @Override
    public String toString() {
        return "UpdateAlertRequest{" +
                "thresholdValue=" + thresholdValue +
                ", notificationType=" + notificationType +
                ", active=" + active +
                ", description='" + description + '\'' +
                '}';
    }
}