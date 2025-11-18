package fr.airsen.api.service;

import fr.airsen.api.dto.request.CreateCampaignRequest;
import fr.airsen.api.entity.*;
import fr.airsen.api.entity.enums.*;
import fr.airsen.api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationCampaignService.
 *
 * Tests critical business logic:
 * - Recipient filtering by geographic scope (GDPR compliance)
 * - Campaign lifecycle management (draft → sending → completed)
 * - Fan-out logic with batch processing
 * - Error handling and validation
 * - Alert signal integration
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationCampaignService Unit Tests")
class NotificationCampaignServiceTest {

    @Mock
    private NotificationCampaignRepository campaignRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AlertSignalRepository alertSignalRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CommuneRepository communeRepository;

    @InjectMocks
    private NotificationCampaignService campaignService;

    @Captor
    private ArgumentCaptor<NotificationCampaign> campaignCaptor;

    @Captor
    private ArgumentCaptor<List<Notification>> notificationsCaptor;

    private User adminUser;
    private AlertSignal testSignal;
    private Commune parisCommune;
    private Department parisDepartment;
    private Region ileDeFranceRegion;

    @BeforeEach
    void setUp() {
        // Create test entities
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

        // Region (Île-de-France)
        ileDeFranceRegion = new Region();
        ileDeFranceRegion.setId(11L);
        ileDeFranceRegion.setRegionCode("11");
        ileDeFranceRegion.setName("Île-de-France");

        // Department (Paris)
        parisDepartment = new Department();
        parisDepartment.setId(75L);
        parisDepartment.setDepartmentCode("75");
        parisDepartment.setName("Paris");
        parisDepartment.setRegion(ileDeFranceRegion);

        // Commune (Paris)
        parisCommune = new Commune();
        parisCommune.setId(1L);
        parisCommune.setInseeCode("75056");
        parisCommune.setName("Paris");
        parisCommune.setDepartment(parisDepartment);

        // Alert signal (Air quality alert)
        testSignal = new AlertSignal(
            AlertSignalSource.ATMO,
            AlertSignalKind.AQI,
            AlertSignalLevel.ALERT,
            GeographicScopeType.COMMUNE,
            1L,
            "High air pollution detected",
            LocalDateTime.now()
        );
        testSignal.setId(1L);
        testSignal.setDetails("PM2.5 levels exceed safe thresholds");
        testSignal.setValidFrom(LocalDateTime.now());
        testSignal.setValidTo(LocalDateTime.now().plusHours(24));
    }

    // ============================================
    // Test 1: Campaign Creation from Alert Signal
    // ============================================

    @Test
    @DisplayName("Should create draft campaign from alert signal with correct title and message")
    void shouldCreateDraftCampaignFromAlertSignal() {
        // Given
        Long signalId = 1L;
        when(alertSignalRepository.findById(signalId))
            .thenReturn(Optional.of(testSignal));
        when(campaignRepository.save(any(NotificationCampaign.class)))
            .thenAnswer(invocation -> {
                NotificationCampaign campaign = invocation.getArgument(0);
                campaign.setId(100L);
                return campaign;
            });

        // When
        NotificationCampaign result = campaignService.createDraftFromSignal(signalId, adminUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(NotificationCampaignStatus.DRAFT);
        assertThat(result.getCreatedBy()).isEqualTo(adminUser);
        assertThat(result.getAlertSignal()).isEqualTo(testSignal);

        // Verify title generation
        assertThat(result.getTitle()).isEqualTo("Air quality alert in Commune ID: 1");

        // Verify message contains signal details
        assertThat(result.getMessage())
            .contains("High air pollution detected")
            .contains("PM2.5 levels exceed safe thresholds")
            .contains("Level: Warning")
            .contains("Source: ATMO France");

        // Verify scope is copied from signal
        assertThat(result.getScopeType()).isEqualTo(GeographicScopeType.COMMUNE);
        assertThat(result.getScopeId()).isEqualTo(1L);

        verify(alertSignalRepository).findById(signalId);
        verify(campaignRepository).save(any(NotificationCampaign.class));
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException when signal not found")
    void shouldThrowExceptionWhenSignalNotFound() {
        // Given
        Long invalidSignalId = 999L;
        when(alertSignalRepository.findById(invalidSignalId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> campaignService.createDraftFromSignal(invalidSignalId, adminUser))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Alert signal not found with id: 999");

        verify(alertSignalRepository).findById(invalidSignalId);
        verify(campaignRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should generate correct title for different alert types")
    void shouldGenerateCorrectTitleForDifferentAlertTypes() {
        // Test HEAT alert
        AlertSignal heatSignal = new AlertSignal(
            AlertSignalSource.WEATHER,
            AlertSignalKind.HEAT,
            AlertSignalLevel.ALERT,
            GeographicScopeType.REGION,
            11L,
            "Heat wave warning",
            LocalDateTime.now()
        );
        heatSignal.setId(2L);

        when(alertSignalRepository.findById(2L)).thenReturn(Optional.of(heatSignal));
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        NotificationCampaign campaign = campaignService.createDraftFromSignal(2L, adminUser);
        assertThat(campaign.getTitle()).isEqualTo("Heat alert in Region ID: 11");

        // Test WIND alert
        AlertSignal windSignal = new AlertSignal(
            AlertSignalSource.WEATHER,
            AlertSignalKind.WIND,
            AlertSignalLevel.INFO,
            GeographicScopeType.DEPARTMENT,
            75L,
            "Strong winds expected",
            LocalDateTime.now()
        );
        windSignal.setId(3L);

        when(alertSignalRepository.findById(3L)).thenReturn(Optional.of(windSignal));

        campaign = campaignService.createDraftFromSignal(3L, adminUser);
        assertThat(campaign.getTitle()).isEqualTo("Strong wind expected in Department ID: 75");
    }

    // ============================================
    // Test 2: Manual Campaign Creation
    // ============================================

    @Test
    @DisplayName("Should create manual campaign without alert signal")
    void shouldCreateManualCampaign() {
        // Given
        CreateCampaignRequest request = new CreateCampaignRequest(
            "Test Campaign",
            "This is a test notification",
            GeographicScopeType.FRANCE,
            null
        );

        when(campaignRepository.save(any(NotificationCampaign.class)))
            .thenAnswer(invocation -> {
                NotificationCampaign campaign = invocation.getArgument(0);
                campaign.setId(200L);
                return campaign;
            });

        // When
        NotificationCampaign result = campaignService.createManualCampaign(request, adminUser);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTitle()).isEqualTo("Test Campaign");
        assertThat(result.getMessage()).isEqualTo("This is a test notification");
        assertThat(result.getScopeType()).isEqualTo(GeographicScopeType.FRANCE);
        assertThat(result.getScopeId()).isNull();
        assertThat(result.getCreatedBy()).isEqualTo(adminUser);
        assertThat(result.getStatus()).isEqualTo(NotificationCampaignStatus.DRAFT);
        assertThat(result.getAlertSignal()).isNull();

        verify(campaignRepository).save(any(NotificationCampaign.class));
    }

    // ============================================
    // Test 3: Recipient Fan-Out by Scope - CRITICAL (GDPR)
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should fan out to all users for FRANCE scope")
    void shouldFanOutToAllUsersForFranceScope() {
        // Given
        NotificationCampaign campaign = new NotificationCampaign(
            "National Alert",
            "Test message",
            GeographicScopeType.FRANCE,
            null,
            adminUser
        );
        campaign.setId(1L);
        campaign.setStatus(NotificationCampaignStatus.DRAFT);

        User user1 = createTestUser(10L, "user1@test.com", true);
        User user2 = createTestUser(11L, "user2@test.com", true);
        User user3 = createTestUser(12L, "user3@test.com", true);

        List<User> allUsers = Arrays.asList(user1, user2, user3);
        Page<User> userPage = new PageImpl<>(allUsers);

        when(campaignRepository.findById(1L)).thenReturn(Optional.of(campaign));
        when(userRepository.findByEmailVerifiedTrue(any(Pageable.class)))
            .thenReturn(userPage);
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        int recipientCount = campaignService.fanOutRecipients(1L);

        // Then
        assertThat(recipientCount).isEqualTo(3);

        verify(notificationRepository).saveAll(notificationsCaptor.capture());
        List<Notification> notifications = notificationsCaptor.getValue();

        assertThat(notifications).hasSize(3);
        assertThat(notifications).extracting(n -> n.getUserReceiver().getEmail())
            .containsExactlyInAnyOrder("user1@test.com", "user2@test.com", "user3@test.com");

        // Verify all notifications have correct properties
        notifications.forEach(notification -> {
            assertThat(notification.getCampaign()).isEqualTo(campaign);
            assertThat(notification.getTitle()).isEqualTo("National Alert");
            assertThat(notification.getMessage()).isEqualTo("Test message");
            assertThat(notification.getDeliveryStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
            assertThat(notification.getSendChannel()).isEqualTo(NotificationChannel.EMAIL);
        });

        // Verify campaign updated with recipient count
        verify(campaignRepository).save(argThat(c -> c.getTotalRecipients() == 3));
    }

    @Test
    @DisplayName("CRITICAL: Should fan out only to users with favorites in REGION scope")
    void shouldFanOutToUsersInRegionScope() {
        // Given
        Long regionId = 11L; // Île-de-France
        NotificationCampaign campaign = new NotificationCampaign(
            "Regional Alert",
            "Alert for Île-de-France",
            GeographicScopeType.REGION,
            regionId,
            adminUser
        );
        campaign.setId(2L);
        campaign.setStatus(NotificationCampaignStatus.DRAFT);

        User user1 = createTestUser(20L, "paris@test.com", true);
        User user2 = createTestUser(21L, "versailles@test.com", true);

        List<User> usersInRegion = Arrays.asList(user1, user2);

        when(campaignRepository.findById(2L)).thenReturn(Optional.of(campaign));
        when(userRepository.findUsersWithFavoritesInRegion(regionId))
            .thenReturn(usersInRegion);
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        int recipientCount = campaignService.fanOutRecipients(2L);

        // Then
        assertThat(recipientCount).isEqualTo(2);

        verify(userRepository).findUsersWithFavoritesInRegion(regionId);
        verify(notificationRepository).saveAll(notificationsCaptor.capture());

        List<Notification> notifications = notificationsCaptor.getValue();
        assertThat(notifications).hasSize(2);
        assertThat(notifications).extracting(n -> n.getUserReceiver().getId())
            .containsExactlyInAnyOrder(20L, 21L);
    }

    @Test
    @DisplayName("CRITICAL: Should fan out only to users with favorites in DEPARTMENT scope")
    void shouldFanOutToUsersInDepartmentScope() {
        // Given
        Long departmentId = 75L; // Paris department
        NotificationCampaign campaign = new NotificationCampaign(
            "Department Alert",
            "Alert for Paris department",
            GeographicScopeType.DEPARTMENT,
            departmentId,
            adminUser
        );
        campaign.setId(3L);
        campaign.setStatus(NotificationCampaignStatus.DRAFT);

        User user1 = createTestUser(30L, "dept-user1@test.com", true);

        when(campaignRepository.findById(3L)).thenReturn(Optional.of(campaign));
        when(userRepository.findUsersWithFavoritesInDepartment(departmentId))
            .thenReturn(Collections.singletonList(user1));
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        int recipientCount = campaignService.fanOutRecipients(3L);

        // Then
        assertThat(recipientCount).isEqualTo(1);

        verify(userRepository).findUsersWithFavoritesInDepartment(departmentId);
        verify(notificationRepository).saveAll(any());
    }

    @Test
    @DisplayName("CRITICAL: Should fan out only to users with favorites in COMMUNE scope")
    void shouldFanOutToUsersInCommuneScope() {
        // Given
        Long communeId = 1L;
        String inseeCode = "75056";

        NotificationCampaign campaign = new NotificationCampaign(
            "Commune Alert",
            "Alert for Paris commune",
            GeographicScopeType.COMMUNE,
            communeId,
            adminUser
        );
        campaign.setId(4L);
        campaign.setStatus(NotificationCampaignStatus.DRAFT);

        User user1 = createTestUser(40L, "commune-user1@test.com", true);
        User user2 = createTestUser(41L, "commune-user2@test.com", true);

        when(campaignRepository.findById(4L)).thenReturn(Optional.of(campaign));
        when(communeRepository.findById(communeId))
            .thenReturn(Optional.of(parisCommune));
        when(userRepository.findUsersWithFavoritesInCommune(inseeCode))
            .thenReturn(Arrays.asList(user1, user2));
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        int recipientCount = campaignService.fanOutRecipients(4L);

        // Then
        assertThat(recipientCount).isEqualTo(2);

        verify(communeRepository).findById(communeId);
        verify(userRepository).findUsersWithFavoritesInCommune(inseeCode);
        verify(notificationRepository).saveAll(notificationsCaptor.capture());

        List<Notification> notifications = notificationsCaptor.getValue();
        assertThat(notifications).hasSize(2);
    }

    @Test
    @DisplayName("CRITICAL: Should return empty list when scopeId is null for non-FRANCE scope")
    void shouldReturnEmptyListWhenScopeIdNullForNonFranceScope() {
        // Given
        NotificationCampaign campaign = new NotificationCampaign(
            "Invalid Campaign",
            "Missing scope ID",
            GeographicScopeType.REGION,
            null, // Invalid: REGION requires scopeId
            adminUser
        );
        campaign.setId(5L);
        campaign.setStatus(NotificationCampaignStatus.DRAFT);

        when(campaignRepository.findById(5L)).thenReturn(Optional.of(campaign));
        when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // When
        int recipientCount = campaignService.fanOutRecipients(5L);

        // Then
        assertThat(recipientCount).isZero();

        // Should not query users
        verify(userRepository, never()).findUsersWithFavoritesInRegion(anyLong());
        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should throw exception when campaign not found during fan-out")
    void shouldThrowExceptionWhenCampaignNotFoundDuringFanOut() {
        // Given
        Long invalidCampaignId = 999L;
        when(campaignRepository.findById(invalidCampaignId))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> campaignService.fanOutRecipients(invalidCampaignId))
            .isInstanceOf(EntityNotFoundException.class)
            .hasMessageContaining("Campaign not found with id: 999");

        verify(notificationRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Should throw exception when trying to fan out non-DRAFT campaign")
    void shouldThrowExceptionWhenFanOutNonDraftCampaign() {
        // Given
        NotificationCampaign campaign = new NotificationCampaign(
            "Already Sent",
            "Test",
            GeographicScopeType.FRANCE,
            null,
            adminUser
        );
        campaign.setId(6L);
        campaign.setStatus(NotificationCampaignStatus.COMPLETED); // Not DRAFT

        when(campaignRepository.findById(6L)).thenReturn(Optional.of(campaign));

        // When & Then
        assertThatThrownBy(() -> campaignService.fanOutRecipients(6L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Campaign must be in DRAFT status");

        verify(notificationRepository, never()).saveAll(any());
    }

    // ============================================
    // Test 4: Campaign Retrieval
    // ============================================

    @Test
    @DisplayName("Should retrieve campaign by ID")
    void shouldRetrieveCampaignById() {
        // Given
        NotificationCampaign campaign = new NotificationCampaign(
            "Test",
            "Message",
            GeographicScopeType.FRANCE,
            null,
            adminUser
        );
        campaign.setId(10L);

        when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));

        // When
        Optional<NotificationCampaign> result = campaignService.getCampaignById(10L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(10L);
        assertThat(result.get().getTitle()).isEqualTo("Test");

        verify(campaignRepository).findById(10L);
    }

    @Test
    @DisplayName("Should retrieve campaigns by admin with pagination")
    void shouldRetrieveCampaignsByAdmin() {
        // Given
        NotificationCampaign campaign1 = new NotificationCampaign("C1", "M1", GeographicScopeType.FRANCE, null, adminUser);
        NotificationCampaign campaign2 = new NotificationCampaign("C2", "M2", GeographicScopeType.FRANCE, null, adminUser);

        Page<NotificationCampaign> campaignPage = new PageImpl<>(Arrays.asList(campaign1, campaign2));
        Pageable pageable = PageRequest.of(0, 10);

        when(campaignRepository.findByCreatedById(adminUser.getId(), pageable))
            .thenReturn(campaignPage);

        // When
        Page<NotificationCampaign> result = campaignService.getCampaignsByAdmin(adminUser, pageable);

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).containsExactly(campaign1, campaign2);

        verify(campaignRepository).findByCreatedById(adminUser.getId(), pageable);
    }

    @Test
    @DisplayName("Should retrieve campaigns by status")
    void shouldRetrieveCampaignsByStatus() {
        // Given
        NotificationCampaign draftCampaign = new NotificationCampaign("Draft", "M", GeographicScopeType.FRANCE, null, adminUser);
        draftCampaign.setStatus(NotificationCampaignStatus.DRAFT);

        Page<NotificationCampaign> campaignPage = new PageImpl<>(Collections.singletonList(draftCampaign));
        Pageable pageable = PageRequest.of(0, 10);

        when(campaignRepository.findByStatus(NotificationCampaignStatus.DRAFT, pageable))
            .thenReturn(campaignPage);

        // When
        Page<NotificationCampaign> result = campaignService.getCampaignsByStatus(NotificationCampaignStatus.DRAFT, pageable);

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(NotificationCampaignStatus.DRAFT);

        verify(campaignRepository).findByStatus(NotificationCampaignStatus.DRAFT, pageable);
    }

    // ============================================
    // Helper Methods
    // ============================================

    private User createTestUser(Long id, String email, boolean emailVerified) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User" + id);
        user.setRole(UserRole.USER);
        user.setEmailVerified(emailVerified);
        return user;
    }
}
