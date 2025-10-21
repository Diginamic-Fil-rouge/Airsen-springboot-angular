package fr.airsen.api.service;

import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.dto.response.ForumActivityStatsDTO;
import fr.airsen.api.dto.response.PublicUserProfileDTO;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.ProfileVisibility;
import fr.airsen.api.exception.ProfileNotAccessibleException;
import fr.airsen.api.mapper.UserMapper;
import fr.airsen.api.repository.ForumMessageRepository;
import fr.airsen.api.repository.ForumThreadRepository;
import fr.airsen.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user profile privacy controls and public profile access.
 *
 * @see ProfileVisibility
 * @see ProfileNotAccessibleException
 * @see PublicUserProfileDTO
 * @since 1.0
 */
@Service
public class UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileService.class);

    private final UserRepository userRepository;
    private final ForumThreadRepository forumThreadRepository;
    private final ForumMessageRepository forumMessageRepository;
    private final UserMapper userMapper;

    /**
     * Constructs UserProfileService with required dependencies.
     *
     * @param userRepository repository for User entities
     * @param forumThreadRepository repository for ForumThread entities
     * @param forumMessageRepository repository for ForumMessage entities
     * @param userMapper mapper for converting User entities to DTOs
     */
    public UserProfileService(
            UserRepository userRepository,
            ForumThreadRepository forumThreadRepository,
            ForumMessageRepository forumMessageRepository,
            UserMapper userMapper) {
        this.userRepository = userRepository;
        this.forumThreadRepository = forumThreadRepository;
        this.forumMessageRepository = forumMessageRepository;
        this.userMapper = userMapper;
    }

    /**
     * Retrieves the public profile of a user if accessible.
     *
     * @param userId unique identifier of the user whose profile is being accessed
     * @return public user profile DTO with bio, stats, and account information
     * @throws ProfileNotAccessibleException if profile cannot be accessed for any reason
     *         (user not found, deleted, inactive, or not public)
     */
    public PublicUserProfileDTO getPublicProfile(Long userId) {
        logger.debug("Attempting to retrieve public profile for user ID: {}", userId);

        // Step 1: Find user by ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    logger.warn("Profile access denied - user not found: {}", userId);
                    return new ProfileNotAccessibleException("User profile not found");
                });

        // Step 2: Check if user is deleted
        if (user.isMarkedForDeletion()) {
            logger.warn("Profile access denied - user deleted: {}", userId);
            throw new ProfileNotAccessibleException("User profile not found");
        }

        // Step 3: Check if user is active
        if (!user.getIsActive()) {
            logger.warn("Profile access denied - user inactive: {}", userId);
            throw new ProfileNotAccessibleException("User profile not found");
        }

        // Step 4: Check if profile is public
        if (!user.isProfilePublic()) {
            logger.warn("Profile access denied - not public (visibility: {}): {}",
                    user.getProfileVisibility(), userId);
            throw new ProfileNotAccessibleException("User profile not found");
        }

        // Step 5: Get forum activity statistics
        ForumActivityStatsDTO stats = getForumStats(userId);

        // Step 6: Build public profile DTO
        PublicUserProfileDTO profile = new PublicUserProfileDTO(
                user.getDisplayName(),
                user.getBio(),
                user.getCreatedAt(),
                user.getEmailVerified(),
                user.getIsActive(),
                stats
        );

        logger.info("Public profile successfully retrieved for user ID: {} (display name: {})",
                userId, user.getDisplayName());

        return profile;
    }

    /**
     * Updates a user's profile visibility settings.
     *
     * @param userId unique identifier of the user whose settings are being updated
     * @param visibility new profile visibility level (HIDDEN, USERNAME_ONLY, or PUBLIC)
     * @return updated user DTO reflecting the new privacy settings
     * @throws EntityNotFoundException if user with the given ID does not exist
     * @throws IllegalArgumentException if visibility parameter is null
     */
    @Transactional
    public UserDTO updatePrivacySettings(Long userId, ProfileVisibility visibility) {
        if (visibility == null) {
            throw new IllegalArgumentException("Profile visibility cannot be null");
        }

        logger.info("Updating privacy settings for user ID: {} to visibility: {}", userId, visibility);

        // Find user by ID
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        // Get previous visibility for logging
        ProfileVisibility previousVisibility = user.getProfileVisibility();

        // Update visibility
        user.setProfileVisibility(visibility);

        // Save user
        userRepository.save(user);

        logger.info("Privacy settings updated for user ID: {} from {} to {}",
                userId, previousVisibility, visibility);

        // Convert to DTO and return
        return userMapper.toDTO(user);
    }

    /**
     * Calculates forum activity statistics for a user.
     *
     * @param userId unique identifier of the user whose forum stats are being calculated
     * @return forum activity statistics DTO with thread and message counts
     */
    private ForumActivityStatsDTO getForumStats(Long userId) {
        logger.debug("Calculating forum statistics for user ID: {}", userId);

        // Count threads created by user
        long threadCount = forumThreadRepository.countByAuthorId(userId);

        // Count messages posted by user
        long messageCount = forumMessageRepository.countByAuthorId(userId);

        logger.debug("Forum stats for user ID: {} - Threads: {}, Messages: {}",
                userId, threadCount, messageCount);

        return new ForumActivityStatsDTO(threadCount, messageCount);
    }
}
