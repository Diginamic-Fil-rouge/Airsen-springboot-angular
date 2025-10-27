package fr.airsen.api.dto.response;

/**
 * Response DTO for notification campaign delivery statistics.
 *
 * Provides aggregated metrics for campaign delivery performance and
 * is used by admin dashboard to display campaign summary information.
 */
public record CampaignStatisticsDTO(

    Long campaignId,

    Integer totalRecipients,

    Integer sentCount,

    Integer failedCount,

    Integer pendingCount,

    Double deliveryRate
) {
}
