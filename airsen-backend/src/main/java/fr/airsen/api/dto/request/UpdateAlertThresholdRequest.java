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

    @NotNull(message = "Threshold value is required")
    @DecimalMin(value = "0.01", message = "Threshold value must be positive")
    @DecimalMax(value = "2000.00", message = "Threshold value is too high")
    @Digits(integer = 6, fraction = 2, message = "Threshold value must have at most 6 digits before decimal and 2 after")
    private BigDecimal thresholdValue;

    @Size(max = 200, message = "Reason cannot exceed 200 characters")
    private String reason;

    public UpdateAlertThresholdRequest() {}

    public UpdateAlertThresholdRequest(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public UpdateAlertThresholdRequest(BigDecimal thresholdValue, String reason) {
        this.thresholdValue = thresholdValue;
        this.reason = reason;
    }

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