package fr.airsen.api.controller;

import fr.airsen.api.dto.request.AdminAlertBroadcastRequest;
import fr.airsen.api.dto.response.BroadcastResultDTO;
import fr.airsen.api.security.UserPrincipal;
import fr.airsen.api.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/admin/notifications")
@Tag(name = "Admin Notifications", description = "Administrator notification management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminNotificationController {

    private final NotificationService notificationService;

    @Autowired
    public AdminNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * POST /admin/notifications/broadcast - Broadcast alert to users by geographic scope.
     * 
     * @param request broadcast request with scope and message details
     * @return broadcast result with statistics
     */
    @PostMapping("/broadcast")
    @Operation(
        summary = "Broadcast admin alert", 
        description = "Send alert notifications to users based on geographic scope (France, Region, Department, Commune). " +
                     "Users are targeted through their alert configurations and favorite communes."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "202", 
            description = "Broadcast initiated successfully",
            content = @Content(schema = @Schema(implementation = BroadcastResultDTO.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request data or scope parameters",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Unauthorized - authentication required",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403", 
            description = "Forbidden - admin role required",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500", 
            description = "Internal server error during broadcast",
            content = @Content
        )
    })
    public ResponseEntity<BroadcastResultDTO> broadcastAlert(
            @Parameter(description = "Broadcast request with title, message, and geographic scope")
            @Valid @RequestBody AdminAlertBroadcastRequest request) {
        
        try {
            Long adminUserId = getCurrentUserId();
            
            BroadcastResultDTO broadcastResult = 
                notificationService.broadcastAdminAlert(adminUserId, request);
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(broadcastResult);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /admin/notifications/statistics - Get system notification statistics.
     * 
     * @return system-wide notification statistics
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Get system notification statistics", 
        description = "Retrieve system-wide notification statistics including total sent, failed, and success rates"
    )
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<NotificationService.NotificationStatistics> getSystemStatistics() {
        
        NotificationService.NotificationStatistics stats = 
            notificationService.getSystemNotificationStatistics();
        
        return ResponseEntity.ok(stats);
    }

    /**
     * POST /admin/notifications/retry-failed - Retry failed notifications.
     * 
     * @return number of notifications retried
     */
    @PostMapping("/retry-failed")
    @Operation(
        summary = "Retry failed notifications", 
        description = "Retry sending notifications that previously failed"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Failed notifications retry initiated"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required", content = @Content)
    })
    public ResponseEntity<RetryResultResponse> retryFailedNotifications() {
        
        int retriedCount = notificationService.retryFailedNotifications();
        
        RetryResultResponse response = new RetryResultResponse(
            retriedCount,
            "Failed notifications retry initiated successfully"
        );
        
        return ResponseEntity.ok(response);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new IllegalStateException("Admin user not authenticated");
        }
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }

    @Schema(description = "Retry operation result")
    public record RetryResultResponse(
        @Schema(description = "Number of notifications retried")
        int retriedCount,
        
        @Schema(description = "Operation status message")
        String message
    ) {}
}