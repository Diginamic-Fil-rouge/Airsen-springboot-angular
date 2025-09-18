package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.NotificationType;
import fr.airsen.api.entity.enums.Pollutant;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing user-defined air quality threshold notifications.
 * 
 * This entity manages alert configurations for monitoring specific pollutants
 * in selected communes and triggering notifications when thresholds are exceeded.
 */
@Entity
@Table(name = "alerts", indexes = {
    @Index(name = "idx_alert_user_id", columnList = "user_id"),
    @Index(name = "idx_alert_commune_id", columnList = "commune_id"),
    @Index(name = "idx_alert_active", columnList = "active"),
    @Index(name = "idx_alert_pollutant", columnList = "pollutant")
})
@EntityListeners(AuditingEntityListener.class)
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * User who created this alert.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required for alert")
    private User user;

    /**
     * Commune being monitored by this alert.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commune_id", nullable = false)
    @NotNull(message = "Commune is required for alert")
    private Commune commune;

    /**
     * Pollutant being monitored by this alert.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "pollutant", nullable = false)
    @NotNull(message = "Pollutant type is required")
    private Pollutant pollutant;

    /**
     * Threshold value that triggers the alert when exceeded.
     */
    @Column(name = "threshold_value", nullable = false, precision = 8, scale = 2)
    @NotNull(message = "Threshold value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Threshold value must be positive")
    @Digits(integer = 6, fraction = 2, message = "Threshold value must have at most 6 digits before decimal and 2 after")
    private BigDecimal thresholdValue;

    /**
     * Notification delivery method for this alert.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    /**
     * Whether this alert is currently active and monitoring.
     */
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Date and time when this alert was created.
     */
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    /**
     * Historical records of this alert being triggered.
     */
    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<AlertHistory> alertHistories;

    public Alert() {
        this.active = true;
    }

    /**
     * Constructor with main parameters.
     * 
     * @param user user creating the alert
     * @param commune commune to monitor
     * @param pollutant pollutant to monitor
     * @param thresholdValue threshold for alert triggering
     * @param notificationType method of notification delivery
     */
    public Alert(User user, Commune commune, Pollutant pollutant, 
                BigDecimal thresholdValue, NotificationType notificationType) {
        this();
        this.user = user;
        this.commune = commune;
        this.pollutant = pollutant;
        this.thresholdValue = thresholdValue;
        this.notificationType = notificationType;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Commune getCommune() {
        return commune;
    }

    public void setCommune(Commune commune) {
        this.commune = commune;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public List<AlertHistory> getAlertHistories() {
        return alertHistories;
    }

    public void setAlertHistories(List<AlertHistory> alertHistories) {
        this.alertHistories = alertHistories;
    }

    // Business methods

    /**
     * Activates this alert for monitoring.
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Deactivates this alert to stop monitoring.
     */
    public void deactivate() {
        this.active = false;
    }

    /**
     * Checks if this alert should be triggered for the given pollutant value.
     * 
     * @param currentValue current pollutant measurement
     * @return true if the current value exceeds the threshold
     */
    public boolean shouldTrigger(BigDecimal currentValue) {
        return active && currentValue != null && 
               currentValue.compareTo(thresholdValue) > 0;
    }

    /**
     * Updates the threshold value for this alert.
     * 
     * @param newThreshold new threshold value
     */
    public void updateThreshold(BigDecimal newThreshold) {
        if (newThreshold != null && newThreshold.compareTo(BigDecimal.ZERO) > 0) {
            this.thresholdValue = newThreshold;
        }
    }

    /**
     * Updates the notification type for this alert.
     * 
     * @param newNotificationType new notification delivery method
     */
    public void updateNotificationType(NotificationType newNotificationType) {
        if (newNotificationType != null) {
            this.notificationType = newNotificationType;
        }
    }

    /**
     * Gets a human-readable description of this alert.
     * 
     * @return alert description
     */
    public String getDescription() {
        return String.format("Alert for %s in %s when %s exceeds %.2f %s",
            commune != null ? commune.getName() : "Unknown commune",
            pollutant.getDisplayName(),
            pollutant.name(),
            thresholdValue,
            pollutant.getUnit());
    }

    @Override
    public String toString() {
        return "Alert{" +
                "id=" + id +
                ", pollutant=" + pollutant +
                ", thresholdValue=" + thresholdValue +
                ", notificationType=" + notificationType +
                ", active=" + active +
                ", createdDate=" + createdDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Alert alert = (Alert) o;
        return id != null && id.equals(alert.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}