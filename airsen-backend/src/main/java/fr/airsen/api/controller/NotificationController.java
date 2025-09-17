package fr.airsen.api.controller;

import fr.airsen.api.dto.response.NotificationDTO;
import fr.airsen.api.entity.Notification;
import fr.airsen.api.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

/**
 * REST Controller for managing user notifications.
 * 
 * This controller provides endpoints for notification management including
 * listing, reading status updates, and bulk operations according to the
 * Airsens API specification.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "User notification management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Constructor for NotificationController.
     * 
     * @param notificationService notification management service
     */
    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * GET /notifications - List user's notifications.
     * 
     * @param userDetails authenticated user details
     * @param pageable pagination parameters
     * @param unreadOnly filter for unread notifications only
     * @return paginated notifications with unread count
     */
    @GetMapping
    @Operation(summary = "List user notifications", 
              description = "Retrieve paginated list of notifications for authenticated user with unread count")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<NotificationListResponse> getUserNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdDate") Pageable pageable,
            @Parameter(description = "Filter for unread notifications only")
            @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly) {
        
        Long userId = getUserIdFromUserDetails(userDetails);
        
        Page<Notification> notifications = unreadOnly ? 
            notificationService.getUnreadNotificationsByRecipientId(userId, pageable) :
            notificationService.getNotificationsByRecipientId(userId, pageable);
        
        Page<NotificationDTO> notificationDTOs = notifications.map(this::convertToNotificationDTO);
        
        // Get unread count
        long unreadCount = notificationService.getUserNotificationStatistics(userId).getUnreadNotifications();
        
        NotificationListResponse response = new NotificationListResponse(notificationDTOs, unreadCount);
        
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /notifications/{notificationId}/read - Mark notification as read.
     * 
     * @param userDetails authenticated user details
     * @param notificationId notification identifier
     * @return success response
     */
    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark notification as read", 
              description = "Mark a specific notification as read for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notification marked as read successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - not notification recipient", content = @Content),
        @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content)
    })
    public ResponseEntity<Map<String, String>> markNotificationAsRead(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Notification identifier") @PathVariable Long notificationId) {
        
        Long userId = getUserIdFromUserDetails(userDetails);
        
        // Check if notification exists and belongs to user
        Optional<Notification> notificationOpt = notificationService.getNotificationById(notificationId);
        if (notificationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Notification notification = notificationOpt.get();
        if (!notification.getUserReceiver().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        // Mark as read (sent status = true in our model)
        notificationService.markNotificationAsSent(notificationId);
        
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    /**
     * PUT /notifications/read-all - Mark all notifications as read.
     * 
     * @param userDetails authenticated user details
     * @return success response
     */
    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", 
              description = "Mark all notifications as read for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All notifications marked as read successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<Map<String, String>> markAllNotificationsAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = getUserIdFromUserDetails(userDetails);
        
        notificationService.markAllNotificationsAsReadForUser(userId);
        
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    /**
     * DELETE /notifications/{notificationId} - Delete notification.
     * 
     * @param userDetails authenticated user details
     * @param notificationId notification identifier
     * @return success response
     */
    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete notification", 
              description = "Delete a specific notification for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Notification deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - not notification recipient", content = @Content),
        @ApiResponse(responseCode = "404", description = "Notification not found", content = @Content)
    })
    public ResponseEntity<Void> deleteNotification(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Notification identifier") @PathVariable Long notificationId) {
        
        Long userId = getUserIdFromUserDetails(userDetails);
        
        // Check if notification exists and belongs to user
        Optional<Notification> notificationOpt = notificationService.getNotificationById(notificationId);
        if (notificationOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Notification notification = notificationOpt.get();
        if (!notification.getUserReceiver().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        notificationService.deleteNotification(notificationId);
        
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /notifications/unread-count - Get unread notification count.
     * 
     * @param userDetails authenticated user details
     * @return unread count
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count", 
              description = "Get the count of unread notifications for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = getUserIdFromUserDetails(userDetails);
        
        NotificationService.NotificationStatistics stats = notificationService.getUserNotificationStatistics(userId);
        
        return ResponseEntity.ok(Map.of("unreadCount", stats.getUnreadNotifications()));
    }

    /**
     * GET /notifications/statistics - Get notification statistics.
     * 
     * @param userDetails authenticated user details
     * @return notification statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get notification statistics", 
              description = "Retrieve user's notification statistics including total, unread, and delivery status")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<NotificationStatsResponse> getNotificationStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = getUserIdFromUserDetails(userDetails);
        
        NotificationService.NotificationStatistics stats = notificationService.getUserNotificationStatistics(userId);
        
        NotificationStatsResponse response = new NotificationStatsResponse(
            stats.getTotalNotifications(),
            stats.getUnreadNotifications(),
            stats.getSuccessfulNotifications(),
            stats.getFailedNotifications(),
            stats.getSuccessRate(),
            stats.getFailureRate()
        );
        
        return ResponseEntity.ok(response);
    }

    // Helper methods

    /**
     * Extracts user ID from UserDetails.
     * In a real implementation, this would be based on your UserDetails implementation.
     */
    private Long getUserIdFromUserDetails(UserDetails userDetails) {
        // This is a placeholder - implement based on your UserDetails implementation
        // For example, if you have a custom UserPrincipal:
        // return ((UserPrincipal) userDetails).getUserId();
        
        // For now, we'll extract from username (assuming username is user ID)
        try {
            return Long.parseLong(userDetails.getUsername());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid user ID in authentication");
        }
    }

    /**
     * Converts Notification entity to NotificationDTO.
     */
    private NotificationDTO convertToNotificationDTO(Notification notification) {
        return new NotificationDTO(
            notification.getId(),
            notification.getUser().getId(),
            notification.getUser().getEmail(),
            notification.getUserReceiver().getId(),
            notification.getUserReceiver().getEmail(),
            notification.getNotificationType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getSendStatus(),
            notification.getSendChannel(),
            notification.getCreatedDate(),
            notification.getSentDate(),
            notification.getErrorMessage()
        );
    }

    // Response classes

    /**
     * Response wrapper for notification list with unread count.
     */
    @Schema(description = "Paginated notification list with metadata")
    public static class NotificationListResponse {
        @Schema(description = "Paginated notification content")
        private final Page<NotificationDTO> content;
        
        @Schema(description = "Total count of unread notifications")
        private final long unreadCount;

        public NotificationListResponse(Page<NotificationDTO> content, long unreadCount) {
            this.content = content;
            this.unreadCount = unreadCount;
        }

        public Page<NotificationDTO> getContent() {
            return content;
        }

        public long getUnreadCount() {
            return unreadCount;
        }
    }

    /**
     * Response for notification statistics.
     */
    @Schema(description = "User notification statistics")
    public static class NotificationStatsResponse {
        @Schema(description = "Total number of notifications")
        private final long totalNotifications;
        
        @Schema(description = "Number of unread notifications")
        private final long unreadNotifications;
        
        @Schema(description = "Number of successfully sent notifications")
        private final long successfulNotifications;
        
        @Schema(description = "Number of failed notifications")
        private final long failedNotifications;
        
        @Schema(description = "Success rate as percentage")
        private final double successRate;
        
        @Schema(description = "Failure rate as percentage")
        private final double failureRate;

        public NotificationStatsResponse(long totalNotifications, long unreadNotifications, 
                                       long successfulNotifications, long failedNotifications,
                                       double successRate, double failureRate) {
            this.totalNotifications = totalNotifications;
            this.unreadNotifications = unreadNotifications;
            this.successfulNotifications = successfulNotifications;
            this.failedNotifications = failedNotifications;
            this.successRate = successRate;
            this.failureRate = failureRate;
        }

        // Getters
        public long getTotalNotifications() { return totalNotifications; }
        public long getUnreadNotifications() { return unreadNotifications; }
        public long getSuccessfulNotifications() { return successfulNotifications; }
        public long getFailedNotifications() { return failedNotifications; }
        public double getSuccessRate() { return successRate; }
        public double getFailureRate() { return failureRate; }
    }
}