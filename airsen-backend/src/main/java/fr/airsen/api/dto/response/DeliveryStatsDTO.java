package fr.airsen.api.dto.response;

/**
 * Data Transfer Object for delivery statistics and performance metrics.
 * 
 * This DTO provides comprehensive delivery performance information
 * for monitoring alert system effectiveness and troubleshooting.
 */
public class DeliveryStatsDTO {

    /**
     * Total number of delivery attempts.
     */
    private Long totalDeliveries;

    /**
     * Number of successful deliveries.
     */
    private Long successfulDeliveries;

    /**
     * Number of failed deliveries.
     */
    private Long failedDeliveries;

    /**
     * Number of pending deliveries.
     */
    private Long pendingDeliveries;

    /**
     * Success rate as a percentage (0-100).
     */
    private Double successRate;

    /**
     * Failure rate as a percentage (0-100).
     */
    private Double failureRate;

    /**
     * Time period for these statistics (e.g., "Last 24 hours").
     */
    private String timePeriod;

    /**
     * Default constructor.
     */
    public DeliveryStatsDTO() {}

    /**
     * Constructor with delivery counts.
     * 
     * @param totalDeliveries total delivery attempts
     * @param successfulDeliveries successful deliveries
     * @param failedDeliveries failed deliveries
     * @param pendingDeliveries pending deliveries
     */
    public DeliveryStatsDTO(Long totalDeliveries, Long successfulDeliveries, 
                           Long failedDeliveries, Long pendingDeliveries) {
        this.totalDeliveries = totalDeliveries;
        this.successfulDeliveries = successfulDeliveries;
        this.failedDeliveries = failedDeliveries;
        this.pendingDeliveries = pendingDeliveries;
        calculateRates();
    }

    /**
     * Constructor with delivery counts and time period.
     * 
     * @param totalDeliveries total delivery attempts
     * @param successfulDeliveries successful deliveries
     * @param failedDeliveries failed deliveries
     * @param pendingDeliveries pending deliveries
     * @param timePeriod time period description
     */
    public DeliveryStatsDTO(Long totalDeliveries, Long successfulDeliveries, 
                           Long failedDeliveries, Long pendingDeliveries, String timePeriod) {
        this(totalDeliveries, successfulDeliveries, failedDeliveries, pendingDeliveries);
        this.timePeriod = timePeriod;
    }

    // Getters and Setters

    public Long getTotalDeliveries() {
        return totalDeliveries;
    }

    public void setTotalDeliveries(Long totalDeliveries) {
        this.totalDeliveries = totalDeliveries;
        calculateRates();
    }

    public Long getSuccessfulDeliveries() {
        return successfulDeliveries;
    }

    public void setSuccessfulDeliveries(Long successfulDeliveries) {
        this.successfulDeliveries = successfulDeliveries;
        calculateRates();
    }

    public Long getFailedDeliveries() {
        return failedDeliveries;
    }

    public void setFailedDeliveries(Long failedDeliveries) {
        this.failedDeliveries = failedDeliveries;
        calculateRates();
    }

    public Long getPendingDeliveries() {
        return pendingDeliveries;
    }

    public void setPendingDeliveries(Long pendingDeliveries) {
        this.pendingDeliveries = pendingDeliveries;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public Double getFailureRate() {
        return failureRate;
    }

    public String getTimePeriod() {
        return timePeriod;
    }

    public void setTimePeriod(String timePeriod) {
        this.timePeriod = timePeriod;
    }

    /**
     * Gets the number of completed deliveries (successful + failed).
     * 
     * @return completed deliveries count
     */
    public Long getCompletedDeliveries() {
        return successfulDeliveries + failedDeliveries;
    }

    /**
     * Gets the pending rate as a percentage.
     * 
     * @return pending rate percentage
     */
    public Double getPendingRate() {
        return totalDeliveries > 0 ? (pendingDeliveries.doubleValue() / totalDeliveries) * 100 : 0.0;
    }

    /**
     * Gets the success rate as a formatted percentage string.
     * 
     * @return formatted success rate (e.g., "95.5%")
     */
    public String getFormattedSuccessRate() {
        return String.format("%.1f%%", successRate);
    }

    /**
     * Gets the failure rate as a formatted percentage string.
     * 
     * @return formatted failure rate (e.g., "4.5%")
     */
    public String getFormattedFailureRate() {
        return String.format("%.1f%%", failureRate);
    }

    /**
     * Gets the pending rate as a formatted percentage string.
     * 
     * @return formatted pending rate (e.g., "0.5%")
     */
    public String getFormattedPendingRate() {
        return String.format("%.1f%%", getPendingRate());
    }

    /**
     * Gets a textual description of the delivery performance.
     * 
     * @return performance description
     */
    public String getPerformanceDescription() {
        if (successRate >= 95) {
            return "Excellent";
        } else if (successRate >= 90) {
            return "Good";
        } else if (successRate >= 80) {
            return "Fair";
        } else if (successRate >= 70) {
            return "Poor";
        } else {
            return "Critical";
        }
    }

    /**
     * Checks if there are delivery issues that need attention.
     * 
     * @return true if failure rate is high or many pending
     */
    public boolean hasDeliveryIssues() {
        return failureRate > 10 || getPendingRate() > 5;
    }

    /**
     * Gets a summary of delivery statistics.
     * 
     * @return delivery summary
     */
    public String getSummary() {
        return String.format("Deliveries: %d total, %d successful (%.1f%%), %d failed (%.1f%%), %d pending",
            totalDeliveries, successfulDeliveries, successRate, failedDeliveries, failureRate, pendingDeliveries);
    }

    /**
     * Calculates success and failure rates based on current values.
     */
    private void calculateRates() {
        if (totalDeliveries != null && totalDeliveries > 0) {
            this.successRate = successfulDeliveries != null ? 
                (successfulDeliveries.doubleValue() / totalDeliveries) * 100 : 0.0;
            this.failureRate = failedDeliveries != null ? 
                (failedDeliveries.doubleValue() / totalDeliveries) * 100 : 0.0;
        } else {
            this.successRate = 0.0;
            this.failureRate = 0.0;
        }
    }

    @Override
    public String toString() {
        return "DeliveryStatsDTO{" +
                "totalDeliveries=" + totalDeliveries +
                ", successfulDeliveries=" + successfulDeliveries +
                ", failedDeliveries=" + failedDeliveries +
                ", pendingDeliveries=" + pendingDeliveries +
                ", successRate=" + successRate +
                ", failureRate=" + failureRate +
                ", timePeriod='" + timePeriod + '\'' +
                '}';
    }
}