package fr.airsen.api.dto.response;

/**
 * DTO for campaign retry operation response.
 * Returns the number of successfully retried notifications.
 */
public record CampaignRetryResponseDTO(
    Long campaignId,
    Integer retriedCount,
    String message
) {
    public static CampaignRetryResponseDTO of(Long campaignId, Integer retriedCount) {
        return new CampaignRetryResponseDTO(
            campaignId,
            retriedCount,
            String.format("Successfully retried %d failed notifications", retriedCount)
        );
    }
}
