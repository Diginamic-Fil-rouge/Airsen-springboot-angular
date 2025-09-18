package fr.airsen.api.dto.response;

/**
 * Data Transfer Object for alert statistics and metrics.
 * 
 * This DTO provides summary information about user alert usage
 * and system-wide alert metrics for reporting and dashboard purposes.
 */
public class AlertStatisticsDTO {

    /**
     * Number of active alerts for the user.
     */
    private Long activeAlerts;

    /**
     * Total number of alerts (active and inactive) for the user.
     */
    private Long totalAlerts;

    /**
     * Maximum allowed alerts per user.
     */
    private Integer maxAllowedAlerts;

    /**
     * Whether the user has reached the alert limit.
     */
    private Boolean atLimit;

    /**
     * Number of remaining alerts the user can create.
     */
    private Integer remainingAlerts;

    /**
     * Percentage of alert quota used.
     */
    private Double quotaUsagePercentage;


    public AlertStatisticsDTO() {}

    public AlertStatisticsDTO(Long activeAlerts, Long totalAlerts, Integer maxAllowedAlerts) {
        this.activeAlerts = activeAlerts;
        this.totalAlerts = totalAlerts;
        this.maxAllowedAlerts = maxAllowedAlerts;
        this.atLimit = activeAlerts >= maxAllowedAlerts;
        this.remainingAlerts = Math.max(0, maxAllowedAlerts - activeAlerts.intValue());
        this.quotaUsagePercentage = maxAllowedAlerts > 0 ? 
            (activeAlerts.doubleValue() / maxAllowedAlerts) * 100 : 0.0;
    }

    // Getters and Setters

    public Long getActiveAlerts() {
        return activeAlerts;
    }

    public void setActiveAlerts(Long activeAlerts) {
        this.activeAlerts = activeAlerts;
        recalculateFields();
    }

    public Long getTotalAlerts() {
        return totalAlerts;
    }

    public void setTotalAlerts(Long totalAlerts) {
        this.totalAlerts = totalAlerts;
    }

    public Integer getMaxAllowedAlerts() {
        return maxAllowedAlerts;
    }

    public void setMaxAllowedAlerts(Integer maxAllowedAlerts) {
        this.maxAllowedAlerts = maxAllowedAlerts;
        recalculateFields();
    }

    public Boolean getAtLimit() {
        return atLimit;
    }

    public Integer getRemainingAlerts() {
        return remainingAlerts;
    }

    public Double getQuotaUsagePercentage() {
        return quotaUsagePercentage;
    }

    /**
     * Gets the number of inactive alerts.
     * 
     * @return number of inactive alerts
     */
    public Long getInactiveAlerts() {
        return totalAlerts - activeAlerts;
    }

    /**
     * Gets the quota usage as a formatted percentage string.
     * 
     * @return formatted percentage (e.g., "75.5%")
     */
    public String getFormattedQuotaUsage() {
        return String.format("%.1f%%", quotaUsagePercentage);
    }

    /**
     * Gets a textual description of the quota status.
     * 
     * @return quota status description
     */
    public String getQuotaStatusDescription() {
        if (atLimit) {
            return "Alert limit reached";
        } else if (quotaUsagePercentage >= 90) {
            return "Near alert limit";
        } else if (quotaUsagePercentage >= 75) {
            return "High alert usage";
        } else if (quotaUsagePercentage >= 50) {
            return "Moderate alert usage";
        } else {
            return "Low alert usage";
        }
    }

    /**
     * Checks if the user can create more alerts.
     * 
     * @return true if user can create more alerts
     */
    public boolean canCreateMoreAlerts() {
        return !atLimit;
    }

    /**
     * Recalculates derived fields when base values change.
     */
    private void recalculateFields() {
        if (activeAlerts != null && maxAllowedAlerts != null) {
            this.atLimit = activeAlerts >= maxAllowedAlerts;
            this.remainingAlerts = Math.max(0, maxAllowedAlerts - activeAlerts.intValue());
            this.quotaUsagePercentage = maxAllowedAlerts > 0 ? 
                (activeAlerts.doubleValue() / maxAllowedAlerts) * 100 : 0.0;
        }
    }

    @Override
    public String toString() {
        return "AlertStatisticsDTO{" +
                "activeAlerts=" + activeAlerts +
                ", totalAlerts=" + totalAlerts +
                ", maxAllowedAlerts=" + maxAllowedAlerts +
                ", atLimit=" + atLimit +
                ", remainingAlerts=" + remainingAlerts +
                ", quotaUsagePercentage=" + quotaUsagePercentage +
                '}';
    }
}