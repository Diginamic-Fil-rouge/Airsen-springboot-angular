package fr.airsen.api.dto.response;

import fr.airsen.api.entity.enums.BroadcastScope;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Result of admin alert broadcast operation")
public record BroadcastResultDTO(

    @Schema(description = "Broadcast operation identifier")
    String broadcastId,

    @Schema(description = "Geographic scope of the broadcast")
    BroadcastScope scope,

    @Schema(description = "Target geographic code")
    String targetCode,

    @Schema(description = "Total number of target users found")
    long totalUsers,

    @Schema(description = "Number of notifications successfully queued for sending")
    long notificationsQueued,

    @Schema(description = "Number of users with invalid email addresses")
    long invalidEmails,

    @Schema(description = "Broadcast initiation timestamp")
    LocalDateTime broadcastTime,

    @Schema(description = "Broadcast status")
    BroadcastStatus status,

    @Schema(description = "Optional error message if broadcast failed")
    String errorMessage

) {
    
    public enum BroadcastStatus {
        INITIATED,
        IN_PROGRESS,
        COMPLETED,
        FAILED
    }

    public double getSuccessRate() {
        return totalUsers > 0 ? (double) notificationsQueued / totalUsers : 0.0;
    }

    public boolean wasSuccessful() {
        return status == BroadcastStatus.COMPLETED || status == BroadcastStatus.IN_PROGRESS;
    }
}