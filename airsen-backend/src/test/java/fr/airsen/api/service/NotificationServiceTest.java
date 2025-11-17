package fr.airsen.api.service;

import fr.airsen.api.entity.Notification;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.NotificationType;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.repository.NotificationCampaignRepository;
import fr.airsen.api.repository.NotificationRepository;
import fr.airsen.api.repository.UserRepository;
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
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NotificationService.
 *
 * Tests critical notification delivery functionality:
 * - Email delivery (async)
 * - Notification state management
 * - User notification retrieval
 * - Error handling for failed deliveries
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationCampaignRepository notificationCampaignRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    @Captor
    private ArgumentCaptor<SimpleMailMessage> mailMessageCaptor;

    private User senderUser;
    private User recipientUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        setupTestEntities();
        setupEmailConfiguration();
    }

    private void setupTestEntities() {
        // Sender user
        senderUser = new User();
        senderUser.setId(1L);
        senderUser.setEmail("admin@airsen.fr");
        senderUser.setFirstName("Admin");
        senderUser.setLastName("User");
        senderUser.setRole(UserRole.ADMIN);

        // Recipient user
        recipientUser = new User();
        recipientUser.setId(2L);
        recipientUser.setEmail("user@airsen.fr");
        recipientUser.setFirstName("Test");
        recipientUser.setLastName("User");
        recipientUser.setRole(UserRole.USER);
        recipientUser.setEmailVerified(true);

        // Test notification
        testNotification = new Notification(
            senderUser,
            recipientUser,
            "Test Notification",
            "This is a test message",
            NotificationType.EMAIL
        );
        testNotification.setId(1L);
    }

    private void setupEmailConfiguration() {
        // Set mail configuration via reflection (simulating @Value injection)
        ReflectionTestUtils.setField(notificationService, "fromAddress", "noreply@airsen.fr");
        ReflectionTestUtils.setField(notificationService, "fromName", "Airsen");
    }

    // ============================================
    // Test 1: Create Notification
    // ============================================

    @Test
    @DisplayName("Should create notification successfully")
    void shouldCreateNotificationSuccessfully() {
        // Given
        Long senderId = 1L;
        Long recipientId = 2L;
        String title = "New Notification";
        String message = "Test message";
        NotificationType type = NotificationType.EMAIL;

        when(userRepository.findById(senderId)).thenReturn(Optional.of(senderUser));
        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipientUser));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(invocation -> {
                Notification n = invocation.getArgument(0);
                n.setId(10L);
                return n;
            });

        // When
        Notification result = notificationService.createNotification(
            senderId, recipientId, title, message, type
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(10L);

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();

        assertThat(captured.getUser()).isEqualTo(senderUser);
        assertThat(captured.getUserReceiver()).isEqualTo(recipientUser);
        assertThat(captured.getTitle()).isEqualTo(title);
        assertThat(captured.getMessage()).isEqualTo(message);
        assertThat(captured.getNotificationType()).isEqualTo(type);
    }

    @Test
    @DisplayName("Should throw exception when sender not found")
    void shouldThrowExceptionWhenSenderNotFound() {
        // Given
        Long invalidSenderId = 999L;
        when(userRepository.findById(invalidSenderId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationService.createNotification(
            invalidSenderId, 2L, "Title", "Message", NotificationType.EMAIL
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Sender not found with id: 999");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when recipient not found")
    void shouldThrowExceptionWhenRecipientNotFound() {
        // Given
        Long invalidRecipientId = 999L;
        when(userRepository.findById(1L)).thenReturn(Optional.of(senderUser));
        when(userRepository.findById(invalidRecipientId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> notificationService.createNotification(
            1L, invalidRecipientId, "Title", "Message", NotificationType.EMAIL
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Recipient not found with id: 999");

        verify(notificationRepository, never()).save(any());
    }

    // ============================================
    // Test 2: Create System Notification
    // ============================================

    @Test
    @DisplayName("Should create system notification for recipient")
    void shouldCreateSystemNotificationForRecipient() {
        // Given
        Long recipientId = 2L;
        String title = "System Alert";
        String message = "Air quality alert in your area";

        when(userRepository.findById(recipientId)).thenReturn(Optional.of(recipientUser));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // When
        Notification result = notificationService.createSystemNotification(
            recipientId, title, message
        );

        // Then
        assertThat(result).isNotNull();

        verify(notificationRepository).save(notificationCaptor.capture());
        Notification captured = notificationCaptor.getValue();

        // System notifications use recipient as both sender and receiver
        assertThat(captured.getUser()).isEqualTo(recipientUser);
        assertThat(captured.getUserReceiver()).isEqualTo(recipientUser);
        assertThat(captured.getTitle()).isEqualTo(title);
        assertThat(captured.getMessage()).isEqualTo(message);
    }

    // ============================================
    // Test 3: Get Notifications
    // ============================================

    @Test
    @DisplayName("Should retrieve notifications by recipient ID")
    void shouldRetrieveNotificationsByRecipientId() {
        // Given
        Long recipientId = 2L;
        Notification notification1 = new Notification(senderUser, recipientUser, "N1", "M1");
        Notification notification2 = new Notification(senderUser, recipientUser, "N2", "M2");

        Page<Notification> notificationsPage = new PageImpl<>(
            Arrays.asList(notification1, notification2),
            PageRequest.of(0, 10),
            2
        );

        when(notificationRepository.findByRecipientId(eq(recipientId), any(Pageable.class)))
            .thenReturn(notificationsPage);

        // When
        Page<Notification> result = notificationService.getNotificationsByRecipientId(
            recipientId, PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);

        verify(notificationRepository).findByRecipientId(eq(recipientId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should retrieve unread notifications by recipient ID")
    void shouldRetrieveUnreadNotificationsByRecipientId() {
        // Given
        Long recipientId = 2L;
        Notification unreadNotification = new Notification(senderUser, recipientUser, "Unread", "Message");

        Page<Notification> unreadPage = new PageImpl<>(
            Collections.singletonList(unreadNotification),
            PageRequest.of(0, 10),
            1
        );

        when(notificationRepository.findUnreadByRecipientId(eq(recipientId), any(Pageable.class)))
            .thenReturn(unreadPage);

        // When
        Page<Notification> result = notificationService.getUnreadNotificationsByRecipientId(
            recipientId, PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Unread");

        verify(notificationRepository).findUnreadByRecipientId(eq(recipientId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should retrieve notifications by sender ID")
    void shouldRetrieveNotificationsBySenderId() {
        // Given
        Long senderId = 1L;
        Page<Notification> sentPage = new PageImpl<>(
            Arrays.asList(testNotification),
            PageRequest.of(0, 10),
            1
        );

        when(notificationRepository.findBySenderId(eq(senderId), any(Pageable.class)))
            .thenReturn(sentPage);

        // When
        Page<Notification> result = notificationService.getNotificationsBySenderId(
            senderId, PageRequest.of(0, 10)
        );

        // Then
        assertThat(result.getContent()).hasSize(1);

        verify(notificationRepository).findBySenderId(eq(senderId), any(Pageable.class));
    }

    @Test
    @DisplayName("Should retrieve notification by ID")
    void shouldRetrieveNotificationById() {
        // Given
        Long notificationId = 1L;
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(testNotification));

        // When
        Optional<Notification> result = notificationService.getNotificationById(notificationId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(testNotification);

        verify(notificationRepository).findById(notificationId);
    }

    // ============================================
    // Test 4: Mark Notification Status
    // ============================================

    @Test
    @DisplayName("Should mark notification as sent")
    void shouldMarkNotificationAsSent() {
        // Given
        Long notificationId = 1L;
        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // When
        Notification result = notificationService.markNotificationAsSent(notificationId);

        // Then
        assertThat(result.wasSent()).isTrue();
        assertThat(result.getSentDate()).isNotNull();

        verify(notificationRepository).save(testNotification);
    }

    @Test
    @DisplayName("Should mark notification as failed with error message")
    void shouldMarkNotificationAsFailedWithErrorMessage() {
        // Given
        Long notificationId = 1L;
        String errorMessage = "SMTP server unavailable";

        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // When
        Notification result = notificationService.markNotificationAsFailed(notificationId, errorMessage);

        // Then
        assertThat(result.hasFailed()).isTrue();
        assertThat(result.getErrorMessage()).isEqualTo(errorMessage);

        verify(notificationRepository).save(testNotification);
    }

    @Test
    @DisplayName("Should mark all notifications as read for user")
    void shouldMarkAllNotificationsAsReadForUser() {
        // Given
        Long userId = 2L;
        doNothing().when(notificationRepository).markAllAsReadForUser(userId);

        // When
        notificationService.markAllNotificationsAsReadForUser(userId);

        // Then
        verify(notificationRepository).markAllAsReadForUser(userId);
    }

    // ============================================
    // Test 5: Delete Notification
    // ============================================

    @Test
    @DisplayName("Should delete notification successfully")
    void shouldDeleteNotificationSuccessfully() {
        // Given
        Long notificationId = 1L;
        when(notificationRepository.existsById(notificationId)).thenReturn(true);
        doNothing().when(notificationRepository).deleteById(notificationId);

        // When
        notificationService.deleteNotification(notificationId);

        // Then
        verify(notificationRepository).existsById(notificationId);
        verify(notificationRepository).deleteById(notificationId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent notification")
    void shouldThrowExceptionWhenDeletingNonExistentNotification() {
        // Given
        Long invalidId = 999L;
        when(notificationRepository.existsById(invalidId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> notificationService.deleteNotification(invalidId))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Notification not found with id: 999");

        verify(notificationRepository, never()).deleteById(anyLong());
    }

    // ============================================
    // Test 6: Send Email Notification (Async) - CRITICAL
    // ============================================

    @Test
    @DisplayName("CRITICAL: Should send email notification successfully")
    void shouldSendEmailNotificationSuccessfully() throws Exception {
        // Given
        Long notificationId = 1L;
        // Notification already configured for email delivery by default constructor

        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        // When
        CompletableFuture<Boolean> future = notificationService.sendEmailNotificationAsync(notificationId);
        Boolean result = future.get();  // Wait for async completion

        // Then
        assertThat(result).isTrue();

        verify(mailSender).send(mailMessageCaptor.capture());
        SimpleMailMessage sentMail = mailMessageCaptor.getValue();

        assertThat(sentMail.getTo()).contains("user@airsen.fr");
        assertThat(sentMail.getSubject()).isEqualTo("Test Notification");
        assertThat(sentMail.getText()).isEqualTo("This is a test message");
        assertThat(sentMail.getFrom()).isEqualTo("Airsen <noreply@airsen.fr>");

        // Verify notification marked as sent
        verify(notificationRepository, atLeastOnce()).save(argThat(n -> n.wasSent()));
    }

    @Test
    @DisplayName("Should handle email sending failure gracefully")
    void shouldHandleEmailSendingFailureGracefully() throws Exception {
        // Given
        Long notificationId = 1L;
        // Notification already configured for email delivery by default constructor

        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        doThrow(new RuntimeException("SMTP connection failed"))
            .when(mailSender).send(any(SimpleMailMessage.class));

        // When
        CompletableFuture<Boolean> future = notificationService.sendEmailNotificationAsync(notificationId);
        Boolean result = future.get();

        // Then
        assertThat(result).isFalse();

        verify(mailSender).send(any(SimpleMailMessage.class));

        // Verify notification marked as failed
        verify(notificationRepository, atLeastOnce()).save(argThat(n ->
            n.hasFailed() && n.getErrorMessage().contains("Failed to send email")
        ));
    }

    @Test
    @DisplayName("Should not send email for invalid notification configuration")
    void shouldNotSendEmailForInvalidNotificationConfiguration() throws Exception {
        // Given
        Long notificationId = 1L;
        // Make notification invalid by setting emailVerified to false
        recipientUser.setEmailVerified(false);

        when(notificationRepository.findById(notificationId))
            .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        // When
        CompletableFuture<Boolean> future = notificationService.sendEmailNotificationAsync(notificationId);
        Boolean result = future.get();

        // Then
        assertThat(result).isFalse();

        // Should not attempt to send email
        verify(mailSender, never()).send(any(SimpleMailMessage.class));

        // Should mark as failed
        verify(notificationRepository, atLeastOnce()).save(argThat(n -> n.hasFailed()));
    }
}
