package fr.airsen.api.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Data Transfer Object for updating only the threshold value of an alert.
 * 
 * This specialized DTO is used for operations that only modify the threshold
 * value of an existing alert, providing focused validation and clarity.
 */
public class UpdateAlertThresholdRequest {

    /**
     * New threshold value for the alert.
     */
    @NotNull(message = "Threshold value is required")
    @DecimalMin(value = "0.01", message = "Threshold value must be positive")
    @DecimalMax(value = "2000.00", message = "Threshold value is too high")
    @Digits(integer = 6, fraction = 2, message = "Threshold value must have at most 6 digits before decimal and 2 after")
    private BigDecimal thresholdValue;

    /**
     * Optional reason for the threshold change.
     */
    @Size(max = 200, message = "Reason cannot exceed 200 characters")
    private String reason;

    /**
     * Default constructor.
     */
    public UpdateAlertThresholdRequest() {}

    /**
     * Constructor with threshold value.
     * 
     * @param thresholdValue new threshold value
     */
    public UpdateAlertThresholdRequest(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    /**
     * Constructor with threshold and reason.
     * 
     * @param thresholdValue new threshold value
     * @param reason reason for the change
     */
    public UpdateAlertThresholdRequest(BigDecimal thresholdValue, String reason) {
        this.thresholdValue = thresholdValue;
        this.reason = reason;
    }

    // Getters and Setters

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "UpdateAlertThresholdRequest{" +
                "thresholdValue=" + thresholdValue +
                ", reason='" + reason + '\'' +
                '}';
    }
}