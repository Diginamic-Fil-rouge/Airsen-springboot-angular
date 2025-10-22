package fr.airsen.api.service;

import fr.airsen.api.entity.*;
import fr.airsen.api.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Deletion Service Tests")
class UserDeletionServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ForumThreadRepository forumThreadRepository;
    @Mock
    private ForumMessageRepository forumMessageRepository;
    @Mock
    private ForumVoteRepository forumVoteRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserFavoritesService userFavoritesService;
    // FIX 2: Removed AuditService mock as it's not yet integrated

    @InjectMocks
    private UserDeletionService userDeletionService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setIsActive(true);
    }

    @Test
    @DisplayName("softDeleteUser_Success: Should mark user as deleted")
    void softDeleteUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        userDeletionService.softDeleteUser(1L, "GDPR Request", 2L);

        // Then
        assertNotNull(testUser.getDeletedAt());
        assertFalse(testUser.getIsActive());
        assertEquals("GDPR Request", testUser.getDeletionReason());
        verify(userRepository).save(testUser);
        // FIX 2: Removed AuditService verification
    }

    @Test
    @DisplayName("softDeleteUser_UserNotFound: Should throw EntityNotFoundException")
    void softDeleteUser_UserNotFound() {
        // Given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> {
            userDeletionService.softDeleteUser(99L, "Reason", 2L);
        });
    }

    @Test
    @DisplayName("hardDeleteUser_Success: Should delete user and all related data")
    void hardDeleteUser_Success() {
        // Given
        testUser.setDeletedAt(LocalDateTime.now().minusDays(31));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(forumThreadRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(forumMessageRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(alertRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(notificationRepository.deleteAllForUser(1L)).thenReturn(0);

        // When
        userDeletionService.hardDeleteUser(1L);

        // Then
        verify(forumThreadRepository).findByAuthor(testUser);
        verify(forumMessageRepository).findByAuthor(testUser);
        verify(alertRepository).findByUserId(1L);
        verify(notificationRepository).deleteAllForUser(1L);
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("hardDeleteUser_GracePeriodNotExpired: Should throw IllegalStateException")
    void hardDeleteUser_GracePeriodNotExpired() {
        // Given
        testUser.setDeletedAt(LocalDateTime.now().minusDays(10));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(IllegalStateException.class, () -> {
            userDeletionService.hardDeleteUser(1L);
        });
        verify(userRepository, never()).delete(any(User.class));
    }

    @Test
    @DisplayName("hardDeleteUser_UserNotFound: Should throw EntityNotFoundException")
    void hardDeleteUser_UserNotFound() {
        // Given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(EntityNotFoundException.class, () -> {
            userDeletionService.hardDeleteUser(99L);
        });
    }

    @Test
    @DisplayName("preserveAuthorNameInForumContent_ThreadsPreserved: Should update threads correctly")
    void preserveAuthorNameInForumContent_ThreadsPreserved() {
        // Given
        testUser.setDeletedAt(LocalDateTime.now().minusDays(31));
        ForumThread thread1 = new ForumThread();
        thread1.setAuthor(testUser);
        ForumThread thread2 = new ForumThread();
        thread2.setAuthor(testUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(forumThreadRepository.findByAuthor(testUser)).thenReturn(List.of(thread1, thread2));
        when(forumMessageRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(alertRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(notificationRepository.deleteAllForUser(1L)).thenReturn(0);

        // When
        userDeletionService.hardDeleteUser(1L);

        // Then
        verify(forumThreadRepository, times(2)).save(any(ForumThread.class));
        assertNull(thread1.getAuthor());
        assertTrue(thread1.getAuthorDeleted());
        assertEquals("Test User", thread1.getAuthorName());
    }

    @Test
    @DisplayName("preserveAuthorNameInForumContent_MessagesPreserved: Should update messages correctly")
    void preserveAuthorNameInForumContent_MessagesPreserved() {
        // Given
        testUser.setDeletedAt(LocalDateTime.now().minusDays(31));
        ForumMessage message1 = new ForumMessage();
        message1.setAuthor(testUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(forumThreadRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(forumMessageRepository.findByAuthor(testUser)).thenReturn(List.of(message1));
        when(alertRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(notificationRepository.deleteAllForUser(1L)).thenReturn(0);

        // When
        userDeletionService.hardDeleteUser(1L);

        // Then
        verify(forumMessageRepository).save(any(ForumMessage.class));
        assertNull(message1.getAuthor());
        assertTrue(message1.getAuthorDeleted());
        assertEquals("Test User", message1.getAuthorName());
    }

    @Test
    @DisplayName("deleteUserAlerts_Success: Should delete all user alerts")
    void deleteUserAlerts_Success() {
        // Given
        testUser.setDeletedAt(LocalDateTime.now().minusDays(31));
        Alert alert1 = new Alert();
        Alert alert2 = new Alert();
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(forumThreadRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(forumMessageRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(alertRepository.findByUserId(1L)).thenReturn(List.of(alert1, alert2));
        when(notificationRepository.deleteAllForUser(1L)).thenReturn(0);

        // When
        userDeletionService.hardDeleteUser(1L);

        // Then
        verify(alertRepository).deleteAll(List.of(alert1, alert2));
    }

    @Test
    @DisplayName("deleteUserFavorites_Success: Should clear all user favorites")
    void deleteUserFavorites_Success() {
        // Given
        testUser.setDeletedAt(LocalDateTime.now().minusDays(31));

        // Create UserFavorite entity (updated to use join entity instead of direct Commune)
        Commune commune = new Commune();
        commune.setInseeCode("75056");
        UserFavorite favorite = new UserFavorite();
        favorite.setUser(testUser);
        favorite.setCommune(commune);
        testUser.setFavorites(new HashSet<>(Collections.singletonList(favorite)));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(forumThreadRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(forumMessageRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(alertRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(notificationRepository.deleteAllForUser(1L)).thenReturn(0);

        // When
        userDeletionService.hardDeleteUser(1L);

        // Then
        verify(userFavoritesService).removeAllFavorites(1L);
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("anonymizeNotifications_ReceivedNotifications: Should delete notifications")
    void anonymizeNotifications_ReceivedNotifications() {
        // Given
        testUser.setDeletedAt(LocalDateTime.now().minusDays(31));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(forumThreadRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(forumMessageRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(alertRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(notificationRepository.deleteAllForUser(1L)).thenReturn(4);

        // When
        userDeletionService.hardDeleteUser(1L);

        // Then
        verify(notificationRepository).deleteAllForUser(1L);
    }

    @Test
    @DisplayName("anonymizeForumVotes_Success: Should anonymize forum votes")
    void anonymizeForumVotes_Success() {
        // Given
        testUser.setDeletedAt(LocalDateTime.now().minusDays(31));
        ForumVote vote1 = new ForumVote();
        vote1.setUser(testUser);
        testUser.setVotes(List.of(vote1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(forumThreadRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(forumMessageRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(alertRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(notificationRepository.deleteAllForUser(1L)).thenReturn(0);

        // When
        userDeletionService.hardDeleteUser(1L);

        // Then
        verify(forumVoteRepository).save(any(ForumVote.class));
        assertNull(vote1.getUser());
        assertTrue(vote1.getUserDeleted());
    }

    @Test
    @DisplayName("hardDeleteUser_CompleteFlow: Should correctly handle all related data")
    void hardDeleteUser_CompleteFlow() {
        // Given
        testUser.setDeletedAt(LocalDateTime.now().minusDays(40));
        ForumThread thread = new ForumThread();
        thread.setAuthor(testUser);
        ForumMessage message = new ForumMessage();
        message.setAuthor(testUser);
        ForumVote vote = new ForumVote();
        vote.setUser(testUser);
        testUser.setVotes(List.of(vote));

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(forumThreadRepository.findByAuthor(testUser)).thenReturn(List.of(thread));
        when(forumMessageRepository.findByAuthor(testUser)).thenReturn(List.of(message));
        when(alertRepository.findByUserId(1L)).thenReturn(List.of(new Alert()));
        when(notificationRepository.deleteAllForUser(1L)).thenReturn(1);

        // When
        userDeletionService.hardDeleteUser(1L);

        // Then
        verify(forumThreadRepository).save(any(ForumThread.class));
        verify(forumMessageRepository).save(any(ForumMessage.class));
        verify(alertRepository).deleteAll(anyList());
        verify(notificationRepository).deleteAllForUser(1L);
        verify(forumVoteRepository).save(any(ForumVote.class));
        verify(userRepository).delete(testUser);
    }

    @Test
    @DisplayName("hardDeleteUser_ForumContentIntact: Should preserve thread and message content")
    void hardDeleteUser_ForumContentIntact() {
        // Given
        testUser.setDeletedAt(LocalDateTime.now().minusDays(35));
        ForumThread thread = new ForumThread();
        thread.setTitle("Original Title");
        thread.setContent("Original Content");
        thread.setAuthor(testUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(forumThreadRepository.findByAuthor(testUser)).thenReturn(List.of(thread));
        when(forumMessageRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(alertRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(notificationRepository.deleteAllForUser(1L)).thenReturn(0);

        // When
        userDeletionService.hardDeleteUser(1L);

        // Then
        verify(forumThreadRepository).save(thread);
        assertEquals("Original Title", thread.getTitle());
        assertEquals("Original Content", thread.getContent());
        assertNull(thread.getAuthor());
        assertEquals("Test User", thread.getAuthorName());
    }

    @Test
    @DisplayName("hardDeleteUser_NoForumContent: Should succeed when user has no content")
    void hardDeleteUser_NoForumContent() {
        // Given
        testUser.setDeletedAt(LocalDateTime.now().minusDays(32));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(forumThreadRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(forumMessageRepository.findByAuthor(testUser)).thenReturn(Collections.emptyList());
        when(alertRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(notificationRepository.deleteAllForUser(1L)).thenReturn(0);

        // When
        userDeletionService.hardDeleteUser(1L);

        // Then
        verify(userRepository).delete(testUser);
        verify(forumThreadRepository, never()).save(any());
        verify(forumMessageRepository, never()).save(any());
    }
}
