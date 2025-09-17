package fr.airsen.api.dto.request;

import fr.airsen.api.entity.enums.NotificationType;
import fr.airsen.api.entity.enums.Pollutant;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Data Transfer Object for creating new alerts.
 * 
 * This DTO represents the request payload for creating a new air quality alert
 * with validation constraints to ensure data integrity.
 */
public class CreateAlertRequest {

    /**
     * Commune identifier where the alert should monitor air quality.
     */
    @NotNull(message = "Commune ID is required")
    @Positive(message = "Commune ID must be positive")
    private Long communeId;

    /**
     * Pollutant type to monitor for this alert.
     */
    @NotNull(message = "Pollutant type is required")
    private Pollutant pollutant;

    /**
     * Threshold value that will trigger the alert when exceeded.
     */
    @NotNull(message = "Threshold value is required")
    @DecimalMin(value = "0.01", message = "Threshold value must be positive")
    @DecimalMax(value = "2000.00", message = "Threshold value is too high")
    @Digits(integer = 6, fraction = 2, message = "Threshold value must have at most 6 digits before decimal and 2 after")
    private BigDecimal thresholdValue;

    /**
     * Notification delivery method for this alert.
     */
    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    /**
     * Optional description or notes for this alert.
     */
    @Size(max = 500, message = "Description cannot exceed 500 characters")
    private String description;

    /**
     * Default constructor.
     */
    public CreateAlertRequest() {}

    /**
     * Constructor with all required fields.
     * 
     * @param communeId commune identifier
     * @param pollutant pollutant type to monitor
     * @param thresholdValue threshold for alert triggering
     * @param notificationType notification delivery method
     */
    public CreateAlertRequest(Long communeId, Pollutant pollutant, 
                             BigDecimal thresholdValue, NotificationType notificationType) {
        this.communeId = communeId;
        this.pollutant = pollutant;
        this.thresholdValue = thresholdValue;
        this.notificationType = notificationType;
    }

    // Getters and Setters

    public Long getCommuneId() {
        return communeId;
    }

    public void setCommuneId(Long communeId) {
        this.communeId = communeId;
    }

    public Pollutant getPollutant() {
        return pollutant;
    }

    public void setPollutant(Pollutant pollutant) {
        this.pollutant = pollutant;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Validates that the threshold value is appropriate for the pollutant type.
     * 
     * @return true if threshold is valid for the pollutant
     */
    public boolean isThresholdValidForPollutant() {
        if (pollutant == null || thresholdValue == null) {
            return false;
        }

        BigDecimal maxValue = switch (pollutant) {
            case NO2 -> new BigDecimal("1000");
            case O3 -> new BigDecimal("500");
            case PM10 -> new BigDecimal("500");
            case PM25 -> new BigDecimal("300");
            case SO2 -> new BigDecimal("1000");
        };

        return thresholdValue.compareTo(maxValue) <= 0;
    }

    @Override
    public String toString() {
        return "CreateAlertRequest{" +
                "communeId=" + communeId +
                ", pollutant=" + pollutant +
                ", thresholdValue=" + thresholdValue +
                ", notificationType=" + notificationType +
                ", description='" + description + '\'' +
                '}';
    }
}