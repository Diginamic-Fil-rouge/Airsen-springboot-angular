package fr.airsen.api.repository;

import fr.airsen.api.entity.Notification;
import fr.airsen.api.entity.enums.NotificationChannel;
import fr.airsen.api.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {


    @Query("SELECT n FROM Notification n WHERE n.userReceiver.id = :userId ORDER BY n.createdDate DESC")
    Page<Notification> findByRecipientId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Finds notifications sent by a specific user.
     * 
     * @param userId sender user identifier
     * @param pageable pagination parameters
     * @return page of notifications sent by the user
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdDate DESC")
    Page<Notification> findBySenderId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Finds unread notifications for a user.
     * Unread notifications are those with readStatus = false.
     * 
     * @param userId recipient user identifier
     * @param pageable pagination parameters
     * @return page of unread notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.userReceiver.id = :userId AND n.readStatus = false ORDER BY n.createdDate DESC")
    Page<Notification> findUnreadByRecipientId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Finds notifications by read status.
     * 
     * @param readStatus read/unread status
     * @param pageable pagination parameters
     * @return page of notifications with the specified status
     */
    Page<Notification> findByReadStatus(Boolean readStatus, Pageable pageable);

    /**
     * Finds pending notifications for processing.
     * Pending notifications have readStatus = false and no error message.
     * 
     * @return list of pending notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.readStatus = false AND (n.errorMessage IS NULL OR n.errorMessage = '') ORDER BY n.createdDate ASC")
    List<Notification> findPendingNotifications();

    /**
     * Finds failed notifications for retry processing.
     * Failed notifications have readStatus = false and an error message.
     * 
     * @return list of failed notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.readStatus = false AND n.errorMessage IS NOT NULL AND n.errorMessage != '' ORDER BY n.createdDate DESC")
    List<Notification> findFailedNotifications();

    /**
     * Finds notifications by delivery channel.
     * 
     * @param channel notification delivery channel
     * @param pageable pagination parameters
     * @return page of notifications using the specified channel
     */
    Page<Notification> findBySendChannel(NotificationChannel channel, Pageable pageable);

    /**
     * Finds notifications by type.
     * 
     * @param notificationType notification type
     * @param pageable pagination parameters
     * @return page of notifications of the specified type
     */
    Page<Notification> findByNotificationType(NotificationType notificationType, Pageable pageable);

    /**
     * Finds notifications within a date range.
     * 
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination parameters
     * @return page of notifications within the date range
     */
    @Query("SELECT n FROM Notification n WHERE n.createdDate BETWEEN :startDate AND :endDate ORDER BY n.createdDate DESC")
    Page<Notification> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      Pageable pageable);

    /**
     * Finds notifications for a user within a date range.
     * 
     * @param userId recipient user identifier
     * @param startDate start of date range
     * @param endDate end of date range
     * @param pageable pagination parameters
     * @return page of notifications for the user within the date range
     */
    @Query("SELECT n FROM Notification n WHERE n.userReceiver.id = :userId AND n.createdDate BETWEEN :startDate AND :endDate ORDER BY n.createdDate DESC")
    Page<Notification> findByRecipientIdAndDateRange(@Param("userId") Long userId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate,
                                                     Pageable pageable);

    /**
     * Finds recent notifications for a user.
     * 
     * @param userId recipient user identifier
     * @param since date from which to find notifications
     * @return list of recent notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.userReceiver.id = :userId AND n.createdDate > :since ORDER BY n.createdDate DESC")
    List<Notification> findRecentByRecipientId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Counts unread notifications for a user.
     * 
     * @param userId recipient user identifier
     * @return count of unread notifications
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userReceiver.id = :userId AND n.readStatus = false")
    long countUnreadByRecipientId(@Param("userId") Long userId);

    /**
     * Counts successful notifications for a user.
     * 
     * @param userId recipient user identifier
     * @return count of successful notifications
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userReceiver.id = :userId AND n.readStatus = true")
    long countSuccessfulByRecipientId(@Param("userId") Long userId);

    /**
     * Counts failed notifications for a user.
     * 
     * @param userId recipient user identifier
     * @return count of failed notifications
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userReceiver.id = :userId AND n.readStatus = false AND n.errorMessage IS NOT NULL AND n.errorMessage != ''")
    long countFailedByRecipientId(@Param("userId") Long userId);

    /**
     * Counts notifications by read status.
     * 
     * @param readStatus read/unread status
     * @return count of notifications with the specified status
     */
    long countByReadStatus(Boolean readStatus);

    /**
     * Marks a notification as successfully sent.
     * 
     * @param notificationId notification identifier
     * @param sentDate date when notification was sent
     */
    @Modifying
    @Query("UPDATE Notification n SET n.readStatus = true, n.sentDate = :sentDate, n.errorMessage = null WHERE n.id = :notificationId")
    void markAsSent(@Param("notificationId") Long notificationId, @Param("sentDate") LocalDateTime sentDate);

    /**
     * Marks a notification as failed with an error message.
     * 
     * @param notificationId notification identifier
     * @param errorMessage error description
     */
    @Modifying
    @Query("UPDATE Notification n SET n.readStatus = false, n.errorMessage = :errorMessage, n.sentDate = null WHERE n.id = :notificationId")
    void markAsFailed(@Param("notificationId") Long notificationId, @Param("errorMessage") String errorMessage);

    /**
     * Updates notification delivery status.
     * 
     * @param notificationId notification identifier
     * @param readStatus new read status
     */
    @Modifying
    @Query("UPDATE Notification n SET n.readStatus = :readStatus WHERE n.id = :notificationId")
    void updateReadStatus(@Param("notificationId") Long notificationId, @Param("readStatus") Boolean readStatus);

    /**
     * Marks all notifications for a user as read.
     * Used when user marks all notifications as read.
     * 
     * @param userId recipient user identifier
     */
    @Modifying
    @Query("UPDATE Notification n SET n.readStatus = true WHERE n.userReceiver.id = :userId AND n.readStatus = false")
    void markAllAsReadForUser(@Param("userId") Long userId);

    /**
     * Deletes notifications older than a specified date.
     * Used for data retention policies.
     * 
     * @param cutoffDate date before which notifications should be deleted
     * @return number of deleted notifications
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.createdDate < :cutoffDate")
    int deleteByDateBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Deletes all notifications for a user.
     * Used when user account is deleted.
     * 
     * @param userId user identifier
     * @return number of deleted notifications
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userReceiver.id = :userId OR n.user.id = :userId")
    int deleteAllForUser(@Param("userId") Long userId);

    /**
     * Finds notification delivery statistics within a date range.
     * Returns aggregated data for reporting.
     * 
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of statistics [readStatus, count]
     */
    @Query("SELECT n.readStatus, COUNT(n) FROM Notification n WHERE n.createdDate BETWEEN :startDate AND :endDate GROUP BY n.readStatus")
    List<Object[]> findDeliveryStats(@Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Finds notification type statistics within a date range.
     * 
     * @param startDate start of date range
     * @param endDate end of date range
     * @return list of statistics [notificationType, count]
     */
    @Query("SELECT n.notificationType, COUNT(n) FROM Notification n WHERE n.createdDate BETWEEN :startDate AND :endDate GROUP BY n.notificationType")
    List<Object[]> findTypeStats(@Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    /**
     * Finds notifications that need email delivery.
     * Used by email service to process notifications.
     * 
     * @return list of notifications requiring email delivery
     */
    @Query("SELECT n FROM Notification n WHERE n.readStatus = false AND n.sendChannel = 'EMAIL' AND (n.errorMessage IS NULL OR n.errorMessage = '') ORDER BY n.createdDate ASC")
    List<Notification> findPendingEmailNotifications();

    /**
     * Checks if a user has valid email for notification delivery.
     * 
     * @param userId user identifier
     * @return true if user has verified email
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.id = :userId AND u.emailVerified = true AND u.email IS NOT NULL AND u.email != ''")
    boolean isUserEmailValid(@Param("userId") Long userId);
}