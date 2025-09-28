package fr.airsen.api.dto.request;

import fr.airsen.api.entity.enums.BroadcastScope;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request for admin alert broadcast to users")
public record AdminAlertBroadcastRequest(

    @Schema(description = "Notification title", example = "Air Quality Alert - High Pollution Levels")
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    String title,

    @Schema(description = "Notification message content", example = "Pollution levels are high across France. Avoid outdoor activities.")
    @NotBlank(message = "Message is required")
    @Size(max = 5000, message = "Message cannot exceed 5000 characters")
    String message,

    @Schema(description = "Geographic scope for the broadcast")
    @NotNull(message = "Broadcast scope is required")
    BroadcastScope scope,

    @Schema(description = "Region code (required when scope is REGION)", example = "11")
    String regionCode,

    @Schema(description = "Department code (required when scope is DEPARTMENT)", example = "75")
    String departmentCode,

    @Schema(description = "Commune code (required when scope is COMMUNE)", example = "75056")
    String communeCode

) {
    
    public boolean isValidForScope() {
        return scope.isValidWithCodes(regionCode, departmentCode, communeCode);
    }

    public String getTargetCode() {
        return switch (scope) {
            case FRANCE -> null;
            case REGION -> regionCode;
            case DEPARTMENT -> departmentCode;
            case COMMUNE -> communeCode;
        };
    }
}