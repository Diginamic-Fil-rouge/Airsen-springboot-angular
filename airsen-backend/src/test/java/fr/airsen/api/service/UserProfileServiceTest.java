package fr.airsen.api.service;

import fr.airsen.api.dto.response.PublicUserProfileDTO;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.ProfileVisibility;
import fr.airsen.api.exception.ProfileNotAccessibleException;
import fr.airsen.api.repository.ForumMessageRepository;
import fr.airsen.api.repository.ForumThreadRepository;
import fr.airsen.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Profile Service Tests")
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ForumThreadRepository forumThreadRepository;
    @Mock
    private ForumMessageRepository forumMessageRepository;

    @InjectMocks
    private UserProfileService userProfileService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setBio("Test bio");
        testUser.setCreatedAt(LocalDateTime.now().minusMonths(6));
        testUser.setEmailVerified(true);
        testUser.setActive(true);
    }

    @Test
    @DisplayName("getPublicProfile_Success_PublicUser: Should return profile for public user")
    void getPublicProfile_Success_PublicUser() {
        // Given
        testUser.setProfileVisibility(ProfileVisibility.PUBLIC);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(forumThreadRepository.countByAuthorId(1L)).thenReturn(5L);
        when(forumMessageRepository.countByAuthorId(1L)).thenReturn(20L);

        // When
        PublicUserProfileDTO profile = userProfileService.getPublicProfile(1L);

        // Then
        assertNotNull(profile);
        assertEquals("test", profile.displayName());
        assertEquals("Test bio", profile.bio());
        assertEquals(5, profile.forumActivity().threadCount());
        assertEquals(20, profile.forumActivity().messageCount());
    }

    @Test
    @DisplayName("getPublicProfile_NotPublic_UsernameOnly: Should throw exception")
    void getPublicProfile_NotPublic_UsernameOnly() {
        // Given
        testUser.setProfileVisibility(ProfileVisibility.USERNAME_ONLY);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(ProfileNotAccessibleException.class, () -> {
            userProfileService.getPublicProfile(1L);
        });
    }

    @Test
    @DisplayName("getPublicProfile_NotPublic_Hidden: Should throw exception")
    void getPublicProfile_NotPublic_Hidden() {
        // Given
        testUser.setProfileVisibility(ProfileVisibility.HIDDEN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(ProfileNotAccessibleException.class, () -> {
            userProfileService.getPublicProfile(1L);
        });
    }

    @Test
    @DisplayName("getPublicProfile_DeletedUser: Should throw exception for deleted user")
    void getPublicProfile_DeletedUser() {
        // Given
        testUser.setProfileVisibility(ProfileVisibility.PUBLIC);
        testUser.setDeletedAt(LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(ProfileNotAccessibleException.class, () -> {
            userProfileService.getPublicProfile(1L);
        });
    }

    @Test
    @DisplayName("getPublicProfile_InactiveUser: Should throw exception for inactive user")
    void getPublicProfile_InactiveUser() {
        // Given
        testUser.setProfileVisibility(ProfileVisibility.PUBLIC);
        testUser.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThrows(ProfileNotAccessibleException.class, () -> {
            userProfileService.getPublicProfile(1L);
        });
    }

    @Test
    @DisplayName("getPublicProfile_UserNotFound: Should throw exception for invalid user ID")
    void getPublicProfile_UserNotFound() {
        // Given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(ProfileNotAccessibleException.class, () -> {
            userProfileService.getPublicProfile(99L);
        });
    }

    @Test
    @DisplayName("updatePrivacySettings_ToPublic: Should correctly update visibility to PUBLIC")
    void updatePrivacySettings_ToPublic() {
        // Given
        testUser.setProfileVisibility(ProfileVisibility.USERNAME_ONLY);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When
        userProfileService.updatePrivacySettings(1L, ProfileVisibility.PUBLIC);

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(ProfileVisibility.PUBLIC, savedUser.getProfileVisibility());
    }

    @Test
    @DisplayName("updatePrivacySettings_ToHidden: Should correctly update visibility to HIDDEN")
    void updatePrivacySettings_ToHidden() {
        // Given
        testUser.setProfileVisibility(ProfileVisibility.PUBLIC);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        // When
        userProfileService.updatePrivacySettings(1L, ProfileVisibility.HIDDEN);

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertEquals(ProfileVisibility.HIDDEN, savedUser.getProfileVisibility());
    }

    @Test
    @DisplayName("getForumStats_WithActivity: Should return correct forum activity counts")
    void getForumStats_WithActivity() {
        // Given
        testUser.setProfileVisibility(ProfileVisibility.PUBLIC);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(forumThreadRepository.countByAuthorId(1L)).thenReturn(5L);
        when(forumMessageRepository.countByAuthorId(1L)).thenReturn(20L);

        // When
        PublicUserProfileDTO profile = userProfileService.getPublicProfile(1L);

        // Then
        assertNotNull(profile.forumActivity());
        assertEquals(5, profile.forumActivity().threadCount());
        assertEquals(20, profile.forumActivity().messageCount());
    }

    @Test
    @DisplayName("getForumStats_NoActivity: Should return zero counts for no activity")
    void getForumStats_NoActivity() {
        // Given
        testUser.setProfileVisibility(ProfileVisibility.PUBLIC);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(forumThreadRepository.countByAuthorId(1L)).thenReturn(0L);
        when(forumMessageRepository.countByAuthorId(1L)).thenReturn(0L);

        // When
        PublicUserProfileDTO profile = userProfileService.getPublicProfile(1L);

        // Then
        assertNotNull(profile.forumActivity());
        assertEquals(0, profile.forumActivity().threadCount());
        assertEquals(0, profile.forumActivity().messageCount());
    }
}