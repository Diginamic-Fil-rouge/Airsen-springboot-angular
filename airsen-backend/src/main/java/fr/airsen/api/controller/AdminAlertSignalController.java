package fr.airsen.api.controller;

import fr.airsen.api.dto.request.CreateManualSignalRequest;
import fr.airsen.api.dto.response.AlertSignalDTO;
import fr.airsen.api.dto.response.AlertSignalStatisticsDTO;
import fr.airsen.api.entity.AlertSignal;
import fr.airsen.api.entity.enums.AlertSignalLevel;
import fr.airsen.api.entity.enums.AlertSignalSource;
import fr.airsen.api.entity.enums.GeographicScopeType;
import fr.airsen.api.mapper.AlertSignalMapper;
import fr.airsen.api.service.AlertSignalDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/admin/alert-signals")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Alert Signals", description = "Admin endpoints for managing detected environmental signals")
@SecurityRequirement(name = "bearerAuth")
public class AdminAlertSignalController {

    private static final Logger log = LoggerFactory.getLogger(AdminAlertSignalController.class);

    private final AlertSignalDetectionService alertSignalDetectionService;
    private final AlertSignalMapper alertSignalMapper;

    public AdminAlertSignalController(
            AlertSignalDetectionService alertSignalDetectionService,
            AlertSignalMapper alertSignalMapper) {
        this.alertSignalDetectionService = alertSignalDetectionService;
        this.alertSignalMapper = alertSignalMapper;
    }

    /**
     * GET /api/v1/admin/alert-signals - List all detected environmental signals with optional filters.
     */
    @GetMapping
    @Operation(
        summary = "List detected environmental signals",
        description = "Retrieve paginated list of alert signals with optional filtering by source, level, and scope type"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Signals retrieved successfully",
            content = @Content(schema = @Schema(implementation = Page.class))
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
        )
    })
    public ResponseEntity<Page<AlertSignalDTO>> getAllSignals(
            @Parameter(description = "Filter by signal source (ATMO, WEATHER)")
            @RequestParam(required = false) AlertSignalSource source,
            @Parameter(description = "Filter by alert level (INFO, WATCH, ALERT)")
            @RequestParam(required = false) AlertSignalLevel level,
            @Parameter(description = "Filter by geographic scope type")
            @RequestParam(required = false) GeographicScopeType scopeType,
            Pageable pageable) {

        log.info("Admin fetching alert signals - source: {}, level: {}, scopeType: {}",
                 source, level, scopeType);

        Page<AlertSignal> signals = alertSignalDetectionService.getAllSignals(pageable);
        Page<AlertSignalDTO> signalDTOs = signals.map(alertSignalMapper::toDTO);

        return ResponseEntity.ok(signalDTOs);
    }

    /**
     * GET /api/v1/admin/alert-signals/{id} - Get alert signal details by ID.
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get alert signal by ID",
        description = "Retrieve detailed information about a specific alert signal"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Signal found",
            content = @Content(schema = @Schema(implementation = AlertSignalDTO.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Signal not found",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin role required",
            content = @Content
        )
    })
    public ResponseEntity<AlertSignalDTO> getSignalById(
            @Parameter(description = "Alert signal ID", required = true)
            @PathVariable Long id) {

        log.info("Admin fetching alert signal with ID: {}", id);

        return alertSignalDetectionService.getSignalById(id)
                .map(alertSignalMapper::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/v1/admin/alert-signals/manual - Create manual alert signal.
     */
    @PostMapping("/manual")
    @Operation(
        summary = "Create manual alert signal",
        description = "Manually create an alert signal (not from automatic detection)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Signal created successfully",
            content = @Content(schema = @Schema(implementation = AlertSignalDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request data",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin role required",
            content = @Content
        )
    })
    public ResponseEntity<AlertSignalDTO> createManualSignal(
            @Parameter(description = "Manual signal creation request", required = true)
            @Valid @RequestBody CreateManualSignalRequest request) {

        log.info("Admin creating manual alert signal: kind={}, level={}",
                 request.kind(), request.level());

        AlertSignal createdSignal = alertSignalDetectionService.createManualSignal(request);
        AlertSignalDTO signalDTO = alertSignalMapper.toDTO(createdSignal);

        return ResponseEntity.status(HttpStatus.CREATED).body(signalDTO);
    }

    /**
     * POST /api/v1/admin/alert-signals/refresh - Manually trigger signal detection.
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Manually refresh alert signals",
        description = "Trigger immediate detection of environmental signals from ATMO and Weather APIs"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Detection completed successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin role required",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Detection failed",
            content = @Content
        )
    })
    public ResponseEntity<Map<String, Object>> refreshSignals() {
        log.info("Admin manually triggering signal detection");

        try {
            alertSignalDetectionService.detectAllSignals();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Signal detection completed successfully");
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error during manual signal detection", e);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Signal detection failed: " + e.getMessage());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * DELETE /api/v1/admin/alert-signals/{id} - Delete alert signal.
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete alert signal",
        description = "Remove an alert signal from the system (admin cleanup)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "204",
            description = "Signal deleted successfully",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Signal not found",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin role required",
            content = @Content
        )
    })
    public ResponseEntity<Void> deleteSignal(
            @Parameter(description = "Alert signal ID", required = true)
            @PathVariable Long id) {

        log.info("Admin deleting alert signal with ID: {}", id);

        alertSignalDetectionService.deleteSignal(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/admin/alert-signals/statistics - Get signal detection statistics.
     */
    @GetMapping("/statistics")
    @Operation(
        summary = "Get signal detection statistics",
        description = "Retrieve aggregated statistics about detected environmental signals"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Statistics retrieved successfully",
            content = @Content(schema = @Schema(implementation = AlertSignalStatisticsDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - admin role required",
            content = @Content
        )
    })
    public ResponseEntity<AlertSignalStatisticsDTO> getStatistics() {
        log.info("Admin fetching alert signal statistics");

        List<AlertSignal> allSignals = alertSignalDetectionService.getAllSignals(Pageable.unpaged()).getContent();

        // Calculate statistics
        int totalSignals = allSignals.size();

        Map<String, Integer> bySource = new HashMap<>();
        Map<String, Integer> byLevel = new HashMap<>();
        LocalDateTime lastDetection = null;

        for (AlertSignal signal : allSignals) {
            // Count by source
            String sourceName = signal.getSource().name();
            bySource.put(sourceName, bySource.getOrDefault(sourceName, 0) + 1);

            // Count by level
            String levelName = signal.getLevel().name();
            byLevel.put(levelName, byLevel.getOrDefault(levelName, 0) + 1);

            // Find last detection
            if (lastDetection == null || signal.getDetectedAt().isAfter(lastDetection)) {
                lastDetection = signal.getDetectedAt();
            }
        }

        AlertSignalStatisticsDTO statistics = new AlertSignalStatisticsDTO(
            totalSignals,
            bySource,
            byLevel,
            lastDetection
        );

        return ResponseEntity.ok(statistics);
    }
}
