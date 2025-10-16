package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.AlertStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "alert_history", indexes = {
    @Index(name = "idx_alert_history_alert_id", columnList = "alert_id"),
    @Index(name = "idx_alert_history_air_quality_id", columnList = "air_quality_id"),
    @Index(name = "idx_alert_history_send_date", columnList = "send_date"),
    @Index(name = "idx_alert_history_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class AlertHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    @NotNull(message = "Alert is required for alert history")
    private Alert alert;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "air_quality_id", nullable = false)
    @NotNull(message = "Air quality measurement is required for alert history")
    private AirQuality airQuality;

    @CreatedDate
    @Column(name = "send_date", nullable = false, updatable = false)
    private LocalDateTime sendDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull(message = "Alert status is required")
    private AlertStatus status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    @Size(max = 2000, message = "Error message cannot exceed 2000 characters")
    private String errorMessage;

    public AlertHistory() {
        this.status = AlertStatus.PENDING;
    }

    public AlertHistory(Alert alert, AirQuality airQuality) {
        this();
        this.alert = alert;
        this.airQuality = airQuality;
    }

    public AlertHistory(Alert alert, AirQuality airQuality, AlertStatus status) {
        this(alert, airQuality);
        this.status = status;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Alert getAlert() {
        return alert;
    }

    public void setAlert(Alert alert) {
        this.alert = alert;
    }

    public AirQuality getAirQuality() {
        return airQuality;
    }

    public void setAirQuality(AirQuality airQuality) {
        this.airQuality = airQuality;
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
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Business methods

    /**
     * Marks this alert delivery as successfully sent.
     */
    public void markAsSent() {
        this.status = AlertStatus.SENT;
        this.errorMessage = null;
    }

    public void markAsFailed(String errorMessage) {
        this.status = AlertStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markAsPending() {
        this.status = AlertStatus.PENDING;
        this.errorMessage = null;
    }

    /**
     * Checks if this alert delivery was successful.
     * 
     * @return true if the status is SENT
     */
    public boolean wasSuccessful() {
        return status == AlertStatus.SENT;
    }

    /**
     * Checks if this alert delivery failed.
     * 
     * @return true if the status is FAILED
     */
    public boolean hasFailed() {
        return status == AlertStatus.FAILED;
    }

    /**
     * Checks if this alert delivery is still pending.
     * 
     * @return true if the status is PENDING
     */
    public boolean isPending() {
        return status == AlertStatus.PENDING;
    }

    /**
     * Gets a human-readable description of this alert history record.
     * 
     * @return alert history description
     */
    public String getDescription() {
        String alertDescription = alert != null ? alert.getDescription() : "Unknown alert";
        String statusDescription = status.getDisplayName();
        
        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
            return String.format("%s - %s: %s", alertDescription, statusDescription, errorMessage);
        } else {
            return String.format("%s - %s", alertDescription, statusDescription);
        }
    }

    @Override
    public String toString() {
        return "AlertHistory{" +
                "id=" + id +
                ", status=" + status +
                ", sendDate=" + sendDate +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        AlertHistory that = (AlertHistory) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}