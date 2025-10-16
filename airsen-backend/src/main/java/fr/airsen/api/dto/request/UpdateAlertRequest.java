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

    @DecimalMin(value = "0.01", message = "Threshold value must be positive")
    @DecimalMax(value = "2000.00", message = "Threshold value is too high")
    @Digits(integer = 6, fraction = 2, message = "Threshold value must have at most 6 digits before decimal and 2 after")
    private BigDecimal thresholdValue;

    private NotificationType notificationType;

    private Boolean active;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    public UpdateAlertRequest() {}

    public UpdateAlertRequest(BigDecimal thresholdValue, NotificationType notificationType) {
        this.thresholdValue = thresholdValue;
        this.notificationType = notificationType;
    }

    public UpdateAlertRequest(Boolean active) {
        this.active = active;
    }

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