package fr.airsen.api.dto.response;

import fr.airsen.api.entity.enums.NotificationType;
import fr.airsen.api.entity.enums.Pollutant;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for simplified Alert information.
 * 
 * This DTO provides a lightweight representation of alert information
 * for use in lists, summaries, and nested objects where full detail is not needed.
 */
public class AlertSummaryDTO {

    private Long id;

    private String communeName;

    private Pollutant pollutant;

    private String pollutantDisplayName;

    private BigDecimal thresholdValue;

    private String unit;

    private NotificationType notificationType;

    private Boolean active;

    private LocalDateTime createdDate;

    public AlertSummaryDTO() {}

    public AlertSummaryDTO(Long id, String communeName, Pollutant pollutant,
                          BigDecimal thresholdValue, NotificationType notificationType,
                          Boolean active, LocalDateTime createdDate) {
        this.id = id;
        this.communeName = communeName;
        this.pollutant = pollutant;
        this.pollutantDisplayName = pollutant != null ? pollutant.getDisplayName() : null;
        this.unit = pollutant != null ? pollutant.getUnit() : null;
        this.thresholdValue = thresholdValue;
        this.notificationType = notificationType;
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
        this.unit = pollutant != null ? pollutant.getUnit() : null;
    }

    public String getPollutantDisplayName() {
        return pollutantDisplayName;
    }

    public BigDecimal getThresholdValue() {
        return thresholdValue;
    }

    public void setThresholdValue(BigDecimal thresholdValue) {
        this.thresholdValue = thresholdValue;
    }

    public String getUnit() {
        return unit;
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

    /**
     * Gets a brief description of the alert.
     * 
     * @return alert description
     */
    public String getDescription() {
        return String.format("%s alert for %s in %s (threshold: %.2f %s)",
            active ? "Active" : "Inactive",
            pollutantDisplayName != null ? pollutantDisplayName : "Unknown pollutant",
            communeName != null ? communeName : "Unknown location",
            thresholdValue,
            unit != null ? unit : "units");
    }

    /**
     * Gets the status text for display.
     * 
     * @return status text
     */
    public String getStatusText() {
        return active ? "Active" : "Inactive";
    }

    @Override
    public String toString() {
        return "AlertSummaryDTO{" +
                "id=" + id +
                ", communeName='" + communeName + '\'' +
                ", pollutant=" + pollutant +
                ", thresholdValue=" + thresholdValue +
                ", active=" + active +
                '}';
    }
}