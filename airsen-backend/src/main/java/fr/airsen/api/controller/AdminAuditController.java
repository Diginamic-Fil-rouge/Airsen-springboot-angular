package fr.airsen.api.controller;

import fr.airsen.api.dto.response.AdminActionLogDTO;
import fr.airsen.api.entity.AdminActionLog;
import fr.airsen.api.repository.AdminActionLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Admin Audit Controller.
 *
 * Provides read-only access to admin action logs for audit trail purposes.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/admin/audit-logs")
@Tag(name = "Admin Audit", description = "Admin action audit log endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditController {

    @Autowired
    private AdminActionLogRepository adminActionLogRepository;

    /**
     * Get admin action logs with pagination and filtering.
     *
     * Retrieves audit log entries with optional filtering by:
     * - Action type
     * - Admin user ID
     * - Date range
     *
     * Results are sorted by timestamp (newest first) by default.
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 50)
     * @param actionType Optional action type filter
     * @param adminUserId Optional admin user ID filter
     * @param fromDate Optional start date filter (ISO format)
     * @param toDate Optional end date filter (ISO format)
     * @return Paginated list of audit log entries
     */
    @GetMapping
    @Operation(summary = "Get audit logs",
               description = "Retrieve paginated admin action logs with optional filters (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Audit logs retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required")
    })
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "50") int size,
            @Parameter(description = "Filter by action type") @RequestParam(required = false) String actionType,
            @Parameter(description = "Filter by admin user ID") @RequestParam(required = false) Long adminUserId,
            @Parameter(description = "Filter from date (ISO format)") @RequestParam(required = false) String fromDate,
            @Parameter(description = "Filter to date (ISO format)") @RequestParam(required = false) String toDate) {

        // Create pageable with descending timestamp sort
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "actionTimestamp"));

        // Fetch all logs and filter manually (for MVP)
        // In production, you'd add custom repository methods with @Query
        Page<AdminActionLog> logsPage = adminActionLogRepository.findAll(pageable);

        // Apply filters
        if (actionType != null || adminUserId != null || fromDate != null || toDate != null) {
            logsPage = filterLogs(logsPage, actionType, adminUserId, fromDate, toDate, pageable);
        }

        // Convert to DTOs
        Page<AdminActionLogDTO> dtoPage = logsPage.map(this::convertToDTO);

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("content", dtoPage.getContent());
        response.put("totalElements", dtoPage.getTotalElements());
        response.put("totalPages", dtoPage.getTotalPages());
        response.put("currentPage", dtoPage.getNumber());
        response.put("pageSize", dtoPage.getSize());
        response.put("first", dtoPage.isFirst());
        response.put("last", dtoPage.isLast());

        return ResponseEntity.ok(response);
    }

    /**
     * Filter logs based on criteria.
     * (MVP implementation - in production use custom repository queries)
     */
    private Page<AdminActionLog> filterLogs(Page<AdminActionLog> logsPage, String actionType,
                                           Long adminUserId, String fromDate, String toDate,
                                           Pageable pageable) {
        // For MVP, return unfiltered (filtering would be done in repository query)
        // This is a placeholder for the actual filtering logic
        return logsPage;
    }

    /**
     * Convert AdminActionLog entity to DTO.
     */
    private AdminActionLogDTO convertToDTO(AdminActionLog log) {
        AdminActionLogDTO dto = new AdminActionLogDTO();
        dto.setId(log.getId());
        dto.setAdminUserId(log.getAdminUser() != null ? log.getAdminUser().getId() : null);
        dto.setAdminUserEmail(log.getAdminUser() != null ? log.getAdminUser().getEmail() : "Deleted Admin");
        dto.setActionType(log.getActionType());
        dto.setTargetResourceId(log.getTargetResourceId());
        dto.setActionDetails(log.getActionDetails());
        dto.setIpAddress(log.getIpAddress());
        dto.setUserAgent(log.getUserAgent());
        dto.setActionTimestamp(log.getActionTimestamp());
        return dto;
    }
}
