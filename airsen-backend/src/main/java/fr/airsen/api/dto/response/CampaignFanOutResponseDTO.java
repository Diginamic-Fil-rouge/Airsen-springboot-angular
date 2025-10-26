package fr.airsen.api.dto.response;

public record CampaignFanOutResponseDTO(
    Long campaignId,
    Integer totalRecipients,
    String message
) {
    public static CampaignFanOutResponseDTO of(Long campaignId, Integer totalRecipients) {
        return new CampaignFanOutResponseDTO(
            campaignId,
            totalRecipients,
            String.format("Successfully fan-out to %d recipients", totalRecipients)
        );
    }
}
