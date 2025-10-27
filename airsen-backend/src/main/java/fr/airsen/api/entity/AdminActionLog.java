package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an audit log entry for administrative actions.
 */
@Entity
@Table(name = "admin_action_logs", indexes = {
    @Index(name = "idx_admin_user", columnList = "admin_user_id"),
    @Index(name = "idx_action_type", columnList = "action_type"),
    @Index(name = "idx_action_timestamp", columnList = "action_timestamp"),
    @Index(name = "idx_target_resource", columnList = "target_resource_id")
})
public class AdminActionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * The admin user who performed this action.
     *
     * This is nullable to handle edge cases where admin accounts
     * may be deleted. When an admin is deleted, this field becomes null but the log entry
     * is preserved for audit trail integrity.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_user_id", nullable = true)
    private User adminUser;

    /**
     * Type of administrative action performed.
     *
     * Stored as string to allow enum evolution without database migration.
     * Maps to {@link fr.airsen.api.entity.enums.AdminActionType} enum values.
     */
    @Column(name = "action_type", nullable = false, length = 50)
    @NotBlank(message = "Action type is required")
    @Size(max = 50, message = "Action type cannot exceed 50 characters")
    private String actionType;

    /**
     * ID of the target resource affected by this action.
     */
    @Column(name = "target_resource_id", nullable = true)
    private Long targetResourceId;

    /**
     * Human-readable description of the action performed.
     */
    @Column(name = "action_details", nullable = false, length = 1000)
    @NotBlank(message = "Action details are required")
    @Size(max = 1000, message = "Action details cannot exceed 1000 characters")
    private String actionDetails;

    /**
     * IP address of the admin who performed the action.
     *
     * Used for security monitoring and forensic investigations. Supports both
     * IPv4 and IPv6 formats.
     */
    @Column(name = "ip_address", nullable = true, length = 45)
    @Size(max = 45, message = "IP address cannot exceed 45 characters")
    private String ipAddress;

    /**
     * User agent string from the HTTP request.
     *
     * Contains browser and device information for security tracking and
     * identifying suspicious access patterns.
     */
    @Column(name = "user_agent", nullable = true, length = 500)
    @Size(max = 500, message = "User agent cannot exceed 500 characters")
    private String userAgent;

    /**
     * Timestamp when this action was performed.
     *
     * Automatically set to current time when the log entry is created.
     * Used for audit trail chronology and retention policy enforcement.
     */
    @Column(name = "action_timestamp", nullable = false, updatable = false)
    @NotNull(message = "Action timestamp is required")
    private LocalDateTime actionTimestamp;

    public AdminActionLog() {
        this.actionTimestamp = LocalDateTime.now();
    }

    public AdminActionLog(User adminUser, String actionType, Long targetResourceId, 
                         String actionDetails, String ipAddress, String userAgent) {
        this.adminUser = adminUser;
        this.actionType = actionType;
        this.targetResourceId = targetResourceId;
        this.actionDetails = actionDetails;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.actionTimestamp = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getAdminUser() {
        return adminUser;
    }

    public void setAdminUser(User adminUser) {
        this.adminUser = adminUser;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Long getTargetResourceId() {
        return targetResourceId;
    }

    public void setTargetResourceId(Long targetResourceId) {
        this.targetResourceId = targetResourceId;
    }

    public String getActionDetails() {
        return actionDetails;
    }

    public void setActionDetails(String actionDetails) {
        this.actionDetails = actionDetails;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getActionTimestamp() {
        return actionTimestamp;
    }

    public void setActionTimestamp(LocalDateTime actionTimestamp) {
        this.actionTimestamp = actionTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AdminActionLog that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AdminActionLog{" +
                "id=" + id +
                ", adminUserId=" + (adminUser != null ? adminUser.getId() : null) +
                ", actionType='" + actionType + '\'' +
                ", targetResourceId=" + targetResourceId +
                ", actionDetails='" + actionDetails + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", actionTimestamp=" + actionTimestamp +
                '}';
    }
}
