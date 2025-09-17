package fr.airsen.api.controller;

import fr.airsen.api.dto.request.CreateAlertRequest;
import fr.airsen.api.dto.request.UpdateAlertRequest;
import fr.airsen.api.dto.request.UpdateAlertThresholdRequest;
import fr.airsen.api.dto.response.AlertDTO;
import fr.airsen.api.dto.response.AlertHistoryDTO;
import fr.airsen.api.dto.response.AlertStatisticsDTO;
import fr.airsen.api.entity.Alert;
import fr.airsen.api.entity.AlertHistory;
import fr.airsen.api.service.AlertHistoryService;
import fr.airsen.api.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST Controller for managing air quality alerts.
 * 
 * This controller provides endpoints for CRUD operations on alerts,
 * alert history management, and alert statistics according to the
 * Airsens API specification.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@Tag(name = "Alerts", description = "Air quality alert management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AlertController {

    private final AlertService alertService;
    private final AlertHistoryService alertHistoryService;

    /**
     * Constructor for AlertController.
     * 
     * @param alertService alert management service
     * @param alertHistoryService alert history service
     */
    @Autowired
    public AlertController(AlertService alertService, AlertHistoryService alertHistoryService) {
        this.alertService = alertService;
        this.alertHistoryService = alertHistoryService;
    }

    /**
     * GET /alerts - List user's alerts.
     * 
     * @param userDetails authenticated user details
     * @param pageable pagination parameters
     * @param activeOnly filter for active alerts only
     * @return paginated list of user's alerts
     */
    @GetMapping
    @Operation(summary = "List user's alerts", description = "Retrieve paginated list of alerts for authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alerts retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content)
    })
    public ResponseEntity<Page<AlertDTO>> getUserAlerts(
            @AuthenticationPrincipal UserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdDate") Pageable pageable,
            @Parameter(description = "Filter for active alerts only")
            @RequestParam(value = "activeOnly", defaultValue = "false") boolean activeOnly) {
        
        Long userId = getUserIdFromUserDetails(userDetails);
        
        Page<Alert> alerts = activeOnly ? 
            alertService.getActiveAlertsByUserId(userId, pageable) :
            alertService.getAllAlertsByUserId(userId, pageable);
        
        Page<AlertDTO> alertDTOs = alerts.map(this::convertToAlertDTO);
        
        return ResponseEntity.ok(alertDTOs);
    }

    /**
     * POST /alerts - Create new alert.
     * 
     * @param userDetails authenticated user details
     * @param createAlertRequest alert creation request
     * @return created alert details
     */
    @PostMapping
    @Operation(summary = "Create new alert", description = "Create a new air quality alert for specified commune and pollutant")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Alert created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "409", description = "Alert already exists or user limit reached"),
        @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<AlertDTO> createAlert(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateAlertRequest createAlertRequest) {
        
        Long userId = getUserIdFromUserDetails(userDetails);
        
        try {
            Alert createdAlert = alertService.createAlert(
                userId,
                createAlertRequest.getCommuneId(),
                createAlertRequest.getPollutant(),
                createAlertRequest.getThresholdValue(),
                createAlertRequest.getNotificationType()
            );
            
            AlertDTO alertDTO = convertToAlertDTO(createdAlert);
            return ResponseEntity.status(HttpStatus.CREATED).body(alertDTO);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * PUT /alerts/{alertId} - Update existing alert.
     * 
     * @param userDetails authenticated user details
     * @param alertId alert identifier
     * @param updateAlertRequest alert update request
     * @return success response
     */
    @PutMapping("/{alertId}")
    @Operation(summary = "Update alert", description = "Update existing alert configuration")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Alert updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - not alert owner", content = @Content),
        @ApiResponse(responseCode = "404", description = "Alert not found", content = @Content)
    })
    public ResponseEntity<String> updateAlert(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Alert identifier") @PathVariable Long alertId,
            @Valid @RequestBody UpdateAlertRequest updateAlertRequest) {
        
        Long userId = getUserIdFromUserDetails(userDetails);
        
        // Check if alert exists and belongs to user
        Optional<Alert> alertOpt = alertService.getAlertById(alertId);
        if (alertOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Alert alert = alertOpt.get();
        if (!alert.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            // Handle activation/deactivation separately for clarity
            if (updateAlertRequest.isOnlyActivationUpdate()) {
                if (updateAlertRequest.getActive()) {
                    alertService.activateAlert(alertId);
                } else {
                    alertService.deactivateAlert(alertId);
                }
            } else {
                // Update other fields
                alertService.updateAlert(alertId, 
                    updateAlertRequest.getThresholdValue(), 
                    updateAlertRequest.getNotificationType());
                
                // Handle activation if specified
                if (updateAlertRequest.getActive() != null) {
                    if (updateAlertRequest.getActive()) {
                        alertService.activateAlert(alertId);
                    } else {
                        alertService.deactivateAlert(alertId);
                    }
                }
            }
            
            return ResponseEntity.ok("{\"message\": \"Alert updated successfully\"}");
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PATCH /alerts/{alertId}/threshold - Update alert threshold only.
     * 
     * @param userDetails authenticated user details
     * @param alertId alert identifier
     * @param thresholdRequest threshold update request
     * @return success response
     */
    @PatchMapping("/{alertId}/threshold")
    @Operation(summary = "Update alert threshold", description = "Update only the threshold value of an alert")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Threshold updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid threshold value"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - not alert owner", content = @Content),
        @ApiResponse(responseCode = "404", description = "Alert not found", content = @Content)
    })
    public ResponseEntity<String> updateAlertThreshold(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Alert identifier") @PathVariable Long alertId,
            @Valid @RequestBody UpdateAlertThresholdRequest thresholdRequest) {
        
        Long userId = getUserIdFromUserDetails(userDetails);
        
        // Check ownership
        Optional<Alert> alertOpt = alertService.getAlertById(alertId);
        if (alertOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Alert alert = alertOpt.get();
        if (!alert.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            alertService.updateAlertThreshold(alertId, thresholdRequest.getThresholdValue());
            return ResponseEntity.ok("{\"message\": \"Alert threshold updated successfully\"}");
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * PATCH /alerts/{alertId}/activate - Activate alert.
     * 
     * @param userDetails authenticated user details
     * @param alertId alert identifier
     * @return success response
     */
    @PatchMapping("/{alertId}/activate")
    @Operation(summary = "Activate alert", description = "Activate an existing alert")
    public ResponseEntity<String> activateAlert(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Alert identifier") @PathVariable Long alertId) {
        
        return toggleAlertActivation(userDetails, alertId, true);
    }

    /**
     * PATCH /alerts/{alertId}/deactivate - Deactivate alert.
     * 
     * @param userDetails authenticated user details
     * @param alertId alert identifier
     * @return success response
     */
    @PatchMapping("/{alertId}/deactivate")
    @Operation(summary = "Deactivate alert", description = "Deactivate an existing alert")
    public ResponseEntity<String> deactivateAlert(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Alert identifier") @PathVariable Long alertId) {
        
        return toggleAlertActivation(userDetails, alertId, false);
    }

    /**
     * DELETE /alerts/{alertId} - Delete alert.
     * 
     * @param userDetails authenticated user details
     * @param alertId alert identifier
     * @return success response
     */
    @DeleteMapping("/{alertId}")
    @Operation(summary = "Delete alert", description = "Delete an existing alert")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Alert deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content),
        @ApiResponse(responseCode = "403", description = "Forbidden - not alert owner", content = @Content),
        @ApiResponse(responseCode = "404", description = "Alert not found", content = @Content)
    })
    public ResponseEntity<Void> deleteAlert(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Alert identifier") @PathVariable Long alertId) {
        
        Long userId = getUserIdFromUserDetails(userDetails);
        
        // Check ownership
        Optional<Alert> alertOpt = alertService.getAlertById(alertId);
        if (alertOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Alert alert = alertOpt.get();
        if (!alert.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        alertService.deleteAlert(alertId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /alerts/history - Get alert delivery history.
     * 
     * @param userDetails authenticated user details
     * @param pageable pagination parameters
     * @param startDate optional start date filter
     * @param endDate optional end date filter
     * @return paginated alert history
     */
//    @GetMapping("/history")
//    @Operation(summary = "Get alert history", description = "Retrieve paginated history of triggered alerts")
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Alert history retrieved successfully"),
//        @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content)
//    })
//    public ResponseEntity<Page<AlertHistoryDTO>> getAlertHistory(
//            @AuthenticationPrincipal UserDetails userDetails,
//            @PageableDefault(size = 20, sort = "sendDate") Pageable pageable,
//            @Parameter(description = "Start date filter (ISO format)")
//            @RequestParam(value = "startDate", required = false) String startDate,
//            @Parameter(description = "End date filter (ISO format)")
//            @RequestParam(value = "endDate", required = false) String endDate) {
//
//        Long userId = getUserIdFromUserDetails(userDetails);
//
//        Page<AlertHistory> history;
//        if (startDate != null && endDate != null) {
//            LocalDateTime start = LocalDateTime.parse(startDate);
//            LocalDateTime end = LocalDateTime.parse(endDate);
//            history = alertHistoryService.getAlertHistoryByUserIdAndDateRange(userId, start, end, pageable);
//        } else {
//            history = alertHistoryService.getAlertHistoryByUserId(userId, pageable);
//        }
//
//        Page<AlertHistoryDTO> historyDTOs = history.map(this::convertToAlertHistoryDTO);
//
//        return ResponseEntity.ok(historyDTOs);
//    }

    /**
     * GET /alerts/statistics - Get user alert statistics.
     * 
     * @param userDetails authenticated user details
     * @return alert statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get alert statistics", description = "Retrieve user's alert usage statistics")
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully")
    public ResponseEntity<AlertStatisticsDTO> getAlertStatistics(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = getUserIdFromUserDetails(userDetails);
        AlertService.AlertStatistics stats = alertService.getUserAlertStatistics(userId);
        
        AlertStatisticsDTO statsDTO = new AlertStatisticsDTO(
            stats.getActiveAlerts(),
            stats.getTotalAlerts(),
            stats.getMaxAllowedAlerts()
        );
        
        return ResponseEntity.ok(statsDTO);
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
     * Helper method to toggle alert activation.
     */
    private ResponseEntity<String> toggleAlertActivation(UserDetails userDetails, Long alertId, boolean activate) {
        Long userId = getUserIdFromUserDetails(userDetails);
        
        // Check ownership
        Optional<Alert> alertOpt = alertService.getAlertById(alertId);
        if (alertOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        Alert alert = alertOpt.get();
        if (!alert.getUser().getId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        
        try {
            if (activate) {
                alertService.activateAlert(alertId);
            } else {
                alertService.deactivateAlert(alertId);
            }
            
            String action = activate ? "activated" : "deactivated";
            return ResponseEntity.ok("{\"message\": \"Alert " + action + " successfully\"}");
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Converts Alert entity to AlertDTO.
     */
    private AlertDTO convertToAlertDTO(Alert alert) {
        return new AlertDTO(
            alert.getId(),
            alert.getUser().getId(),
            alert.getUser().getEmail(),
            (long) alert.getCommune().getId(),
            alert.getCommune().getName(),
            alert.getPollutant(),
            alert.getThresholdValue(),
            alert.getNotificationType(),
            alert.getActive(),
            alert.getCreatedDate()
        );
    }

    /**
     * Converts AlertHistory entity to AlertHistoryDTO.
     */
    private AlertHistoryDTO convertToAlertHistoryDTO(AlertHistory alertHistory) {
        return new AlertHistoryDTO(
            alertHistory.getId(),
            alertHistory.getAlert().getId(),
            alertHistory.getAlert().getUser().getId(),
            alertHistory.getAlert().getUser().getEmail(),
            (long) alertHistory.getAirQuality().getId(),
            alertHistory.getSendDate(),
            alertHistory.getStatus(),
            alertHistory.getErrorMessage()
        );
    }
}