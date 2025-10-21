package fr.airsen.api.dto.response;

import fr.airsen.api.entity.enums.ProfileVisibility;

/**
 * Data Transfer Object for user privacy settings.
 *
 * This immutable record represents the current privacy settings for an authenticated user.
 * It provides information about the user's chosen profile visibility level and a human-readable
 * description of what that setting means.
 *
 * GDPR Compliance: This DTO supports GDPR Article 25 (Data Protection by
 * Design and Default) by enabling users to explicitly control their profile visibility.
 *
 * Privacy Settings:
 * - HIDDEN: Profile completely hidden, username only in forum posts. Profile Accessible: No (404)
 * - USERNAME_ONLY: Username displayed, no public profile page access (default). Profile Accessible: No (404)
 * - PUBLIC: Full profile accessible with bio, badges, and activity stats. Profile Accessible: Yes
 *
 * Usage Example:
 * PrivacySettingsDTO settings = new PrivacySettingsDTO(
 *     ProfileVisibility.PUBLIC,
 *     "Public profile with bio and stats"
 * );
 *
 * @param currentVisibility the user's current profile visibility setting (HIDDEN, USERNAME_ONLY, or PUBLIC)
 * @param description human-readable explanation of the current visibility setting
 *
 * @see ProfileVisibility
 * @see fr.airsen.api.service.UserProfileService
 * @since 1.0
 */
public record PrivacySettingsDTO(
        ProfileVisibility currentVisibility,
        String description
) {
    /**
     * Compact constructor with validation.
     *
     * Ensures that required fields are non-null and valid.
     *
     * @param currentVisibility the profile visibility setting (required, cannot be null)
     * @param description human-readable explanation (required, cannot be null or empty)
     * @throws IllegalArgumentException if required fields are null or empty
     */
    public PrivacySettingsDTO {
        // Validation: required fields
        if (currentVisibility == null) {
            throw new IllegalArgumentException("Profile visibility cannot be null");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("Description cannot be null or empty");
        }

        // Trim description
        description = description.trim();
    }

    /**
     * Checks if the current visibility setting is PUBLIC.
     *
     * Returns true if the user's profile is publicly accessible.
     *
     * @return true if current visibility is PUBLIC, false otherwise
     */
    public boolean isPublic() {
        return currentVisibility == ProfileVisibility.PUBLIC;
    }

    /**
     * Checks if the current visibility setting is HIDDEN.
     *
     * Returns true if the user's profile is completely hidden.
     *
     * @return true if current visibility is HIDDEN, false otherwise
     */
    public boolean isHidden() {
        return currentVisibility == ProfileVisibility.HIDDEN;
    }

    /**
     * Checks if the current visibility setting is USERNAME_ONLY.
     *
     * Returns true if only the username is visible (default setting).
     *
     * @return true if current visibility is USERNAME_ONLY, false otherwise
     */
    public boolean isUsernameOnly() {
        return currentVisibility == ProfileVisibility.USERNAME_ONLY;
    }
}
