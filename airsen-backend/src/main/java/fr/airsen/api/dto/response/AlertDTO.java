package fr.airsen.api.dto.response;

import fr.airsen.api.entity.enums.NotificationType;
import fr.airsen.api.entity.enums.Pollutant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for Alert entity responses.
 * 
 * This DTO represents complete alert information for API responses,
 * including all relevant details for client applications.
 */
public class AlertDTO {


    private Long id;

    private Long userId;

    private String userEmail;

    private Long communeId;

    private String communeName;

    private Pollutant pollutant;

    private String pollutantDisplayName;

    private String pollutantUnit;

    private BigDecimal thresholdValue;

    private NotificationType notificationType;

    private String notificationTypeDisplayName;

    private Boolean active;

    private LocalDateTime createdDate;

    private String description;

    public AlertDTO() {}

    public AlertDTO(Long id, Long userId, String userEmail, Long communeId, String communeName,
                   Pollutant pollutant, BigDecimal thresholdValue, NotificationType notificationType,
                   Boolean active, LocalDateTime createdDate) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.communeId = communeId;
        this.communeName = communeName;
        this.pollutant = pollutant;
        this.pollutantDisplayName = pollutant != null ? pollutant.getDisplayName() : null;
        this.pollutantUnit = pollutant != null ? pollutant.getUnit() : null;
        this.thresholdValue = thresholdValue;
        this.notificationType = notificationType;
        this.notificationTypeDisplayName = notificationType != null ? notificationType.getDisplayName() : null;
        this.active = active;
        this.createdDate = createdDate;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getCommuneId() {
        return communeId;
    }

    public void setCommuneId(Long communeId) {
        this.communeId = communeId;
    }

    public String getCommuneName() {
        return communeName;
    }

    public void setCommuneName(String communeName) {
        this.communeName = communeName;
    }

    public Pollutant getPollutant() {
        return pollutant;
    }

    public void setPollutant(Pollutant pollutant) {
        this.pollutant = pollutant;
        this.pollutantDisplayName = pollutant != null ? pollutant.getDisplayName() : null;
        this.pollutantUnit = pollutant != null ? pollutant.getUnit() : null;
    }

    public String getPollutantDisplayName() {
        return pollutantDisplayName;
    }

    public String getPollutantUnit() {
        return pollutantUnit;
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
        this.notificationTypeDisplayName = notificationType != null ? notificationType.getDisplayName() : null;
    }

    public String getNotificationTypeDisplayName() {
        return notificationTypeDisplayName;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Gets a human-readable summary of the alert.
     * 
     * @return alert summary
     */
    public String getSummary() {
        return String.format("Alert for %s in %s when %s exceeds %.2f %s",
            communeName != null ? communeName : "Unknown location",
            pollutantDisplayName != null ? pollutantDisplayName : "Unknown pollutant",
            pollutant != null ? pollutant.name() : "N/A",
            thresholdValue,
            pollutantUnit != null ? pollutantUnit : "units");
    }

    /**
     * Checks if the alert includes email notifications.
     * 
     * @return true if email notifications are enabled
     */
    public boolean includesEmailNotification() {
        return notificationType != null && notificationType.includesEmail();
    }

    /**
     * Checks if the alert includes push notifications.
     * 
     * @return true if push notifications are enabled
     */
    public boolean includesPushNotification() {
        return notificationType != null && notificationType.includesPush();
    }

    @Override
    public String toString() {
        return "AlertDTO{" +
                "id=" + id +
                ", userId=" + userId +
                ", communeId=" + communeId +
                ", communeName='" + communeName + '\'' +
                ", pollutant=" + pollutant +
                ", thresholdValue=" + thresholdValue +
                ", notificationType=" + notificationType +
                ", active=" + active +
                ", createdDate=" + createdDate +
                '}';
    }
}