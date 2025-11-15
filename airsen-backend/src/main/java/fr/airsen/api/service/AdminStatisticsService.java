package fr.airsen.api.service;

import fr.airsen.api.dto.response.AdminStatisticsDTO;
import fr.airsen.api.entity.enums.CampaignStatus;
import fr.airsen.api.repository.AlertSignalRepository;
import fr.airsen.api.repository.ForumThreadRepository;
import fr.airsen.api.repository.NotificationCampaignRepository;
import fr.airsen.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service for aggregating admin dashboard statistics.
 *
 * Provides various metrics and counts for the admin overview dashboard.
 * Statistics are cached for 5 minutes to improve performance.
 */
@Service
public class AdminStatisticsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AlertSignalRepository alertSignalRepository;

    @Autowired
    private NotificationCampaignRepository notificationCampaignRepository;

    @Autowired
    private ForumThreadRepository forumThreadRepository;

    /**
     * Get comprehensive admin statistics.
     *
     * Aggregates data from multiple repositories to provide a dashboard overview.
     * Results are cached for 5 minutes.
     *
     * @return AdminStatisticsDTO with all metrics
     */
    @Cacheable(value = "adminStatistics", unless = "#result == null")
    public AdminStatisticsDTO getStatistics() {
        AdminStatisticsDTO stats = new AdminStatisticsDTO();

        // User statistics
        stats.setTotalUsers(userRepository.count());
        stats.setActiveUsers(countActiveUsers());
        stats.setSuspendedUsers(countSuspendedUsers());
        stats.setNewUsersThisWeek(countNewUsersThisWeek());

        // Alert statistics
        stats.setTotalAlerts(alertSignalRepository.count());
        stats.setActiveAlerts(countActiveAlerts());

        // Campaign statistics
        stats.setTotalCampaigns(notificationCampaignRepository.count());
        stats.setCampaignsInProgress(countCampaignsInProgress());

        // Forum statistics
        stats.setTotalForumThreads(forumThreadRepository.count());

        // Notification statistics
        stats.setTotalNotificationsSent(countTotalNotificationsSent());

        return stats;
    }

    /**
     * Count active users (isActive = true).
     */
    private Long countActiveUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getIsActive() != null && user.getIsActive())
                .count();
    }

    /**
     * Count suspended users (isActive = false).
     */
    private Long countSuspendedUsers() {
        return userRepository.findAll().stream()
                .filter(user -> user.getIsActive() != null && !user.getIsActive())
                .count();
    }

    /**
     * Count users created in the last 7 days.
     */
    private Long countNewUsersThisWeek() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        return userRepository.findAll().stream()
                .filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(oneWeekAgo))
                .count();
    }

    /**
     * Count active alert signals (validTo is null or in the future).
     */
    private Long countActiveAlerts() {
        LocalDateTime now = LocalDateTime.now();
        return alertSignalRepository.findAll().stream()
                .filter(alert -> alert.getValidTo() == null || alert.getValidTo().isAfter(now))
                .count();
    }

    /**
     * Count campaigns currently in progress (status = SENDING).
     */
    private Long countCampaignsInProgress() {
        try {
            return notificationCampaignRepository.findAll().stream()
                    .filter(campaign -> campaign.getStatus() == CampaignStatus.SENDING)
                    .count();
        } catch (Exception e) {
            // If CampaignStatus enum doesn't exist, return 0
            return 0L;
        }
    }

    /**
     * Count total notifications sent across all campaigns.
     */
    private Long countTotalNotificationsSent() {
        return notificationCampaignRepository.findAll().stream()
                .mapToLong(campaign -> {
                    Integer sentCount = campaign.getSentCount();
                    return sentCount != null ? sentCount.longValue() : 0L;
                })
                .sum();
    }
}
