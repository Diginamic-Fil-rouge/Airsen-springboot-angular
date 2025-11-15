package fr.airsen.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * DTO for admin action log entries.
 *
 * Contains audit information about administrative actions performed in the system.
 */
public class AdminActionLogDTO {

    private Long id;
    private Long adminUserId;
    private String adminUserEmail;
    private String actionType;
    private Long targetResourceId;
    private String actionDetails;
    private String ipAddress;
    private String userAgent;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime actionTimestamp;

    public AdminActionLogDTO() {
    }

    public AdminActionLogDTO(Long id, Long adminUserId, String adminUserEmail, String actionType,
                            Long targetResourceId, String actionDetails, String ipAddress,
                            String userAgent, LocalDateTime actionTimestamp) {
        this.id = id;
        this.adminUserId = adminUserId;
        this.adminUserEmail = adminUserEmail;
        this.actionType = actionType;
        this.targetResourceId = targetResourceId;
        this.actionDetails = actionDetails;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.actionTimestamp = actionTimestamp;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAdminUserId() {
        return adminUserId;
    }

    public void setAdminUserId(Long adminUserId) {
        this.adminUserId = adminUserId;
    }

    public String getAdminUserEmail() {
        return adminUserEmail;
    }

    public void setAdminUserEmail(String adminUserEmail) {
        this.adminUserEmail = adminUserEmail;
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
    public String toString() {
        return "AdminActionLogDTO{" +
                "id=" + id +
                ", adminUserId=" + adminUserId +
                ", adminUserEmail='" + adminUserEmail + '\'' +
                ", actionType='" + actionType + '\'' +
                ", targetResourceId=" + targetResourceId +
                ", actionDetails='" + actionDetails + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", userAgent='" + userAgent + '\'' +
                ", actionTimestamp=" + actionTimestamp +
                '}';
    }
}
