package fr.airsen.api.controller;

import fr.airsen.api.dto.response.AdminStatisticsDTO;
import fr.airsen.api.service.AdminStatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Statistics Controller.
 *
 * Provides aggregated statistics for the admin dashboard.
 * All endpoints require ADMIN role.
 */
@RestController
@RequestMapping("/admin/statistics")
@Tag(name = "Admin Statistics", description = "Admin dashboard statistics and metrics")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatisticsController {

    @Autowired
    private AdminStatisticsService adminStatisticsService;

    /**
     * Get admin dashboard statistics.
     *
     * Returns comprehensive metrics for the admin dashboard including:
     * - User counts (total, active, suspended, new this week)
     * - Alert counts (total, active)
     * - Campaign counts (total, in progress)
     * - Forum thread count
     * - Total notifications sent
     *
     * Results are cached for 5 minutes for performance.
     *
     * @return AdminStatisticsDTO with all metrics
     */
    @GetMapping
    @Operation(summary = "Get admin statistics",
               description = "Get aggregated statistics for the admin dashboard (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required")
    })
    public ResponseEntity<AdminStatisticsDTO> getStatistics() {
        AdminStatisticsDTO statistics = adminStatisticsService.getStatistics();
        return ResponseEntity.ok(statistics);
    }
}
