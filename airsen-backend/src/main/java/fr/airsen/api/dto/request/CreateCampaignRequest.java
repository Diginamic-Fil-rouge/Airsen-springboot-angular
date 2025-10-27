package fr.airsen.api.dto.request;

import fr.airsen.api.entity.enums.GeographicScopeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a manual notification campaign.
 * 
 * Admins can create custom notification campaigns targeting specific geographic scopes
 * without necessarily linking to an alert signal.
 */
public record CreateCampaignRequest(
    
    @NotBlank(message = "Campaign title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    String title,
    
    @NotBlank(message = "Campaign message is required")
    @Size(max = 5000, message = "Message cannot exceed 5000 characters")
    String message,
    
    @NotNull(message = "Geographic scope type is required")
    GeographicScopeType scopeType,
    
    Long scopeId
) {
    /**
     * Validates that scopeId is provided when scopeType requires it.
     * 
     * @return true if valid
     * @throws IllegalArgumentException if scopeId is missing for non-FRANCE scope
     */
    public boolean isValid() {
        if (scopeType != GeographicScopeType.FRANCE && scopeId == null) {
            throw new IllegalArgumentException(
                "scopeId is required for scope type: " + scopeType
            );
        }
        return true;
    }
}
