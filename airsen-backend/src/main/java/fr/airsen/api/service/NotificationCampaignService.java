package fr.airsen.api.service;

import fr.airsen.api.dto.request.CreateCampaignRequest;
import fr.airsen.api.entity.*;
import fr.airsen.api.entity.enums.*;
import fr.airsen.api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for managing notification broadcast campaigns.
 *
 * Handles campaign creation, recipient fan-out, notification delivery,
 * and campaign monitoring/reporting.
 */
@Service
@Transactional
public class NotificationCampaignService {

    private static final Logger log = LoggerFactory.getLogger(NotificationCampaignService.class);
    private static final int BATCH_SIZE = 100;

    private final NotificationCampaignRepository campaignRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AlertSignalRepository alertSignalRepository;
    private final NotificationService notificationService;
    private final CommuneRepository communeRepository;

    public NotificationCampaignService(
            NotificationCampaignRepository campaignRepository,
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            AlertSignalRepository alertSignalRepository,
            NotificationService notificationService,
            CommuneRepository communeRepository) {
        this.campaignRepository = campaignRepository;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.alertSignalRepository = alertSignalRepository;
        this.notificationService = notificationService;
        this.communeRepository = communeRepository;
    }

    /**
     * Creates a draft campaign pre-filled from an alert signal.
     *
     * This allows admins to quickly create notification campaigns based on
     * detected environmental signals (ATMO pollution, weather alerts).
     *
     * @param signalId alert signal identifier
     * @param admin admin user creating the campaign
     * @return created draft campaign
     * @throws EntityNotFoundException if signal not found
     */
    public NotificationCampaign createDraftFromSignal(Long signalId, User admin) {
        log.info("Creating draft campaign from alert signal {} by admin {}", signalId, admin.getId());

        AlertSignal signal = alertSignalRepository.findById(signalId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Alert signal not found with id: " + signalId));

        NotificationCampaign campaign = new NotificationCampaign();
        campaign.setTitle(generateTitleFromSignal(signal));
        campaign.setMessage(generateMessageFromSignal(signal));
        campaign.setScopeType(signal.getScopeType());
        campaign.setScopeId(signal.getScopeId());
        campaign.setCreatedBy(admin);
        campaign.setStatus(NotificationCampaignStatus.DRAFT);
        campaign.setAlertSignal(signal);

        NotificationCampaign saved = campaignRepository.save(campaign);
        log.info("Created draft campaign {} from signal {}", saved.getId(), signalId);

        return saved;
    }

    /**
     * Generates campaign title based on alert signal type and area.
     */
    private String generateTitleFromSignal(AlertSignal signal) {
        String area = signal.getScopeDescription();

        return switch (signal.getKind()) {
            case HEAT -> "Heat alert in " + area;
            case WIND -> "Strong wind expected in " + area;
            case RAIN -> "Heavy rain warning for " + area;
            case PM25, PM10, AQI -> "Air quality alert in " + area;
            default -> "Environmental alert in " + area;
        };
    }

    /**
     * Generates campaign message with details from alert signal.
     */
    private String generateMessageFromSignal(AlertSignal signal) {
        StringBuilder message = new StringBuilder();
        message.append(signal.getSummary()).append("\n\n");

        if (signal.getDetails() != null && !signal.getDetails().isBlank()) {
            message.append(signal.getDetails()).append("\n\n");
        }

        if (signal.getValidFrom() != null && signal.getValidTo() != null) {
            message.append("Valid from: ")
                   .append(signal.getValidFrom().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                   .append(" to ")
                   .append(signal.getValidTo().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                   .append("\n");
        }

        message.append("\nLevel: ").append(signal.getLevel().getDisplayName());
        message.append("\nSource: ").append(signal.getSource().getDisplayName());

        return message.toString();
    }

    /**
     * Creates a manual notification campaign.
     *
     * Admins can create custom campaigns without linking to an alert signal.
     *
     * @param request campaign creation request
     * @param admin admin user creating the campaign
     * @return created draft campaign
     * @throws IllegalArgumentException if validation fails
     */
    public NotificationCampaign createManualCampaign(@Valid CreateCampaignRequest request, User admin) {
        log.info("Creating manual campaign by admin {}", admin.getId());

        // Validate request
        request.isValid();

        NotificationCampaign campaign = new NotificationCampaign();
        campaign.setTitle(request.title());
        campaign.setMessage(request.message());
        campaign.setScopeType(request.scopeType());
        campaign.setScopeId(request.scopeId());
        campaign.setCreatedBy(admin);
        campaign.setStatus(NotificationCampaignStatus.DRAFT);

        NotificationCampaign saved = campaignRepository.save(campaign);
        log.info("Created manual campaign {}", saved.getId());

        return saved;
    }

    /**
     * Fans out notifications to all recipients matching campaign scope.
     *
     * This creates individual Notification entities for each user in the target
     * geographic scope, ready for batch sending.
     *
     * Recipients include:
     * - Users whose commune/department/region matches scope
     * - Users who have the target area in their favorites
     *
     * @param campaignId campaign identifier
     * @return number of recipients added
     * @throws EntityNotFoundException if campaign not found
     * @throws IllegalStateException if campaign is not in DRAFT status
     */
    public int fanOutRecipients(Long campaignId) {
        log.info("Fanning out recipients for campaign {}", campaignId);

        NotificationCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Campaign not found with id: " + campaignId));

        if (!campaign.isDraft()) {
            throw new IllegalStateException(
                    "Campaign must be in DRAFT status. Current status: " + campaign.getStatus());
        }

        List<User> recipients = filterUsersByScope(campaign.getScopeType(), campaign.getScopeId());
        log.info("Found {} recipients for campaign {}", recipients.size(), campaignId);

        List<Notification> notifications = new ArrayList<>();
        User admin = campaign.getCreatedBy();

        for (User recipient : recipients) {
            Notification notification = new Notification();
            notification.setCampaign(campaign);
            notification.setUser(admin);
            notification.setUserReceiver(recipient);
            notification.setTitle(campaign.getTitle());
            notification.setMessage(campaign.getMessage());
            notification.setSendChannel(NotificationChannel.EMAIL);
            notification.setDeliveryStatus(NotificationDeliveryStatus.PENDING);
            notification.setNotificationType(NotificationType.EMAIL);

            notifications.add(notification);
        }

        // Batch save notifications
        notificationRepository.saveAll(notifications);

        // Update campaign recipient count
        campaign.setTotalRecipients(recipients.size());
        campaignRepository.save(campaign);

        log.info("Fanned out {} notifications for campaign {}", notifications.size(), campaignId);
        return notifications.size();
    }

    /**
     * Filters users by geographic scope to determine campaign recipients.
     *
     * Uses database queries to efficiently find users with favorites in the target scope.
     * This avoids loading all users into memory and filtering with Java streams.
     *
     * Geographic Targeting Logic:
     * - FRANCE: All active users with verified emails
     * - REGION: Users with favorites in the target region
     * - DEPARTMENT: Users with favorites in the target department
     * - COMMUNE: Users with favorites in the target commune
     *
     * @param scopeType geographic scope type
     * @param scopeId scope identifier (nullable for FRANCE)
     * @return list of users matching the scope
     */
    private List<User> filterUsersByScope(GeographicScopeType scopeType, Long scopeId) {
        log.debug("Filtering users by scope: type={}, scopeId={}", scopeType, scopeId);

        Set<User> targetedUsers = new HashSet<>();

        switch (scopeType) {
            case FRANCE:
                // All active users with verified emails
                List<User> allUsers = userRepository.findByEmailVerifiedTrue(Pageable.unpaged()).getContent();
                targetedUsers.addAll(allUsers);
                log.info("FRANCE scope: {} active users targeted", targetedUsers.size());
                break;

            case REGION:
                // Users with favorites in this region (direct database query)
                if (scopeId == null) {
                    log.warn("REGION scope specified but scopeId is null");
                    return Collections.emptyList();
                }
                List<User> usersInRegion = userRepository.findUsersWithFavoritesInRegion(scopeId);
                targetedUsers.addAll(usersInRegion);
                log.info("REGION scope (ID={}): {} users with favorites targeted", scopeId, targetedUsers.size());
                break;

            case DEPARTMENT:
                // Users with favorites in this department (direct database query)
                if (scopeId == null) {
                    log.warn("DEPARTMENT scope specified but scopeId is null");
                    return Collections.emptyList();
                }
                List<User> usersInDepartment = userRepository.findUsersWithFavoritesInDepartment(scopeId);
                targetedUsers.addAll(usersInDepartment);
                log.info("DEPARTMENT scope (ID={}): {} users with favorites targeted", scopeId, targetedUsers.size());
                break;

            case COMMUNE:
                // Users with favorites in this commune (direct database query)
                if (scopeId == null) {
                    log.warn("COMMUNE scope specified but scopeId is null");
                    return Collections.emptyList();
                }

                // Lookup commune to get INSEE code for efficient query
                Commune commune = communeRepository.findById(scopeId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Commune not found with id: " + scopeId));

                List<User> usersInCommune = userRepository.findUsersWithFavoritesInCommune(commune.getInseeCode());
                targetedUsers.addAll(usersInCommune);
                log.info("COMMUNE scope (ID={}, INSEE={}): {} users with favorites targeted",
                        scopeId, commune.getInseeCode(), targetedUsers.size());
                break;

            default:
                log.error("Unknown geographic scope type: {}", scopeType);
                throw new IllegalStateException("Unknown scope type: " + scopeType);
        }

        // Convert Set back to List and return
        List<User> result = new ArrayList<>(targetedUsers);
        log.debug("Total users targeted after deduplication: {}", result.size());
        return result;
    }

    /**
     * Sends all pending notifications for a campaign.
     *
     * Updates campaign status to SENDING, processes all notifications,
     * and marks campaign as COMPLETED when done.
     *
     * @param campaignId campaign identifier
     * @throws EntityNotFoundException if campaign not found
     * @throws IllegalStateException if campaign is not in DRAFT status
     */
    public void sendCampaign(Long campaignId) {
        log.info("Starting to send campaign {}", campaignId);

        NotificationCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Campaign not found with id: " + campaignId));

        if (!campaign.isDraft()) {
            throw new IllegalStateException(
                    "Campaign must be in DRAFT status. Current status: " + campaign.getStatus());
        }

        // Update status to SENDING
        campaign.setStatus(NotificationCampaignStatus.SENDING);
        campaignRepository.save(campaign);

        // Get all PENDING notifications for this campaign
        List<Notification> pendingNotifications = notificationRepository
                .findByCampaignAndDeliveryStatus(campaign, NotificationDeliveryStatus.PENDING);

        log.info("Found {} pending notifications for campaign {}",
                 pendingNotifications.size(), campaignId);

        // Process notifications in batches
        int totalSent = 0;
        int totalFailed = 0;

        for (int i = 0; i < pendingNotifications.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, pendingNotifications.size());
            List<Notification> batch = pendingNotifications.subList(i, end);

            for (Notification notification : batch) {
                try {
                    // Send notification synchronously (email service handles the actual sending)
                    notificationService.sendEmailNotificationAsync(notification.getId()).get();
                    totalSent++;
                } catch (Exception e) {
                    log.error("Failed to send notification {} for campaign {}: {}",
                             notification.getId(), campaignId, e.getMessage());
                    totalFailed++;
                }
            }

            // Reload notifications to get updated status
            for (Notification notification : batch) {
                notificationRepository.findById(notification.getId()).ifPresent(n -> {
                    if (n.wasSent()) {
                        // Already counted in totalSent
                    } else if (n.hasFailed()) {
                        // Already counted in totalFailed
                    }
                });
            }            // Update campaign counters after each batch
            campaign.setSentCount(totalSent);
            campaign.setFailedCount(totalFailed);
            campaignRepository.save(campaign);

            log.info("Campaign {} progress: {}/{} sent, {} failed",
                     campaignId, totalSent, pendingNotifications.size(), totalFailed);
        }

        // Mark campaign as completed
        campaign.setStatus(NotificationCampaignStatus.COMPLETED);
        campaignRepository.save(campaign);

        log.info("Campaign {} completed: {} sent, {} failed",
                 campaignId, totalSent, totalFailed);
    }

    /**
     * Retries all failed notifications for a campaign.
     *
     * @param campaignId campaign identifier
     * @return count of successfully retried notifications
     * @throws EntityNotFoundException if campaign not found
     * @throws IllegalStateException if campaign is not COMPLETED or FAILED
     */
    public int retryFailedNotifications(Long campaignId) {
        log.info("Retrying failed notifications for campaign {}", campaignId);

        NotificationCampaign campaign = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Campaign not found with id: " + campaignId));

        if (campaign.getStatus() != NotificationCampaignStatus.COMPLETED &&
            campaign.getStatus() != NotificationCampaignStatus.FAILED) {
            throw new IllegalStateException(
                    "Campaign must be COMPLETED or FAILED to retry. Current status: " + campaign.getStatus());
        }

        List<Notification> failedNotifications = notificationRepository
                .findByCampaignAndDeliveryStatus(campaign, NotificationDeliveryStatus.FAILED);

        log.info("Found {} failed notifications to retry for campaign {}",
                 failedNotifications.size(), campaignId);

        int successCount = 0;
        int stillFailedCount = 0;

        for (Notification notification : failedNotifications) {
            try {
                notificationService.sendEmailNotificationAsync(notification.getId()).get();
                successCount++;
                campaign.incrementSent();
                campaign.setFailedCount(campaign.getFailedCount() - 1);
            } catch (Exception e) {
                log.warn("Retry failed for notification {}: {}",
                        notification.getId(), e.getMessage());
                stillFailedCount++;
            }
        }        campaignRepository.save(campaign);

        log.info("Campaign {} retry completed: {} successful, {} still failed",
                 campaignId, successCount, stillFailedCount);

        return successCount;
    }

    /**
     * Retrieves a campaign by ID.
     *
     * @param id campaign identifier
     * @return campaign if found
     */
    @Transactional(readOnly = true)
    public Optional<NotificationCampaign> getCampaignById(Long id) {
        return campaignRepository.findById(id);
    }

    /**
     * Retrieves campaigns created by a specific admin.
     *
     * @param admin admin user
     * @param pageable pagination parameters
     * @return page of campaigns
     */
    @Transactional(readOnly = true)
    public Page<NotificationCampaign> getCampaignsByAdmin(User admin, Pageable pageable) {
        return campaignRepository.findByCreatedById(admin.getId(), pageable);
    }

    /**
     * Retrieves all campaigns with pagination.
     *
     * @param pageable pagination parameters
     * @return page of campaigns
     */
    @Transactional(readOnly = true)
    public Page<NotificationCampaign> getAllCampaigns(Pageable pageable) {
        return campaignRepository.findAll(pageable);
    }
 /**
     * Retrieves campaigns by status with pagination.
     *
     * @param status campaign status filter
     * @param pageable pagination parameters
     * @return page of campaigns with specified status
     */
    @Transactional(readOnly = true)
    public Page<NotificationCampaign> getCampaignsByStatus(NotificationCampaignStatus status, Pageable pageable) {
        return campaignRepository.findByStatus(status, pageable);
    }
}

