package fr.airsen.api.controller;

import fr.airsen.api.entity.Notification;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Controller for managing user notifications.
 * 
 * This controller provides endpoints for notification management including
 * listing, reading status updates, and bulk operations according to the
 * Airsens API specification.
 */
@RestController
@RequestMapping("/notifications")
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
     * @param unreadOnly filter for unread notifications only
     * @param page page number (0-based)
     * @param size page size
     * @return notifications with unread count
     */
    @GetMapping
    @Operation(summary = "List user notifications", 
              description = "Retrieve paginated list of notifications for authenticated user with unread count")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Notifications retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<Map<String, Object>> getUserNotifications(
            @Parameter(description = "Filter for unread notifications only")
            @RequestParam(value = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(value = "size", defaultValue = "20") int size) {
        
        Long userId = getCurrentUserId();
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdDate"));
        
        Page<Notification> notifications = unreadOnly ? 
            notificationService.getUnreadNotificationsByRecipientId(userId, pageable) :
            notificationService.getNotificationsByRecipientId(userId, pageable);
        
        List<Notification> content = notifications.getContent();
        
        // Get unread count
        long unreadCount = notificationService.getUserNotificationStatistics(userId).getUnreadNotifications();
        
        Map<String, Object> response = Map.of(
            "content", content,
            "unreadCount", unreadCount
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /notifications/{notificationId}/read - Mark notification as read.
     * 
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
            @Parameter(description = "Notification identifier") @PathVariable Long notificationId) {
        
        Long userId = getCurrentUserId();
        
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
     * @return success response
     */
    @PutMapping("/read-all")
    @Operation(summary = "Mark all notifications as read", 
              description = "Mark all notifications as read for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "All notifications marked as read successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
    })
    public ResponseEntity<Map<String, String>> markAllNotificationsAsRead() {
        
        Long userId = getCurrentUserId();
        
        notificationService.markAllNotificationsAsReadForUser(userId);
        
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }

    /**
     * DELETE /notifications/{notificationId} - Delete notification.
     * 
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
            @Parameter(description = "Notification identifier") @PathVariable Long notificationId) {
        
        Long userId = getCurrentUserId();
        
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
     * @return unread count
     */
    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count", 
              description = "Get the count of unread notifications for the authenticated user")
    @ApiResponse(responseCode = "200", description = "Unread count retrieved successfully")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationCount() {
        
        Long userId = getCurrentUserId();
        
        NotificationService.NotificationStatistics stats = notificationService.getUserNotificationStatistics(userId);
        
        return ResponseEntity.ok(Map.of("unreadCount", stats.getUnreadNotifications()));
    }

    /**
     * GET /notifications/statistics - Get notification statistics.
     * 
     * @return notification statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get notification statistics", 
              description = "Retrieve user's notification statistics including total, unread, and delivery status")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<NotificationStatsResponse> getNotificationStatistics() {
        
        Long userId = getCurrentUserId();
        
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
     * Gets the current authenticated user ID.
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new IllegalStateException("User not authenticated");
        }
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }


    // Response classes

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