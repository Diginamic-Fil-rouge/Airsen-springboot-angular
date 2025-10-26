package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.GeographicScopeType;
import fr.airsen.api.entity.enums.NotificationCampaignStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity representing a notification broadcast campaign.
 *
 * A campaign allows administrators to send notifications to multiple users
 * based on geographic scope (France, Region, Department, or Commune).
 * Tracks sending progress with recipient counts and delivery statistics.
 */
@Entity
@Table(name = "notification_campaigns", indexes = {
    @Index(name = "idx_campaign_status_created", columnList = "status, created_at"),
    @Index(name = "idx_campaign_created_by", columnList = "created_by")
})
public class NotificationCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title", nullable = false, length = 255)
    @NotBlank(message = "Campaign title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Campaign message is required")
    @Size(max = 5000, message = "Message cannot exceed 5000 characters")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 50)
    @NotNull(message = "Geographic scope type is required")
    private GeographicScopeType scopeType;

    @Column(name = "scope_id")
    private Long scopeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "created_by",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_campaign_created_by")
    )
    @NotNull(message = "Campaign creator is required")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "alert_signal_id",
        foreignKey = @ForeignKey(name = "fk_campaign_alert_signal")
    )
    private AlertSignal alertSignal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @NotNull(message = "Campaign status is required")
    private NotificationCampaignStatus status = NotificationCampaignStatus.DRAFT;

    @Column(name = "total_recipients", nullable = false)
    @Min(value = 0, message = "Total recipients cannot be negative")
    private Integer totalRecipients = 0;

    @Column(name = "sent_count", nullable = false)
    @Min(value = 0, message = "Sent count cannot be negative")
    private Integer sentCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Min(value = 0, message = "Failed count cannot be negative")
    private Integer failedCount = 0;

    @OneToMany(
        mappedBy = "campaign",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    private List<Notification> notifications = new ArrayList<>();

    public NotificationCampaign() {
        this.status = NotificationCampaignStatus.DRAFT;
        this.totalRecipients = 0;
        this.sentCount = 0;
        this.failedCount = 0;
    }

    public NotificationCampaign(String title, String message, GeographicScopeType scopeType,
                               Long scopeId, User createdBy) {
        this();
        this.title = title;
        this.message = message;
        this.scopeType = scopeType;
        this.scopeId = scopeId;
        this.createdBy = createdBy;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public AlertSignal getAlertSignal() {
        return alertSignal;
    }

    public void setAlertSignal(AlertSignal alertSignal) {
        this.alertSignal = alertSignal;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public NotificationCampaignStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationCampaignStatus status) {
        this.status = status;
    }

    public Integer getTotalRecipients() {
        return totalRecipients;
    }

    public void setTotalRecipients(Integer totalRecipients) {
        this.totalRecipients = totalRecipients;
    }

    public Integer getSentCount() {
        return sentCount;
    }

    public void setSentCount(Integer sentCount) {
        this.sentCount = sentCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }


    public boolean isDraft() {
        return this.status == NotificationCampaignStatus.DRAFT;
    }

    public boolean isSending() {
        return this.status == NotificationCampaignStatus.SENDING;
    }

    public boolean isCompleted() {
        return this.status == NotificationCampaignStatus.COMPLETED;
    }

    /**
     * Increments the sent count by 1.
     *
     * Thread-safe increment operation for tracking successful deliveries.
     */
    public void incrementSent() {
        this.sentCount++;
    }

    /**
     * Increments the failed count by 1.
     *
     * Thread-safe increment operation for tracking failed deliveries.
     */
    public void incrementFailed() {
        this.failedCount++;
    }

    public void markAsCompleted() {
        this.status = NotificationCampaignStatus.COMPLETED;
    }

    /**
     * Marks the campaign as failed due to critical error.
     *
     * Sets status to FAILED. This method should be called when a critical
     * error prevents the campaign from completing successfully.
     */
    public void markAsFailed() {
        this.status = NotificationCampaignStatus.FAILED;
    }

    public void markAsSending() {
        this.status = NotificationCampaignStatus.SENDING;
    }

    public double getDeliveryRate() {
        if (totalRecipients == 0) {
            return 0.0;
        }
        return (sentCount * 100.0) / totalRecipients;
    }

    /**
     * Calculates the failure rate as a percentage.
     *
     * @return failure rate as percentage (0.0 to 100.0)
     */
    public double getFailureRate() {
        if (totalRecipients == 0) {
            return 0.0;
        }
        return (failedCount * 100.0) / totalRecipients;
    }

    /**
     * Checks if all notifications have been processed.
     *
     * @return true if sentCount + failedCount equals totalRecipients
     */
    public boolean isFullyProcessed() {
        return (sentCount + failedCount) >= totalRecipients;
    }

    /**
     * Adds a notification to this campaign.
     *
     * Establishes bidirectional relationship with Notification entity.
     *
     * @param notification notification to add
     */
    public void addNotification(Notification notification) {
        notifications.add(notification);
        notification.setCampaign(this);
    }


    public void removeNotification(Notification notification) {
        notifications.remove(notification);
        notification.setCampaign(null);
    }

    /**
     * Gets a human-readable description of the geographic scope.
     *
     * @return scope description (e.g., "France", "Region #12", "Department #75")
     */
    public String getScopeDescription() {
        if (scopeType == GeographicScopeType.FRANCE) {
            return "France";
        }
        return scopeType.getDisplayName() + " #" + scopeId;
    }

    /**
     * Creates a summary of this campaign for admin dashboard.
     *
     * @return campaign summary with key metrics
     */
    public String getCampaignSummary() {
        return String.format("Campaign[%d]: '%s' - %s (%d recipients, %d sent, %d failed, %.1f%% delivered)",
            id, title, status.getDisplayName(), totalRecipients, sentCount, failedCount, getDeliveryRate());
    }

    @Override
    public String toString() {
        return "NotificationCampaign{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", scopeType=" + scopeType +
                ", scopeId=" + scopeId +
                ", status=" + status +
                ", totalRecipients=" + totalRecipients +
                ", sentCount=" + sentCount +
                ", failedCount=" + failedCount +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NotificationCampaign that = (NotificationCampaign) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
