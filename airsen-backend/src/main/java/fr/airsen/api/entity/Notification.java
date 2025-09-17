package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.NotificationChannel;
import fr.airsen.api.entity.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entity representing user notification system.
 * 
 * This entity manages notifications sent to users through various channels,
 * currently limited to email delivery as per business requirements.
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user_id", columnList = "user_id"),
    @Index(name = "idx_notification_receiver_id", columnList = "user_id_receiver"),
    @Index(name = "idx_notification_send_status", columnList = "send_status"),
    @Index(name = "idx_notification_created_date", columnList = "created_date")
})
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    /**
     * Unique identifier for the notification.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * User who sent this notification (can be system-generated).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Sender user is required for notification")
    private User user;

    /**
     * User who will receive this notification.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_receiver", nullable = false)
    @NotNull(message = "Receiver user is required for notification")
    private User userReceiver;

    /**
     * Type of notification delivery method.
     * Currently limited to email only per business requirement.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    /**
     * Title of the notification.
     */
    @Column(name = "title", nullable = false, length = 255)
    @NotBlank(message = "Notification title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    /**
     * Content message of the notification.
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Notification message is required")
    @Size(max = 5000, message = "Message cannot exceed 5000 characters")
    private String message;

    /**
     * Delivery success status of the notification.
     */
    @Column(name = "send_status", nullable = false)
    private Boolean sendStatus = false;

    /**
     * Channel used for notification delivery.
     * Currently limited to email only per business requirement.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "send_channel", nullable = false)
    @NotNull(message = "Send channel is required")
    private NotificationChannel sendChannel;

    /**
     * Date and time when this notification was created.
     */
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    /**
     * Error message if delivery failed.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    @Size(max = 1000, message = "Error message cannot exceed 1000 characters")
    private String errorMessage;

    /**
     * Default constructor.
     */
    public Notification() {
        this.sendStatus = false;
        this.notificationType = NotificationType.EMAIL;
        this.sendChannel = NotificationChannel.EMAIL;
    }

    /**
     * Constructor with main parameters.
     * 
     * @param user sender of the notification
     * @param userReceiver receiver of the notification
     * @param title notification title
     * @param message notification content
     */
    public Notification(User user, User userReceiver, String title, String message) {
        this();
        this.user = user;
        this.userReceiver = userReceiver;
        this.title = title;
        this.message = message;
    }

    /**
     * Constructor with notification type.
     * 
     * @param user sender of the notification
     * @param userReceiver receiver of the notification
     * @param title notification title
     * @param message notification content
     * @param notificationType delivery method
     */
    public Notification(User user, User userReceiver, String title, String message, 
                       NotificationType notificationType) {
        this(user, userReceiver, title, message);
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

    public User getUserReceiver() {
        return userReceiver;
    }

    public void setUserReceiver(User userReceiver) {
        this.userReceiver = userReceiver;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(NotificationType notificationType) {
        this.notificationType = notificationType;
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
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    // Business methods

    /**
     * Marks this notification as successfully sent.
     */
    public void markAsSent() {
        this.sendStatus = true;
        this.errorMessage = null;
    }

    /**
     * Marks this notification as failed with an error message.
     * 
     * @param errorMessage description of the delivery failure
     */
    public void markAsFailed(String errorMessage) {
        this.sendStatus = false;
        this.errorMessage = errorMessage;
    }

    /**
     * Checks if this notification has failed to send.
     * 
     * @return true if the notification failed to send
     */
    public boolean hasFailed() {
        return !sendStatus && errorMessage != null && !errorMessage.trim().isEmpty();
    }

    /**
     * Checks if this notification is pending delivery.
     * 
     * @return true if the notification is pending
     */
    public boolean isPending() {
        return !sendStatus && (errorMessage == null || errorMessage.trim().isEmpty());
    }

    /**
     * Validates that the notification has valid email delivery configuration.
     * 
     * @return true if email delivery is properly configured
     */
    public boolean isValidForEmailDelivery() {
        return userReceiver != null && 
               userReceiver.getEmail() != null && 
               !userReceiver.getEmail().trim().isEmpty() &&
               userReceiver.getEmailVerified() &&
               (notificationType == NotificationType.EMAIL || 
                notificationType == NotificationType.EMAIL_AND_PUSH);
    }

    /**
     * Gets the recipient's email address for delivery.
     * 
     * @return recipient email address or null if not available
     */
    public String getRecipientEmail() {
        return userReceiver != null ? userReceiver.getEmail() : null;
    }

    /**
     * Creates a summary of this notification for logging.
     * 
     * @return notification summary
     */
    public String getSummary() {
        String recipientInfo = userReceiver != null ? userReceiver.getEmail() : "Unknown recipient";
        String statusInfo = sendStatus ? "Sent" : "Pending/Failed";
        
        return String.format("Notification[%d]: '%s' to %s (%s)", 
                           id, title, recipientInfo, statusInfo);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", notificationType=" + notificationType +
                ", sendStatus=" + sendStatus +
                ", sendChannel=" + sendChannel +
                ", createdDate=" + createdDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Notification that = (Notification) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}