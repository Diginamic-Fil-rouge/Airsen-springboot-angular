package fr.airsen.api.controller;

import fr.airsen.api.dto.request.CreateCampaignRequest;
import fr.airsen.api.dto.response.CampaignFanOutResponseDTO;
import fr.airsen.api.dto.response.CampaignRetryResponseDTO;
import fr.airsen.api.dto.response.NotificationCampaignDTO;
import fr.airsen.api.entity.NotificationCampaign;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.NotificationCampaignStatus;
import fr.airsen.api.mapper.NotificationCampaignMapper;
import fr.airsen.api.service.CurrentUserService;
import fr.airsen.api.service.NotificationCampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/admin/notification-campaigns")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Notification Campaigns", description = "Admin endpoints for managing notification broadcast campaigns")
public class AdminNotificationCampaignController {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationCampaignController.class);

    private final NotificationCampaignService campaignService;
    private final NotificationCampaignMapper campaignMapper;
    private final CurrentUserService currentUserService;

    public AdminNotificationCampaignController(
        NotificationCampaignService campaignService,
        NotificationCampaignMapper campaignMapper,
        CurrentUserService currentUserService
    ) {
        this.campaignService = campaignService;
        this.campaignMapper = campaignMapper;
        this.currentUserService = currentUserService;
    }


    @PostMapping("/from-signal/{signalId}")
    @Operation(summary = "Create draft campaign from alert signal")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Campaign created successfully",
            content = @Content(schema = @Schema(implementation = NotificationCampaignDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid signal ID"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not admin"),
        @ApiResponse(responseCode = "404", description = "Signal not found")
    })
    public ResponseEntity<NotificationCampaignDTO> createDraftFromSignal(
        @Parameter(description = "Alert signal ID", required = true)
        @PathVariable Long signalId
    ) {
        User admin = currentUserService.getCurrentUser();
        log.info("Admin {} creating draft campaign from signal {}", admin.getEmail(), signalId);

        NotificationCampaign campaign = campaignService.createDraftFromSignal(signalId, admin);
        NotificationCampaignDTO dto = campaignMapper.toDTO(campaign);

        log.info("Draft campaign {} created from signal {}", campaign.getId(), signalId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }


    @PostMapping
    @Operation(summary = "Create manual notification campaign")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Campaign created successfully",
            content = @Content(schema = @Schema(implementation = NotificationCampaignDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not admin")
    })
    public ResponseEntity<NotificationCampaignDTO> createManualCampaign(
        @Parameter(description = "Campaign creation request", required = true)
        @Valid @RequestBody CreateCampaignRequest request
    ) {
        User admin = currentUserService.getCurrentUser();
        log.info("Admin {} creating manual campaign: {}", admin.getEmail(), request.title());

        NotificationCampaign campaign = campaignService.createManualCampaign(request, admin);
        NotificationCampaignDTO dto = campaignMapper.toDTO(campaign);

        log.info("Manual campaign {} created", campaign.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Fan-out notifications to all users matching the campaign's geographic scope.
     * Creates individual notification records for each recipient.
     *
     * @param id campaign ID
     * @return fan-out summary with recipient count
     */
    @PostMapping("/{id}/fanout")
    @Operation(summary = "Fan-out notifications to users")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Notifications fan-out successful",
            content = @Content(schema = @Schema(implementation = CampaignFanOutResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Campaign not in DRAFT status"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not admin"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    public ResponseEntity<CampaignFanOutResponseDTO> fanOutRecipients(
        @Parameter(description = "Campaign ID", required = true)
        @PathVariable Long id
    ) {
        log.info("Fan-out recipients for campaign {}", id);

        int recipientCount = campaignService.fanOutRecipients(id);
        CampaignFanOutResponseDTO response = CampaignFanOutResponseDTO.of(id, recipientCount);

        log.info("Fan-out completed for campaign {}: {} recipients", id, recipientCount);
        return ResponseEntity.ok(response);
    }

    /**
     * Send all pending notifications for a campaign.
     * This is an async operation that processes notifications in batches.
     *
     * @param id campaign ID
     * @return accepted status (async processing)
     */
    @PostMapping("/{id}/send")
    @Operation(summary = "Send campaign notifications")
    @ApiResponses({
        @ApiResponse(responseCode = "202", description = "Campaign sending initiated (async)"),
        @ApiResponse(responseCode = "400", description = "Campaign not in DRAFT status"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not admin"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    public ResponseEntity<Void> sendCampaign(
        @Parameter(description = "Campaign ID", required = true)
        @PathVariable Long id
    ) {
        log.info("Sending campaign {}", id);

        campaignService.sendCampaign(id);

        log.info("Campaign {} sending initiated", id);
        return ResponseEntity.accepted().build();
    }

    /**
     * Retry failed notifications for a completed campaign.
     *
     * @param id campaign ID
     * @return retry summary with count of successfully retried notifications
     */
    @PostMapping("/{id}/retry-failed")
    @Operation(summary = "Retry failed notifications")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Failed notifications retried",
            content = @Content(schema = @Schema(implementation = CampaignRetryResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Campaign not in COMPLETED/FAILED status"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not admin"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    public ResponseEntity<CampaignRetryResponseDTO> retryFailedNotifications(
        @Parameter(description = "Campaign ID", required = true)
        @PathVariable Long id
    ) {
        log.info("Retrying failed notifications for campaign {}", id);

        int retriedCount = campaignService.retryFailedNotifications(id);
        CampaignRetryResponseDTO response = CampaignRetryResponseDTO.of(id, retriedCount);

        log.info("Retried {} failed notifications for campaign {}", retriedCount, id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get campaign details by ID with delivery metrics.
     *
     * @param id campaign ID
     * @return campaign DTO
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get campaign details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaign retrieved successfully",
            content = @Content(schema = @Schema(implementation = NotificationCampaignDTO.class))),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not admin"),
        @ApiResponse(responseCode = "404", description = "Campaign not found")
    })
    public ResponseEntity<NotificationCampaignDTO> getCampaignById(
        @Parameter(description = "Campaign ID", required = true)
        @PathVariable Long id
    ) {
        log.debug("Fetching campaign {}", id);

        NotificationCampaign campaign = campaignService.getCampaignById(id)
            .orElseThrow(() -> new EntityNotFoundException("Campaign not found with ID: " + id));

        NotificationCampaignDTO dto = campaignMapper.toDTO(campaign);
        return ResponseEntity.ok(dto);
    }

    /**
     * List all campaigns with optional status filter and pagination.
     *
     * @param status optional campaign status filter
     * @param pageable pagination parameters
     * @return page of campaign DTOs
     */
    @GetMapping
    @Operation(summary = "List notification campaigns")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Campaigns retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Not authenticated"),
        @ApiResponse(responseCode = "403", description = "Not admin")
    })
    public ResponseEntity<Page<NotificationCampaignDTO>> getAllCampaigns(
        @Parameter(description = "Filter by campaign status")
        @RequestParam(required = false) NotificationCampaignStatus status,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        log.debug("Listing campaigns with status filter: {}", status);

        Page<NotificationCampaign> campaigns = (status != null)
            ? campaignService.getCampaignsByStatus(status, pageable)
            : campaignService.getAllCampaigns(pageable);

        Page<NotificationCampaignDTO> dtos = campaigns.map(campaignMapper::toDTO);
        return ResponseEntity.ok(dtos);
    }

}
