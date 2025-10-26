package fr.airsen.api.dto.response;

import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.entity.enums.GeographicScopeType;
import fr.airsen.api.entity.enums.NotificationCampaignStatus;

import java.time.LocalDateTime;

public record NotificationCampaignDTO(
    Long id,
    String title,
    String message,
    GeographicScopeType scopeType,
    Long scopeId,
    UserDTO createdBy,
    LocalDateTime createdAt,
    NotificationCampaignStatus status,
    Integer totalRecipients,
    Integer sentCount,
    Integer failedCount,
    Double deliveryRate
) {
}
