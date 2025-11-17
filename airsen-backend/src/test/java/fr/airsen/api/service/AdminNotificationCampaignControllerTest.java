package fr.airsen.api.controller;

import fr.airsen.api.dto.request.CreateCampaignRequest;
import fr.airsen.api.dto.response.CampaignFanOutResponseDTO;
import fr.airsen.api.dto.response.CampaignRetryResponseDTO;
import fr.airsen.api.dto.response.NotificationCampaignDTO;
import fr.airsen.api.entity.NotificationCampaign;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.GeographicScopeType;
import fr.airsen.api.entity.enums.NotificationCampaignStatus;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.mapper.NotificationCampaignMapper;
import fr.airsen.api.service.CurrentUserService;
import fr.airsen.api.service.NotificationCampaignService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminNotificationCampaignController.
 *
 * CRITICAL TESTS - Campaign Lifecycle & GDPR:
 * - Campaign approval workflow (draft → fanout → send → completed)
 * - Recipient targeting and privacy compliance
 * - Admin authorization for all operations
 * - Campaign state transitions
 *
 * NOTE: @PreAuthorize("hasRole('ADMIN')") is tested via Spring Security integration tests.
 * These unit tests focus on controller logic assuming authorization passed.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AdminNotificationCampaignController Unit Tests")
class AdminNotificationCampaignControllerTest {

    @Mock
    private NotificationCampaignService campaignService;

    @Mock
    private NotificationCampaignMapper campaignMapper;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AdminNotificationCampaignController controller;

    private User adminUser;
    private NotificationCampaign testCampaign;
    private NotificationCampaignDTO testCampaignDTO;

    @BeforeEach
    void setUp() {
        setupTestEntities();
    }

    private void setupTestEntities() {
        // Admin user
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("admin@airsen.fr");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(UserRole.ADMIN);

        // Test campaign
        testCampaign = new NotificationCampaign(
            "Test Campaign",
            "This is a test notification",
            GeographicScopeType.FRANCE,
            null,
            adminUser
        );
        testCampaign.setId(1L);
        testCampaign.setStatus(NotificationCampaignStatus.DRAFT);
        testCampaign.setCreatedAt(LocalDateTime.now());

        // Test campaign DTO
        testCampaignDTO = new NotificationCampaignDTO(
            1L,
            "Test Campaign",
            "This is a test notification",
            GeographicScopeType.FRANCE,
            null,
            null,
            LocalDateTime.now(),
            NotificationCampaignStatus.DRAFT,
            0,
            0,
            0,
            0.0
        );
    }

    // ============================================
    // Test 1: POST /admin/notification-campaigns/from-signal/{signalId}
    // ============================================

    @Test
    @DisplayName("Should create draft campaign from alert signal")
    void shouldCreateDraftCampaignFromAlertSignal() {
        // Given
        Long signalId = 100L;
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(campaignService.createDraftFromSignal(signalId, adminUser))
            .thenReturn(testCampaign);
        when(campaignMapper.toDTO(testCampaign))
            .thenReturn(testCampaignDTO);

        // When
        ResponseEntity<NotificationCampaignDTO> response =
            controller.createDraftFromSignal(signalId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
        assertThat(response.getBody().title()).isEqualTo("Test Campaign");
        assertThat(response.getBody().status()).isEqualTo(NotificationCampaignStatus.DRAFT);

        verify(currentUserService).getCurrentUser();
        verify(campaignService).createDraftFromSignal(signalId, adminUser);
        verify(campaignMapper).toDTO(testCampaign);
    }

    @Test
    @DisplayName("Should throw exception when signal not found during campaign creation")
    void shouldThrowExceptionWhenSignalNotFoundDuringCampaignCreation() {
        // Given
        Long invalidSignalId = 999L;
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(campaignService.createDraftFromSignal(invalidSignalId, adminUser))
            .thenThrow(new EntityNotFoundException("Alert signal not found with id: 999"));

        // When & Then
        assertThatThrownBy(() -> controller.createDraftFromSignal(invalidSignalId))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Alert signal not found");

        verify(currentUserService).getCurrentUser();
        verify(campaignService).createDraftFromSignal(invalidSignalId, adminUser);
    }

    // ============================================
    // Test 2: POST /admin/notification-campaigns (Manual Campaign)
    // ============================================

    @Test
    @DisplayName("Should create manual notification campaign")
    void shouldCreateManualNotificationCampaign() {
        // Given
        CreateCampaignRequest request = new CreateCampaignRequest(
            "Manual Campaign",
            "Manually created notification",
            GeographicScopeType.REGION,
            11L
        );

        NotificationCampaign manualCampaign = new NotificationCampaign(
            "Manual Campaign",
            "Manually created notification",
            GeographicScopeType.REGION,
            11L,
            adminUser
        );
        manualCampaign.setId(2L);

        NotificationCampaignDTO manualDTO = new NotificationCampaignDTO(
            2L,
            "Manual Campaign",
            "Manually created notification",
            GeographicScopeType.REGION,
            11L,
            null,
            LocalDateTime.now(),
            NotificationCampaignStatus.DRAFT,
            0, 0, 0, 0.0
        );

        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(campaignService.createManualCampaign(request, adminUser))
            .thenReturn(manualCampaign);
        when(campaignMapper.toDTO(manualCampaign))
            .thenReturn(manualDTO);

        // When
        ResponseEntity<NotificationCampaignDTO> response =
            controller.createManualCampaign(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(2L);
        assertThat(response.getBody().title()).isEqualTo("Manual Campaign");
        assertThat(response.getBody().scopeType()).isEqualTo(GeographicScopeType.REGION);
        assertThat(response.getBody().scopeId()).isEqualTo(11L);

        verify(currentUserService).getCurrentUser();
        verify(campaignService).createManualCampaign(request, adminUser);
        verify(campaignMapper).toDTO(manualCampaign);
    }

    // ============================================
    // Test 3: POST /admin/notification-campaigns/{id}/fanout
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should fan-out recipients for campaign")
    void shouldFanOutRecipientsForCampaign() {
        // Given
        Long campaignId = 1L;
        int recipientCount = 150;

        when(campaignService.fanOutRecipients(campaignId))
            .thenReturn(recipientCount);

        // When
        ResponseEntity<CampaignFanOutResponseDTO> response =
            controller.fanOutRecipients(campaignId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().campaignId()).isEqualTo(campaignId);
        assertThat(response.getBody().totalRecipients()).isEqualTo(150);

        verify(campaignService).fanOutRecipients(campaignId);
    }

    @Test
    @DisplayName("CRITICAL: Should return zero recipients when no users match scope")
    void shouldReturnZeroRecipientsWhenNoUsersMatchScope() {
        // Given: Campaign with scope that has no users
        Long campaignId = 1L;
        when(campaignService.fanOutRecipients(campaignId))
            .thenReturn(0);

        // When
        ResponseEntity<CampaignFanOutResponseDTO> response =
            controller.fanOutRecipients(campaignId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalRecipients()).isZero();

        verify(campaignService).fanOutRecipients(campaignId);
    }

    @Test
    @DisplayName("Should throw exception when fan-out called on non-DRAFT campaign")
    void shouldThrowExceptionWhenFanOutCalledOnNonDraftCampaign() {
        // Given
        Long campaignId = 1L;
        when(campaignService.fanOutRecipients(campaignId))
            .thenThrow(new IllegalStateException("Campaign must be in DRAFT status"));

        // When & Then
        assertThatThrownBy(() -> controller.fanOutRecipients(campaignId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DRAFT status");

        verify(campaignService).fanOutRecipients(campaignId);
    }

    // ============================================
    // Test 4: POST /admin/notification-campaigns/{id}/send
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should send campaign notifications (async)")
    void shouldSendCampaignNotificationsAsync() {
        // Given
        Long campaignId = 1L;
        doNothing().when(campaignService).sendCampaign(campaignId);

        // When
        ResponseEntity<Void> response = controller.sendCampaign(campaignId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED); // 202 Accepted
        assertThat(response.getBody()).isNull();

        verify(campaignService).sendCampaign(campaignId);
    }

    @Test
    @DisplayName("Should throw exception when send called on non-DRAFT campaign")
    void shouldThrowExceptionWhenSendCalledOnNonDraftCampaign() {
        // Given
        Long campaignId = 1L;
        doThrow(new IllegalStateException("Campaign must be in DRAFT status"))
            .when(campaignService).sendCampaign(campaignId);

        // When & Then
        assertThatThrownBy(() -> controller.sendCampaign(campaignId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("DRAFT status");

        verify(campaignService).sendCampaign(campaignId);
    }

    // ============================================
    // Test 5: POST /admin/notification-campaigns/{id}/retry-failed
    // ============================================

    @Test
    @DisplayName("Should retry failed notifications successfully")
    void shouldRetryFailedNotificationsSuccessfully() {
        // Given
        Long campaignId = 1L;
        int retriedCount = 5; // 5 notifications successfully retried

        when(campaignService.retryFailedNotifications(campaignId))
            .thenReturn(retriedCount);

        // When
        ResponseEntity<CampaignRetryResponseDTO> response =
            controller.retryFailedNotifications(campaignId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().campaignId()).isEqualTo(campaignId);
        assertThat(response.getBody().retriedCount()).isEqualTo(5);

        verify(campaignService).retryFailedNotifications(campaignId);
    }

    @Test
    @DisplayName("Should return zero when no failed notifications to retry")
    void shouldReturnZeroWhenNoFailedNotificationsToRetry() {
        // Given
        Long campaignId = 1L;
        when(campaignService.retryFailedNotifications(campaignId))
            .thenReturn(0);

        // When
        ResponseEntity<CampaignRetryResponseDTO> response =
            controller.retryFailedNotifications(campaignId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().retriedCount()).isZero();
    }

    @Test
    @DisplayName("Should throw exception when retry called on DRAFT campaign")
    void shouldThrowExceptionWhenRetryCalledOnDraftCampaign() {
        // Given
        Long campaignId = 1L;
        when(campaignService.retryFailedNotifications(campaignId))
            .thenThrow(new IllegalStateException("Campaign must be COMPLETED or FAILED to retry"));

        // When & Then
        assertThatThrownBy(() -> controller.retryFailedNotifications(campaignId))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("COMPLETED or FAILED");

        verify(campaignService).retryFailedNotifications(campaignId);
    }

    // ============================================
    // Test 6: GET /admin/notification-campaigns/{id}
    // ============================================

    @Test
    @DisplayName("Should get campaign by ID with metrics")
    void shouldGetCampaignByIdWithMetrics() {
        // Given
        Long campaignId = 1L;
        when(campaignService.getCampaignById(campaignId))
            .thenReturn(Optional.of(testCampaign));
        when(campaignMapper.toDTO(testCampaign))
            .thenReturn(testCampaignDTO);

        // When
        ResponseEntity<NotificationCampaignDTO> response =
            controller.getCampaignById(campaignId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
        assertThat(response.getBody().title()).isEqualTo("Test Campaign");

        verify(campaignService).getCampaignById(campaignId);
        verify(campaignMapper).toDTO(testCampaign);
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when campaign not found by ID")
    void shouldThrowEntityNotFoundExceptionWhenCampaignNotFoundById() {
        // Given
        Long invalidId = 999L;
        when(campaignService.getCampaignById(invalidId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> controller.getCampaignById(invalidId))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Campaign not found with ID: 999");

        verify(campaignService).getCampaignById(invalidId);
        verify(campaignMapper, never()).toDTO(any());
    }

    // ============================================
    // Test 7: GET /admin/notification-campaigns (List All)
    // ============================================

    @Test
    @DisplayName("Should list all campaigns with pagination")
    void shouldListAllCampaignsWithPagination() {
        // Given
        NotificationCampaign campaign1 = createTestCampaign(1L, "Campaign 1", NotificationCampaignStatus.DRAFT);
        NotificationCampaign campaign2 = createTestCampaign(2L, "Campaign 2", NotificationCampaignStatus.COMPLETED);

        Page<NotificationCampaign> campaignPage = new PageImpl<>(
            Arrays.asList(campaign1, campaign2),
            PageRequest.of(0, 20),
            2
        );

        NotificationCampaignDTO dto1 = createTestDTO(1L, "Campaign 1", NotificationCampaignStatus.DRAFT);
        NotificationCampaignDTO dto2 = createTestDTO(2L, "Campaign 2", NotificationCampaignStatus.COMPLETED);

        when(campaignService.getAllCampaigns(any(Pageable.class)))
            .thenReturn(campaignPage);
        when(campaignMapper.toDTO(campaign1)).thenReturn(dto1);
        when(campaignMapper.toDTO(campaign2)).thenReturn(dto2);

        // When
        ResponseEntity<Page<NotificationCampaignDTO>> response =
            controller.getAllCampaigns(null, PageRequest.of(0, 20));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(2);
        assertThat(response.getBody().getTotalElements()).isEqualTo(2);

        verify(campaignService).getAllCampaigns(any(Pageable.class));
        verify(campaignMapper, times(2)).toDTO(any());
    }

    @Test
    @DisplayName("Should filter campaigns by status")
    void shouldFilterCampaignsByStatus() {
        // Given
        NotificationCampaign draftCampaign = createTestCampaign(1L, "Draft Campaign", NotificationCampaignStatus.DRAFT);

        Page<NotificationCampaign> draftPage = new PageImpl<>(
            Collections.singletonList(draftCampaign),
            PageRequest.of(0, 20),
            1
        );

        NotificationCampaignDTO draftDTO = createTestDTO(1L, "Draft Campaign", NotificationCampaignStatus.DRAFT);

        when(campaignService.getCampaignsByStatus(eq(NotificationCampaignStatus.DRAFT), any(Pageable.class)))
            .thenReturn(draftPage);
        when(campaignMapper.toDTO(draftCampaign)).thenReturn(draftDTO);

        // When
        ResponseEntity<Page<NotificationCampaignDTO>> response =
            controller.getAllCampaigns(NotificationCampaignStatus.DRAFT, PageRequest.of(0, 20));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);
        assertThat(response.getBody().getContent().get(0).status())
            .isEqualTo(NotificationCampaignStatus.DRAFT);

        verify(campaignService).getCampaignsByStatus(NotificationCampaignStatus.DRAFT, any(Pageable.class));
        verify(campaignService, never()).getAllCampaigns(any());
    }

    @Test
    @DisplayName("Should return empty page when no campaigns exist")
    void shouldReturnEmptyPageWhenNoCampaignsExist() {
        // Given
        Page<NotificationCampaign> emptyPage = new PageImpl<>(
            Collections.emptyList(),
            PageRequest.of(0, 20),
            0
        );

        when(campaignService.getAllCampaigns(any(Pageable.class)))
            .thenReturn(emptyPage);

        // When
        ResponseEntity<Page<NotificationCampaignDTO>> response =
            controller.getAllCampaigns(null, PageRequest.of(0, 20));

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).isEmpty();
        assertThat(response.getBody().getTotalElements()).isZero();
    }

    // ============================================
    // Test 8: Campaign Lifecycle Workflow
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should follow complete campaign lifecycle (create → fanout → send → retry)")
    void shouldFollowCompleteCampaignLifecycle() {
        // This is an integration-style test showing the full workflow

        // Step 1: Create campaign from signal
        Long signalId = 100L;
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(campaignService.createDraftFromSignal(signalId, adminUser))
            .thenReturn(testCampaign);
        when(campaignMapper.toDTO(testCampaign))
            .thenReturn(testCampaignDTO);

        ResponseEntity<NotificationCampaignDTO> createResponse =
            controller.createDraftFromSignal(signalId);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long campaignId = createResponse.getBody().id();

        // Step 2: Fan-out recipients
        when(campaignService.fanOutRecipients(campaignId))
            .thenReturn(100);

        ResponseEntity<CampaignFanOutResponseDTO> fanOutResponse =
            controller.fanOutRecipients(campaignId);
        assertThat(fanOutResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fanOutResponse.getBody().totalRecipients()).isEqualTo(100);

        // Step 3: Send campaign
        doNothing().when(campaignService).sendCampaign(campaignId);

        ResponseEntity<Void> sendResponse = controller.sendCampaign(campaignId);
        assertThat(sendResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        // Step 4: Retry failed notifications (if any)
        when(campaignService.retryFailedNotifications(campaignId))
            .thenReturn(2);

        ResponseEntity<CampaignRetryResponseDTO> retryResponse =
            controller.retryFailedNotifications(campaignId);
        assertThat(retryResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(retryResponse.getBody().retriedCount()).isEqualTo(2);

        // Verify all steps were called
        verify(campaignService).createDraftFromSignal(signalId, adminUser);
        verify(campaignService).fanOutRecipients(campaignId);
        verify(campaignService).sendCampaign(campaignId);
        verify(campaignService).retryFailedNotifications(campaignId);
    }

    // ============================================
    // Helper Methods
    // ============================================

    private NotificationCampaign createTestCampaign(Long id, String title, NotificationCampaignStatus status) {
        NotificationCampaign campaign = new NotificationCampaign(
            title,
            "Message for " + title,
            GeographicScopeType.FRANCE,
            null,
            adminUser
        );
        campaign.setId(id);
        campaign.setStatus(status);
        campaign.setCreatedAt(LocalDateTime.now());
        return campaign;
    }

    private NotificationCampaignDTO createTestDTO(Long id, String title, NotificationCampaignStatus status) {
        return new NotificationCampaignDTO(
            id,
            title,
            "Message for " + title,
            GeographicScopeType.FRANCE,
            null,
            null,
            LocalDateTime.now(),
            status,
            0, 0, 0, 0.0
        );
    }
}
