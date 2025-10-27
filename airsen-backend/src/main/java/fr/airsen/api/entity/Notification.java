package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.NotificationChannel;
import fr.airsen.api.entity.enums.NotificationDeliveryStatus;
import fr.airsen.api.entity.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user_id", columnList = "user_id"),
    @Index(name = "idx_notification_receiver_id", columnList = "user_id_receiver"),
    @Index(name = "idx_notification_send_status", columnList = "send_status"),
    @Index(name = "idx_notification_created_date", columnList = "created_date"),
    @Index(name = "idx_notification_campaign_delivery", columnList = "campaign_id, delivery_status")
})
@EntityListeners(AuditingEntityListener.class)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "Sender user is required for notification")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id_receiver", nullable = false)
    @NotNull(message = "Receiver user is required for notification")
    private User userReceiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "campaign_id",
        foreignKey = @ForeignKey(name = "fk_notification_campaign")
    )
    private NotificationCampaign campaign;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 50)
    @NotNull(message = "Delivery status is required")
    private NotificationDeliveryStatus deliveryStatus = NotificationDeliveryStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    @NotNull(message = "Notification type is required")
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 255)
    @NotBlank(message = "Notification title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Notification message is required")
    @Size(max = 5000, message = "Message cannot exceed 5000 characters")
    private String message;

    @Column(name = "send_status", nullable = false)
    private Boolean readStatus = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_channel", nullable = false)
    @NotNull(message = "Send channel is required")
    private NotificationChannel sendChannel;

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "sent_date")
    private LocalDateTime sentDate;

    @Column(name = "error_message", columnDefinition = "TEXT")
    @Size(max = 1000, message = "Error message cannot exceed 1000 characters")
    private String errorMessage;

    public Notification() {
        this.readStatus = false;
        this.notificationType = NotificationType.EMAIL;
        this.sendChannel = NotificationChannel.EMAIL;
        this.deliveryStatus = NotificationDeliveryStatus.PENDING;
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

    public NotificationCampaign getCampaign() {
        return campaign;
    }

    public void setCampaign(NotificationCampaign campaign) {
        this.campaign = campaign;
    }

    public NotificationDeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(NotificationDeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
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

    public Boolean getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Boolean readStatus) {
        this.readStatus = readStatus;
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

    // Business methods

    /**
     * Marks this notification as successfully sent.
     */
    public void markAsSent() {
        this.readStatus = true;
        this.sentDate = LocalDateTime.now();
        this.errorMessage = null;
        this.deliveryStatus = NotificationDeliveryStatus.SENT;
    }

    /**
     * Marks this notification as failed with an error message.
     *
     * @param errorMessage description of the delivery failure
     */
    public void markAsFailed(String errorMessage) {
        this.readStatus = false;
        this.errorMessage = errorMessage;
        this.sentDate = null;
        this.deliveryStatus = NotificationDeliveryStatus.FAILED;
    }

    /**
     * Checks if this notification has failed to send.
     *
     * @return true if the notification failed to send
     */
    public boolean hasFailed() {
        return deliveryStatus == NotificationDeliveryStatus.FAILED;
    }

    /**
     * Checks if this notification is pending delivery.
     *
     * @return true if the notification is pending
     */
    public boolean isPending() {
        return deliveryStatus == NotificationDeliveryStatus.PENDING;
    }

    /**
     * Checks if this notification was successfully sent.
     *
     * @return true if the notification was sent successfully
     */
    public boolean wasSent() {
        return deliveryStatus == NotificationDeliveryStatus.SENT;
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
        String statusInfo = readStatus ? "Read" : "Unread";

        return String.format("Notification[%d]: '%s' to %s (%s)",
                           id, title, recipientInfo, statusInfo);
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", notificationType=" + notificationType +
                ", readStatus=" + readStatus +
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
