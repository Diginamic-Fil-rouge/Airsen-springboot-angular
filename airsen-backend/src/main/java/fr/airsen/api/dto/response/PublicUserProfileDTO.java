package fr.airsen.api.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for public user profile information.
 *
 * <p>This immutable record represents the public-facing view of a user profile in AIRSEN.
 * It is only returned for users with PUBLIC profile visibility and excludes sensitive
 * information like email address, password, or personal contact details.</p>
 *
 * <p><strong>GDPR Compliance:</strong> This DTO implements Article 25 (Data Protection by
 * Design and Default) by exposing only the minimum necessary information for public profiles.
 * Users with HIDDEN or USERNAME_ONLY visibility do not have their profiles exposed via this DTO.</p>
 *
 * <h3>Profile Visibility Matrix:</h3>
 * <table border="1">
 *   <tr>
 *     <th>Visibility</th>
 *     <th>Profile Accessible?</th>
 *     <th>Data Exposed</th>
 *   </tr>
 *   <tr>
 *     <td>HIDDEN</td>
 *     <td>No (404)</td>
 *     <td>None</td>
 *   </tr>
 *   <tr>
 *     <td>USERNAME_ONLY</td>
 *     <td>No (404)</td>
 *     <td>Username only (in forum posts)</td>
 *   </tr>
 *   <tr>
 *     <td>PUBLIC</td>
 *     <td>Yes</td>
 *     <td>displayName, bio, createdAt, emailVerified, isActive, forumStats</td>
 *   </tr>
 * </table>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * ForumActivityStatsDTO stats = new ForumActivityStatsDTO(15L, 87L);
 * PublicUserProfileDTO profile = new PublicUserProfileDTO(
 *     "Marie Dupont",
 *     "Environmental advocate passionate about air quality monitoring",
 *     LocalDateTime.of(2023, 1, 15, 10, 30),
 *     true,
 *     true,
 *     stats
 * );
 * }</pre>
 *
 * @param displayName user's display name (full name or email fallback)
 * @param bio user's biographical description (optional, can be null)
 * @param createdAt timestamp when the user account was created
 * @param emailVerified whether the user's email address has been verified
 * @param isActive whether the user account is active (can access the platform)
 * @param forumStats aggregated forum activity statistics (thread and message counts)
 *
 * @see ForumActivityStatsDTO
 * @see fr.airsen.api.entity.enums.ProfileVisibility
 * @see fr.airsen.api.service.UserProfileService
 * @since 1.0
 */
public record PublicUserProfileDTO(
        String displayName,
        String bio,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
        LocalDateTime createdAt,
        Boolean emailVerified,
        Boolean isActive,
        ForumActivityStatsDTO forumStats
) {
    /**
     * Compact constructor with validation and default values.
     *
     * <p>Ensures that required fields are non-null and provides sensible defaults
     * for optional fields.</p>
     *
     * @param displayName user's display name (required, cannot be null)
     * @param bio user's bio (optional, can be null or empty)
     * @param createdAt account creation date (required, cannot be null)
     * @param emailVerified email verification status (defaults to false if null)
     * @param isActive account active status (defaults to true if null)
     * @param forumStats forum activity statistics (required, cannot be null)
     * @throws IllegalArgumentException if required fields are null
     */
    public PublicUserProfileDTO {
        // Validation: required fields
        if (displayName == null || displayName.trim().isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be null or empty");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("Created date cannot be null");
        }
        if (forumStats == null) {
            throw new IllegalArgumentException("Forum stats cannot be null");
        }

        // Default values for optional fields
        if (emailVerified == null) {
            emailVerified = false;
        }
        if (isActive == null) {
            isActive = true;
        }

        // Trim display name
        displayName = displayName.trim();

        // Normalize bio (null if empty string)
        if (bio != null && bio.trim().isEmpty()) {
            bio = null;
        }
    }

    /**
     * Checks if the user has a bio configured.
     *
     * @return true if bio is not null and not empty, false otherwise
     */
    public boolean hasBio() {
        return bio != null && !bio.trim().isEmpty();
    }

    /**
     * Checks if the user is an active community member.
     *
     * <p>A user is considered an active community member if their account is active,
     * email is verified, and they have forum activity.</p>
     *
     * @return true if user is active, verified, and has forum contributions
     */
    public boolean isActiveCommunityMember() {
        return isActive && emailVerified && forumStats.hasActivity();
    }

    /**
     * Gets a short summary of the user's forum activity.
     *
     * <p>Returns a human-readable string describing the user's forum participation,
     * e.g., "15 threads, 87 messages" or "No forum activity yet".</p>
     *
     * @return formatted forum activity summary
     */
    public String getForumActivitySummary() {
        if (!forumStats.hasActivity()) {
            return "No forum activity yet";
        }
        return String.format("%d threads, %d messages", forumStats.threadCount(), forumStats.messageCount());
    }
}
