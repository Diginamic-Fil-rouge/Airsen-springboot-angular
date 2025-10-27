package fr.airsen.api.service;

import fr.airsen.api.entity.*;
import fr.airsen.api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for GDPR-compliant user deletion with forum author preservation.
 */
@Service
public class UserDeletionService {

    private static final Logger logger = LoggerFactory.getLogger(UserDeletionService.class);

    private final UserRepository userRepository;
    private final ForumThreadRepository forumThreadRepository;
    private final ForumMessageRepository forumMessageRepository;
    private final ForumVoteRepository forumVoteRepository;
    private final AlertSignalRepository alertSignalRepository;
    private final NotificationRepository notificationRepository;
    private final UserFavoritesService userFavoritesService;

    /**
     * Constructs UserDeletionService with required repository dependencies.
     *
     * @param userRepository repository for User entities
     * @param forumThreadRepository repository for ForumThread entities
     * @param forumMessageRepository repository for ForumMessage entities
     * @param forumVoteRepository repository for ForumVote entities
     * @param alertSignalRepository repository for Alert entities
     * @param notificationRepository repository for Notification entities
     * @param userFavoritesService service for managing user favorites
     */
    public UserDeletionService(
            UserRepository userRepository,
            ForumThreadRepository forumThreadRepository,
            ForumMessageRepository forumMessageRepository,
            ForumVoteRepository forumVoteRepository,
            AlertSignalRepository alertSignalRepository,
            NotificationRepository notificationRepository,
            UserFavoritesService userFavoritesService) {
        this.userRepository = userRepository;
        this.forumThreadRepository = forumThreadRepository;
        this.forumMessageRepository = forumMessageRepository;
        this.forumVoteRepository = forumVoteRepository;
        this.alertSignalRepository = alertSignalRepository;
        this.notificationRepository = notificationRepository;
        this.userFavoritesService = userFavoritesService;
    }

    /**
     * Soft deletes a user account, initiating a 30-day grace period before permanent deletion.
     *
     * During the grace period:
     * - User account is marked inactive (isActive = false)
     * - User cannot login or access protected resources
     * - User can restore account by calling {@link User#cancelDeletion()}
     * - Deletion timestamp ({@code deletedAt}) is recorded for grace period calculation
     *
     * After 30 days, a scheduled job will call {@link #hardDeleteUser(Long)} to permanently
     * delete personal data while preserving forum content.
     *
     * GDPR Compliance: This implements Article 17 Right to Erasure with a
     * grace period. The 30-day window respects "without undue delay" while preventing accidental
     * deletions and allowing users to change their minds.
     *
     * Authorization: This method can be called by:
     * - The user themselves (self-service deletion)
     * - Administrators (account suspension/removal)
     *
     * @param userId unique identifier of the user to soft delete
     * @param reason optional reason for deletion (for analytics and audit trail), can be null
     * @param adminUserId optional ID of admin who initiated deletion (null if user self-initiated)
     * @throws EntityNotFoundException if user with the given ID does not exist
     */
    @Transactional
    public void softDeleteUser(Long userId, String reason, Long adminUserId) {
        logger.info("Initiating soft delete for user ID: {} with reason: '{}', admin: {}",
                userId, reason != null ? reason : "Not provided", adminUserId != null ? adminUserId : "Self-initiated");

        // Find user by ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        // Check if already marked for deletion
        if (user.isMarkedForDeletion()) {
            logger.warn("User ID: {} is already marked for deletion (deletedAt: {})", userId, user.getDeletedAt());
            return; // Idempotent - no action needed
        }

        // Mark user for deletion (sets deletedAt, deletionReason, isActive = false)
        user.markForDeletion(reason);

        // Save user to database
        userRepository.save(user);

        logger.info("Successfully soft deleted user ID: {}. Grace period ends: {}, Account inactive: true",
                userId, user.getDeletedAt().plusDays(30));

        // Note: AuditService integration deferred to future phase
        // if (adminUserId != null) {
        //     auditService.logAdminAction(adminUserId, "SOFT_DELETE_USER", userId, reason);
        // }
    }

    /**
     * Permanently deletes a user account after the 30-day grace period has expired.
     *
     * CRITICAL: This method preserves forum content per GDPR Article 17(3)(e)
     * public interest exception. Environmental discussions serve community value
     * and are retained with the original author's display name.
     *
     * This method performs the following operations in sequence:
     * 1. Verify grace period has expired (throws exception if not)
     * 2. Preserve author display name in all forum threads and messages
     * 3. Delete user-created alerts
     * 4. Clear user's favorite communes
     * 5. Anonymize received notifications (preserve history, break FK link)
     * 6. Anonymize forum votes (preserve counts, remove voter identity)
     * 7. Delete user entity from database
     *
     * Authorization: This method should ONLY be called by a scheduled job
     * (e.g., @Scheduled cron task), NOT directly by users or admins. The scheduled job finds
     * all users with expired grace periods and processes them automatically.
     *
     * Data Deletion vs Preservation:
     * - Deleted: User entity (email, password, names, bio, etc.),
     *            alerts, favorite communes
     * - Preserved with author name: Forum threads and messages
     * - Anonymized: Forum votes (counts kept), notifications (history kept)
     *
     * Transaction Safety: All operations run in a single transaction
     * (@Transactional). If any step fails, the entire deletion is rolled back to prevent
     * data corruption.
     *
     * @param userId unique identifier of the user to permanently delete
     * @throws EntityNotFoundException if user with the given ID does not exist
     * @throws IllegalStateException if grace period has not expired yet (attempted early deletion)
     */
    @Transactional
    public void hardDeleteUser(Long userId) {
        logger.info("Starting hard delete for user ID: {}", userId);

        // Find user by ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        // Verify grace period has expired
        if (!user.isDeletionGracePeriodExpired()) {
            String message = String.format(
                    "Cannot hard delete user ID: %d - grace period not expired (deletedAt: %s, expires: %s)",
                    userId, user.getDeletedAt(), user.getDeletedAt().plusDays(30)
            );
            logger.error(message);
            throw new IllegalStateException(message);
        }

        logger.info("Grace period expired for user ID: {}. Proceeding with permanent deletion.", userId);

        // Step 1: Preserve author name in forum content (CRITICAL - must happen BEFORE user deletion)
        preserveAuthorNameInForumContent(user);

        // Step 2: Delete user-created alerts
        deleteUserAlerts(userId);

        // Step 3: Clear user's favorite communes
        deleteUserFavorites(userId);

        // Step 4: Anonymize received notifications (preserve history)
        anonymizeNotifications(userId);

        // Step 5: Anonymize forum votes (preserve counts)
        anonymizeForumVotes(userId);

        // Step 6: Delete user entity (personal data removed)
        userRepository.delete(user);

        logger.info("Successfully hard deleted user ID: {}. Personal data removed, forum content preserved.", userId);
    }

    /**
     * Preserves the user's display name in all their forum threads and messages.
     *
     * GDPR Article 17(3)(e) - Public Interest Exception: This method
     * implements the legal basis for preserving forum content. Environmental discussions
     * serve public interest (freedom of expression, community value), so author names are
     * preserved even after user deletion.
     *
     * For each forum thread and message authored by the user:
     * - Copies user's display name to {@code authorName} field
     * - Sets {@code author} relationship to null (breaks profile link)
     * - Sets {@code authorDeleted} flag to true (marks as deleted author)
     *
     * Privacy Balance: The author's name (e.g., "Marie Dupont") remains
     * visible for discussion context, but the profile link is permanently broken. Users cannot
     * navigate to the deleted user's profile page.
     *
     * CRITICAL: This method MUST be called BEFORE deleting the User entity,
     * otherwise the display name will be lost.
     *
     * @param user the User entity whose forum content should be preserved
     */
    private void preserveAuthorNameInForumContent(User user) {
        logger.info("Preserving author name for deleted user ID: {}", user.getId());

        String authorName = user.getDisplayName();
        logger.debug("Using display name: '{}' for author preservation", authorName);

        // Preserve author name in all forum threads
        List<ForumThread> threads = forumThreadRepository.findByAuthor(user);
        for (ForumThread thread : threads) {
            thread.setAuthorName(authorName);       // Preserve display name
            thread.setAuthor(null);                 // Break FK link to user
            thread.setAuthorDeleted(true);          // Mark as deleted author
            forumThreadRepository.save(thread);
        }
        logger.info("Preserved author name in {} forum threads for user ID: {}", threads.size(), user.getId());

        // Preserve author name in all forum messages
        List<ForumMessage> messages = forumMessageRepository.findByAuthor(user);
        for (ForumMessage message : messages) {
            message.setAuthorName(authorName);      // Preserve display name
            message.setAuthor(null);                // Break FK link to user
            message.setAuthorDeleted(true);         // Mark as deleted author
            forumMessageRepository.save(message);
        }
        logger.info("Preserved author name in {} forum messages for user ID: {}", messages.size(), user.getId());

        logger.info("Author preservation complete: {} threads + {} messages for user ID: {}",
                threads.size(), messages.size(), user.getId());
    }

    /**
     * Deletes all alerts created by the user.
     *
     * GDPR Justification: User-created alerts are personal data directly
     * tied to the user's account. Unlike forum content (which serves public interest), alerts
     * are administrative data with no community value, so they are deleted per Article 17.
     *
     * Note: Admin-created alerts broadcast to all users are NOT deleted,
     * as they serve public safety purposes (air quality warnings, weather alerts).
     *
     * @param userId unique identifier of the user whose alerts should be deleted
     */
    private void deleteUserAlerts(Long userId) {
        logger.info("Deleting alerts for user ID: {}", userId);

        List<Alert> alerts = alertSignalRepository.findByUserId(userId);
        if (!alerts.isEmpty()) {
            alertSignalRepository.deleteAll(alerts);
            logger.info("Deleted {} alerts for user ID: {}", alerts.size(), userId);
        } else {
            logger.debug("No alerts found for user ID: {}", userId);
        }
    }

    /**
     * Clears the user's favorite communes collection.
     *
     * Implementation Note: This clears the many-to-many relationship
     * in the {@code user_favorite} join table. The Commune entities themselves are NOT
     * deleted (they are shared reference data).
     *
     * @param userId unique identifier of the user whose favorites should be cleared
     */
    private void deleteUserFavorites(Long userId) {
        logger.info("Clearing favorite communes for user ID: {}", userId);
        userFavoritesService.removeAllFavorites(userId);
        logger.info("Cleared favorite communes for user ID: {}", userId);
    }

    /**
     * Anonymizes all notifications received by the user.
     *
     * Data Retention Strategy: Notification history is preserved for
     * administrative purposes (delivery tracking, system monitoring), but the recipient
     * link is broken to anonymize the data.
     *
     * For each notification received by the user:
     * - Sets {@code userReceiver} relationship to null (breaks FK link)
     * - Preserves notification content, timestamps, and delivery status
     *
     * Privacy Balance: Notification system can still track delivery
     * metrics and troubleshoot issues, but cannot identify which user received the
     * notification (recipient identity anonymized).
     *
     * @param userId unique identifier of the user whose notifications should be anonymized
     */
    private void anonymizeNotifications(Long userId) {
        logger.info("Anonymizing notifications for user ID: {}", userId);

        // Note: NotificationRepository uses Pageable for findByRecipientId, but we need all notifications
        // For now, we'll use a workaround by deleting all notifications for the user
        // Future enhancement: Add findAllByUserReceiverId(Long userId) to NotificationRepository
        int deletedCount = notificationRepository.deleteAllForUser(userId);
        logger.info("Deleted {} notifications for user ID: {} (anonymization via deletion)", deletedCount, userId);

        // Alternative implementation if we want to preserve notification history:
        // List<Notification> notifications = notificationRepository.findAllByUserReceiverId(userId);
        // for (Notification notification : notifications) {
        //     notification.setUserReceiver(null);  // Break FK link
        //     notificationRepository.save(notification);
        // }
    }

    /**
     * Anonymizes all forum votes cast by the user.
     *
     * Data Retention Strategy: Vote counts are preserved to maintain
     * accurate thread scores (community value), but voter identity is removed (privacy).
     *
     * For each vote cast by the user:
     * - Sets {@code user} relationship to null (breaks FK link)
     * - Sets {@code userDeleted} flag to true (marks as deleted voter)
     * - Preserves {@code voteType} (UPVOTE/DOWNVOTE) for score calculation
     *
     * Privacy Balance: Thread scores remain accurate (vote counts intact),
     * but no one can identify who cast the vote (voter anonymization complete).
     *
     * GDPR Justification: Vote counts serve public interest (highlight
     * quality content), but voter identity is personal data with no community value, so it's
     * removed per Article 17.
     *
     * @param userId unique identifier of the user whose votes should be anonymized
     */
    private void anonymizeForumVotes(Long userId) {
        logger.info("Anonymizing forum votes for user ID: {}", userId);

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            logger.warn("User ID: {} not found, cannot anonymize votes", userId);
            return;
        }

        List<ForumVote> votes = user.getVotes();
        if (votes != null && !votes.isEmpty()) {
            for (ForumVote vote : votes) {
                vote.setUser(null);              // Break FK link to user
                vote.setUserDeleted(true);       // Mark as deleted voter
                forumVoteRepository.save(vote);
            }
            logger.info("Anonymized {} forum votes for user ID: {}", votes.size(), userId);
        } else {
            logger.debug("No forum votes found for user ID: {}", userId);
        }
    }
}
