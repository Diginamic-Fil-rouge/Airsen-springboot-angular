package fr.airsen.api.dto.response;

import fr.airsen.api.entity.enums.NotificationChannel;
import fr.airsen.api.entity.enums.NotificationType;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for Notification entity responses.
 * 
 * This DTO represents notification information for API responses,
 * including delivery status and content details.
 */
public class NotificationDTO {

    private Long id;

    private Long senderId;

    private String senderEmail;

    private Long recipientId;

    private String recipientEmail;

    private NotificationType notificationType;

    private String notificationTypeDisplayName;

    private String title;

    private String message;

    private Boolean sendStatus;

    private NotificationChannel sendChannel;

    private String sendChannelDisplayName;

    private LocalDateTime createdDate;

    private LocalDateTime sentDate;

    private String errorMessage;

    public NotificationDTO() {}

    public NotificationDTO(Long id, Long senderId, String senderEmail, Long recipientId,
                          String recipientEmail, NotificationType notificationType, String title,
                          String message, Boolean sendStatus, NotificationChannel sendChannel,
                          LocalDateTime createdDate, LocalDateTime sentDate, String errorMessage) {
        this.id = id;
        this.senderId = senderId;
        this.senderEmail = senderEmail;
        this.recipientId = recipientId;
        this.recipientEmail = recipientEmail;
        this.notificationType = notificationType;
        this.notificationTypeDisplayName = notificationType != null ? notificationType.getDisplayName() : null;
        this.title = title;
        this.message = message;
        this.sendStatus = sendStatus;
        this.sendChannel = sendChannel;
        this.sendChannelDisplayName = sendChannel != null ? sendChannel.getDisplayName() : null;
        this.createdDate = createdDate;
        this.sentDate = sentDate;
        this.errorMessage = errorMessage;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public Long getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Long recipientId) {
        this.recipientId = recipientId;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
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

    public Boolean getSendStatus() {
        return sendStatus;
    }

    public void setSendStatus(Boolean sendStatus) {
        this.sendStatus = sendStatus;
    }

    public NotificationChannel getSendChannel() {
        return sendChannel;
    }

    public void setSendChannel(NotificationChannel sendChannel) {
        this.sendChannel = sendChannel;
        this.sendChannelDisplayName = sendChannel != null ? sendChannel.getDisplayName() : null;
    }

    public String getSendChannelDisplayName() {
        return sendChannelDisplayName;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getSentDate() {
        return sentDate;
    }

    public void setSentDate(LocalDateTime sentDate) {
        this.sentDate = sentDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Checks if the notification was successfully sent.
     * 
     * @return true if successfully sent
     */
    public boolean wasSuccessfullySent() {
        return sendStatus && sentDate != null;
    }

    /**
     * Checks if the notification delivery failed.
     * 
     * @return true if delivery failed
     */
    public boolean hasFailed() {
        return !sendStatus && errorMessage != null && !errorMessage.trim().isEmpty();
    }

    /**
     * Checks if the notification is pending delivery.
     * 
     * @return true if pending
     */
    public boolean isPending() {
        return !sendStatus && (errorMessage == null || errorMessage.trim().isEmpty());
    }

    /**
     * Gets the delivery status text for display.
     * 
     * @return status text
     */
    public String getStatusText() {
        if (wasSuccessfullySent()) {
            return "Sent";
        } else if (hasFailed()) {
            return "Failed";
        } else {
            return "Pending";
        }
    }

    /**
     * Gets a truncated version of the message for display in lists.
     * 
     * @param maxLength maximum length of truncated message
     * @return truncated message
     */
    public String getTruncatedMessage(int maxLength) {
        if (message == null) {
            return "";
        }
        
        if (message.length() <= maxLength) {
            return message;
        }
        
        return message.substring(0, maxLength - 3) + "...";
    }

    /**
     * Gets a summary of the notification for display.
     * 
     * @return notification summary
     */
    public String getSummary() {
        return String.format("From: %s, To: %s, Subject: %s, Status: %s",
            senderEmail != null ? senderEmail : "System",
            recipientEmail != null ? recipientEmail : "Unknown",
            title != null ? title : "No subject",
            getStatusText());
    }

    @Override
    public String toString() {
        return "NotificationDTO{" +
                "id=" + id +
                ", senderId=" + senderId +
                ", recipientId=" + recipientId +
                ", title='" + title + '\'' +
                ", sendStatus=" + sendStatus +
                ", sendChannel=" + sendChannel +
                ", createdDate=" + createdDate +
                ", sentDate=" + sentDate +
                '}';
    }
}