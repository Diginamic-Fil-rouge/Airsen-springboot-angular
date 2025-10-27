package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.AlertSignalKind;
import fr.airsen.api.entity.enums.AlertSignalLevel;
import fr.airsen.api.entity.enums.AlertSignalSource;
import fr.airsen.api.entity.enums.GeographicScopeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;


@Entity
@Table(name = "alert_signals", indexes = {
    @Index(name = "idx_alert_signal_source_level", columnList = "source, level"),
    @Index(name = "idx_alert_signal_scope", columnList = "scope_type, scope_id"),
    @Index(name = "idx_alert_signal_detected_at", columnList = "detected_at DESC"),
    @Index(name = "idx_alert_signal_valid_period", columnList = "valid_from, valid_to")
})
public class AlertSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    @NotNull(message = "Alert signal source is required")
    private AlertSignalSource source;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    @NotNull(message = "Alert signal kind is required")
    private AlertSignalKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    @NotNull(message = "Alert signal level is required")
    private AlertSignalLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 20)
    @NotNull(message = "Geographic scope type is required")
    private GeographicScopeType scopeType;

    @Column(name = "scope_id", nullable = true)
    private Long scopeId;

    @Column(name = "summary", nullable = false, length = 255)
    @NotNull(message = "Alert signal summary is required")
    @Size(max = 255, message = "Summary must not exceed 255 characters")
    private String summary;

    @Column(name = "details", columnDefinition = "TEXT", nullable = true)
    private String details;

    @Column(name = "detected_at", nullable = false)
    @NotNull(message = "Detection timestamp is required")
    @PastOrPresent(message = "Detection timestamp cannot be in the future")
    private LocalDateTime detectedAt;

    @Column(name = "valid_from", nullable = true)
    private LocalDateTime validFrom;

    @Column(name = "valid_to", nullable = true)
    private LocalDateTime validTo;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AlertSignal() {
    }

    /**
     * Constructor for creating a new alert signal.
     *
     * @param source source of the alert signal (ATMO or WEATHER)
     * @param kind type of environmental condition
     * @param level severity level of the signal
     * @param scopeType geographic scope type
     * @param scopeId ID of the geographic entity (nullable for FRANCE)
     * @param summary brief description of the signal
     * @param detectedAt timestamp when the signal was detected
     */
    public AlertSignal(AlertSignalSource source, AlertSignalKind kind, AlertSignalLevel level,
                      GeographicScopeType scopeType, Long scopeId, String summary,
                      LocalDateTime detectedAt) {
        this.source = source;
        this.kind = kind;
        this.level = level;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.summary = summary;
        this.detectedAt = detectedAt;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AlertSignalSource getSource() {
        return source;
    }

    public void setSource(AlertSignalSource source) {
        this.source = source;
    }

    public AlertSignalKind getKind() {
        return kind;
    }

    public void setKind(AlertSignalKind kind) {
        this.kind = kind;
    }

    public AlertSignalLevel getLevel() {
        return level;
    }

    public void setLevel(AlertSignalLevel level) {
        this.level = level;
    }

    public GeographicScopeType getScopeType() {
        return scopeType;
    }

    public void setScopeType(GeographicScopeType scopeType) {
        this.scopeType = scopeType;
    }

    public Long getScopeId() {
        return scopeId;
    }

    public void setScopeId(Long scopeId) {
        this.scopeId = scopeId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }

    public void setDetectedAt(LocalDateTime detectedAt) {
        this.detectedAt = detectedAt;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDateTime validTo) {
        this.validTo = validTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // TODO: Add getCampaigns/setCampaigns in Phase 2.3 when NotificationCampaign entity is created
    // public List<NotificationCampaign> getCampaigns() {
    //     return campaigns;
    // }
    //
    // public void setCampaigns(List<NotificationCampaign> campaigns) {
    //     this.campaigns = campaigns;
    // }

    // Business Methods

    /**
     * Checks if this signal has expired based on validTo timestamp.
     *
     * @return true if validTo is set and is before current time
     */
    public boolean isExpired() {
        return validTo != null && LocalDateTime.now().isAfter(validTo);
    }

    /**
     * Checks if this signal is currently active.
     * An active signal is one where:
     * - If validFrom is set, current time is after or equal to validFrom
     * - If validTo is set, current time is before validTo
     * - If neither is set, the signal is considered always active
     *
     * @return true if the signal is currently active
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = validFrom == null || !now.isBefore(validFrom);
        boolean beforeEnd = validTo == null || now.isBefore(validTo);
        return afterStart && beforeEnd;
    }

    /**
     * Gets a human-readable description of the geographic scope.
     * For scoped alerts (REGION/DEPARTMENT/COMMUNE), this includes the scope type.
     *
     * @return human-readable scope description
     */
    public String getScopeDescription() {
        return switch (scopeType) {
            case FRANCE -> "France";
            case REGION -> scopeId != null ? "Region ID: " + scopeId : "Region";
            case DEPARTMENT -> scopeId != null ? "Department ID: " + scopeId : "Department";
            case COMMUNE -> scopeId != null ? "Commune ID: " + scopeId : "Commune";
        };
    }

    /**
     * Generates a brief signal summary for admin dashboard display.
     * Combines level, kind, and geographic scope into a concise description.
     *
     * @return brief signal summary
     */
    public String getSignalSummary() {
        return String.format("%s %s alert for %s",
            level.getDisplayName(),
            kind.getDisplayName(),
            getScopeDescription()
        );
    }

    /**
     * Checks if this signal can be used to create a notification campaign.
     * A signal is campaign-eligible if it is active and not expired.
     *
     * @return true if the signal can be used for campaign creation
     */
    public boolean isCampaignEligible() {
        return isActive() && !isExpired();
    }

    /**
     * Sets the validity period for this signal.
     *
     * @param validFrom start of validity period
     * @param validTo end of validity period
     * @throws IllegalArgumentException if validTo is before validFrom
     */
    public void setValidityPeriod(LocalDateTime validFrom, LocalDateTime validTo) {
        if (validFrom != null && validTo != null && validTo.isBefore(validFrom)) {
            throw new IllegalArgumentException("Valid-to timestamp must be after valid-from timestamp");
        }
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    @Override
    public String toString() {
        return "AlertSignal{" +
                "id=" + id +
                ", source=" + source +
                ", kind=" + kind +
                ", level=" + level +
                ", scopeType=" + scopeType +
                ", scopeId=" + scopeId +
                ", summary='" + summary + '\'' +
                ", detectedAt=" + detectedAt +
                ", validFrom=" + validFrom +
                ", validTo=" + validTo +
                ", active=" + isActive() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AlertSignal that = (AlertSignal) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
