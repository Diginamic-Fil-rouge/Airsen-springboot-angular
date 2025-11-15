package fr.airsen.api.dto.response;

/**
 * DTO for admin dashboard statistics.
 *
 * Contains aggregated metrics for the admin dashboard overview.
 */
public class AdminStatisticsDTO {

    private Long totalUsers;
    private Long activeUsers;
    private Long suspendedUsers;
    private Long newUsersThisWeek;
    private Long totalAlerts;
    private Long activeAlerts;
    private Long totalCampaigns;
    private Long campaignsInProgress;
    private Long totalForumThreads;
    private Long totalNotificationsSent;

    public AdminStatisticsDTO() {
    }

    public AdminStatisticsDTO(Long totalUsers, Long activeUsers, Long suspendedUsers,
                             Long newUsersThisWeek, Long totalAlerts, Long activeAlerts,
                             Long totalCampaigns, Long campaignsInProgress,
                             Long totalForumThreads, Long totalNotificationsSent) {
        this.totalUsers = totalUsers;
        this.activeUsers = activeUsers;
        this.suspendedUsers = suspendedUsers;
        this.newUsersThisWeek = newUsersThisWeek;
        this.totalAlerts = totalAlerts;
        this.activeAlerts = activeAlerts;
        this.totalCampaigns = totalCampaigns;
        this.campaignsInProgress = campaignsInProgress;
        this.totalForumThreads = totalForumThreads;
        this.totalNotificationsSent = totalNotificationsSent;
    }

    // Getters and Setters

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Long getActiveUsers() {
        return activeUsers;
    }

    public void setActiveUsers(Long activeUsers) {
        this.activeUsers = activeUsers;
    }

    public Long getSuspendedUsers() {
        return suspendedUsers;
    }

    public void setSuspendedUsers(Long suspendedUsers) {
        this.suspendedUsers = suspendedUsers;
    }

    public Long getNewUsersThisWeek() {
        return newUsersThisWeek;
    }

    public void setNewUsersThisWeek(Long newUsersThisWeek) {
        this.newUsersThisWeek = newUsersThisWeek;
    }

    public Long getTotalAlerts() {
        return totalAlerts;
    }

    public void setTotalAlerts(Long totalAlerts) {
        this.totalAlerts = totalAlerts;
    }

    public Long getActiveAlerts() {
        return activeAlerts;
    }

    public void setActiveAlerts(Long activeAlerts) {
        this.activeAlerts = activeAlerts;
    }

    public Long getTotalCampaigns() {
        return totalCampaigns;
    }

    public void setTotalCampaigns(Long totalCampaigns) {
        this.totalCampaigns = totalCampaigns;
    }

    public Long getCampaignsInProgress() {
        return campaignsInProgress;
    }

    public void setCampaignsInProgress(Long campaignsInProgress) {
        this.campaignsInProgress = campaignsInProgress;
    }

    public Long getTotalForumThreads() {
        return totalForumThreads;
    }

    public void setTotalForumThreads(Long totalForumThreads) {
        this.totalForumThreads = totalForumThreads;
    }

    public Long getTotalNotificationsSent() {
        return totalNotificationsSent;
    }

    public void setTotalNotificationsSent(Long totalNotificationsSent) {
        this.totalNotificationsSent = totalNotificationsSent;
    }

    @Override
    public String toString() {
        return "AdminStatisticsDTO{" +
                "totalUsers=" + totalUsers +
                ", activeUsers=" + activeUsers +
                ", suspendedUsers=" + suspendedUsers +
                ", newUsersThisWeek=" + newUsersThisWeek +
                ", totalAlerts=" + totalAlerts +
                ", activeAlerts=" + activeAlerts +
                ", totalCampaigns=" + totalCampaigns +
                ", campaignsInProgress=" + campaignsInProgress +
                ", totalForumThreads=" + totalForumThreads +
                ", totalNotificationsSent=" + totalNotificationsSent +
                '}';
    }
}
