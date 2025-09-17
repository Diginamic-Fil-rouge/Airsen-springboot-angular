package fr.airsen.api.service;

import fr.airsen.api.entity.Notification;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.NotificationChannel;
import fr.airsen.api.entity.enums.NotificationType;
import fr.airsen.api.repository.NotificationRepository;
import fr.airsen.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing Notification entities and email delivery.
 * 
 * This service provides comprehensive notification management functionality including
 * email processing, queue management, validation, and delivery status tracking
 * for the Airsens air quality monitoring system.
 */
@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    /**
     * Constructor for NotificationService.
     * 
     * @param notificationRepository notification data access repository
     * @param userRepository user data access repository
     * @param mailSender Spring mail sender for email delivery
     */
    @Autowired
    public NotificationService(NotificationRepository notificationRepository,
                              UserRepository userRepository,
                              JavaMailSender mailSender) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.mailSender = mailSender;
    }

    /**
     * Creates a new notification.
     * 
     * @param senderId sender user identifier
     * @param recipientId recipient user identifier
     * @param title notification title
     * @param message notification content
     * @param notificationType notification delivery type
     * @return created notification
     * @throws IllegalArgumentException if users not found
     */
    public Notification createNotification(Long senderId, Long recipientId, String title, 
                                         String message, NotificationType notificationType) {
        User sender = userRepository.findById(senderId)
            .orElseThrow(() -> new IllegalArgumentException("Sender not found with id: " + senderId));

        User recipient = userRepository.findById(recipientId)
            .orElseThrow(() -> new IllegalArgumentException("Recipient not found with id: " + recipientId));

        Notification notification = new Notification(sender, recipient, title, message, notificationType);
        return notificationRepository.save(notification);
    }

    /**
     * Creates a system notification (sent by system user).
     * 
     * @param recipientId recipient user identifier
     * @param title notification title
     * @param message notification content
     * @return created notification
     */
    public Notification createSystemNotification(Long recipientId, String title, String message) {
        // For system notifications, we'll use the first admin user as sender
        // In production, you might want a dedicated system user
        User recipient = userRepository.findById(recipientId)
            .orElseThrow(() -> new IllegalArgumentException("Recipient not found with id: " + recipientId));

        // Create notification with recipient as both sender and receiver for system messages
        Notification notification = new Notification(recipient, recipient, title, message);
        return notificationRepository.save(notification);
    }

    /**
     * Retrieves notifications received by a user.
     * 
     * @param userId recipient user identifier
     * @param pageable pagination parameters
     * @return page of received notifications
     */
    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsByRecipientId(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientId(userId, pageable);
    }

    /**
     * Retrieves notifications sent by a user.
     * 
     * @param userId sender user identifier
     * @param pageable pagination parameters
     * @return page of sent notifications
     */
    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsBySenderId(Long userId, Pageable pageable) {
        return notificationRepository.findBySenderId(userId, pageable);
    }

    /**
     * Retrieves unread notifications for a user.
     * 
     * @param userId recipient user identifier
     * @param pageable pagination parameters
     * @return page of unread notifications
     */
    @Transactional(readOnly = true)
    public Page<Notification> getUnreadNotificationsByRecipientId(Long userId, Pageable pageable) {
        return notificationRepository.findUnreadByRecipientId(userId, pageable);
    }

    /**
     * Retrieves a specific notification by ID.
     * 
     * @param notificationId notification identifier
     * @return optional notification
     */
    @Transactional(readOnly = true)
    public Optional<Notification> getNotificationById(Long notificationId) {
        return notificationRepository.findById(notificationId);
    }

    /**
     * Marks a notification as successfully sent.
     * 
     * @param notificationId notification identifier
     * @return updated notification
     */
    public Notification markNotificationAsSent(Long notificationId) {
        Notification notification = getNotificationByIdRequired(notificationId);
        notification.markAsSent();
        return notificationRepository.save(notification);
    }

    /**
     * Marks a notification as failed with error message.
     * 
     * @param notificationId notification identifier
     * @param errorMessage error description
     * @return updated notification
     */
    public Notification markNotificationAsFailed(Long notificationId, String errorMessage) {
        Notification notification = getNotificationByIdRequired(notificationId);
        notification.markAsFailed(errorMessage);
        return notificationRepository.save(notification);
    }

    /**
     * Marks all notifications for a user as read.
     * 
     * @param userId recipient user identifier
     */
    public void markAllNotificationsAsReadForUser(Long userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    /**
     * Deletes a notification.
     * 
     * @param notificationId notification identifier
     * @throws IllegalArgumentException if notification not found
     */
    public void deleteNotification(Long notificationId) {
        if (!notificationRepository.existsById(notificationId)) {
            throw new IllegalArgumentException("Notification not found with id: " + notificationId);
        }
        notificationRepository.deleteById(notificationId);
    }

    /**
     * Sends an email notification asynchronously.
     * 
     * @param notificationId notification identifier
     * @return future with sending result
     */
    @Async
    public CompletableFuture<Boolean> sendEmailNotificationAsync(Long notificationId) {
        try {
            Notification notification = getNotificationByIdRequired(notificationId);
            
            if (!notification.isValidForEmailDelivery()) {
                String error = "Invalid email configuration for notification: " + notificationId;
                markNotificationAsFailed(notificationId, error);
                logger.warn(error);
                return CompletableFuture.completedFuture(false);
            }

            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(notification.getRecipientEmail());
            mailMessage.setSubject(notification.getTitle());
            mailMessage.setText(notification.getMessage());
            mailMessage.setFrom("noreply@airsen.fr"); // Configure this in application properties

            mailSender.send(mailMessage);
            
            markNotificationAsSent(notificationId);
            logger.info("Email notification sent successfully: {}", notificationId);
            
            return CompletableFuture.completedFuture(true);
            
        } catch (Exception e) {
            String error = "Failed to send email notification: " + e.getMessage();
            markNotificationAsFailed(notificationId, error);
            logger.error("Error sending email notification {}: {}", notificationId, e.getMessage(), e);
            
            return CompletableFuture.completedFuture(false);
        }
    }

    /**
     * Processes pending email notifications in batch.
     * 
     * @return number of notifications processed
     */
    public int processPendingEmailNotifications() {
        List<Notification> pendingNotifications = notificationRepository.findPendingEmailNotifications();
        
        int processed = 0;
        for (Notification notification : pendingNotifications) {
            try {
                sendEmailNotificationAsync(notification.getId());
                processed++;
            } catch (Exception e) {
                logger.error("Error processing pending notification {}: {}", 
                           notification.getId(), e.getMessage());
            }
        }
        
        logger.info("Processed {} pending email notifications", processed);
        return processed;
    }

    /**
     * Retries failed email notifications.
     * 
     * @return number of notifications retried
     */
    public int retryFailedNotifications() {
        List<Notification> failedNotifications = notificationRepository.findFailedNotifications();
        
        int retried = 0;
        for (Notification notification : failedNotifications) {
            try {
                // Reset status to pending before retry
                notification.setSendStatus(false);
                notification.setErrorMessage(null);
                notificationRepository.save(notification);
                
                sendEmailNotificationAsync(notification.getId());
                retried++;
                
            } catch (Exception e) {
                logger.error("Error retrying failed notification {}: {}", 
                           notification.getId(), e.getMessage());
            }
        }
        
        logger.info("Retried {} failed email notifications", retried);
        return retried;
    }

    /**
     * Gets notification statistics for a user.
     * 
     * @param userId recipient user identifier
     * @return notification statistics
     */
    @Transactional(readOnly = true)
    public NotificationStatistics getUserNotificationStatistics(Long userId) {
        long unreadCount = notificationRepository.countUnreadByRecipientId(userId);
        long successfulCount = notificationRepository.countSuccessfulByRecipientId(userId);
        long failedCount = notificationRepository.countFailedByRecipientId(userId);
        long totalCount = unreadCount + successfulCount + failedCount;
        
        return new NotificationStatistics(totalCount, unreadCount, successfulCount, failedCount);
    }

    /**
     * Gets system-wide notification statistics.
     * 
     * @return system notification statistics
     */
    @Transactional(readOnly = true)
    public NotificationStatistics getSystemNotificationStatistics() {
        long successfulCount = notificationRepository.countBySendStatus(true);
        long failedCount = notificationRepository.countBySendStatus(false);
        long totalCount = successfulCount + failedCount;
        
        return new NotificationStatistics(totalCount, 0, successfulCount, failedCount);
    }

    /**
     * Deletes old notifications before a specified date.
     * 
     * @param cutoffDate date before which notifications should be deleted
     * @return number of deleted notifications
     */
    public int deleteOldNotifications(LocalDateTime cutoffDate) {
        return notificationRepository.deleteByDateBefore(cutoffDate);
    }

    /**
     * Checks if a user has valid email for notifications.
     * 
     * @param userId user identifier
     * @return true if user has verified email
     */
    @Transactional(readOnly = true)
    public boolean isUserEmailValid(Long userId) {
        return notificationRepository.isUserEmailValid(userId);
    }

    /**
     * Gets recent notifications for a user.
     * 
     * @param userId recipient user identifier
     * @param since date from which to find notifications
     * @return list of recent notifications
     */
    @Transactional(readOnly = true)
    public List<Notification> getRecentNotificationsByRecipientId(Long userId, LocalDateTime since) {
        return notificationRepository.findRecentByRecipientId(userId, since);
    }

    /**
     * Helper method to get notification by ID with exception if not found.
     * 
     * @param notificationId notification identifier
     * @return notification
     * @throws IllegalArgumentException if not found
     */
    private Notification getNotificationByIdRequired(Long notificationId) {
        return notificationRepository.findById(notificationId)
            .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));
    }

    /**
     * Inner class for notification statistics.
     */
    public static class NotificationStatistics {
        private final long totalNotifications;
        private final long unreadNotifications;
        private final long successfulNotifications;
        private final long failedNotifications;

        public NotificationStatistics(long totalNotifications, long unreadNotifications, 
                                    long successfulNotifications, long failedNotifications) {
            this.totalNotifications = totalNotifications;
            this.unreadNotifications = unreadNotifications;
            this.successfulNotifications = successfulNotifications;
            this.failedNotifications = failedNotifications;
        }

        public long getTotalNotifications() { return totalNotifications; }
        public long getUnreadNotifications() { return unreadNotifications; }
        public long getSuccessfulNotifications() { return successfulNotifications; }
        public long getFailedNotifications() { return failedNotifications; }
        
        public double getSuccessRate() {
            return totalNotifications > 0 ? (double) successfulNotifications / totalNotifications : 0.0;
        }
        
        public double getFailureRate() {
            return totalNotifications > 0 ? (double) failedNotifications / totalNotifications : 0.0;
        }
    }
}