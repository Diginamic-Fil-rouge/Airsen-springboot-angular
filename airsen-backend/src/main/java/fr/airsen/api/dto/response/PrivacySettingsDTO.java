package fr.airsen.api.dto.response;

import fr.airsen.api.entity.enums.ProfileVisibility;

/**
 * Data Transfer Object for user privacy settings.
 *
 * <p>This immutable record represents the current privacy settings for an authenticated user.
 * It provides information about the user's chosen profile visibility level and a human-readable
 * description of what that setting means.</p>
 *
 * <p><strong>GDPR Compliance:</strong> This DTO supports GDPR Article 25 (Data Protection by
 * Design and Default) by enabling users to explicitly control their profile visibility.</p>
 *
 * <h3>Privacy Settings:</h3>
 * <table border="1">
 *   <tr>
 *     <th>Visibility Level</th>
 *     <th>Description</th>
 *     <th>Profile Accessible?</th>
 *   </tr>
 *   <tr>
 *     <td>HIDDEN</td>
 *     <td>Profile completely hidden, username only in forum posts</td>
 *     <td>No (404)</td>
 *   </tr>
 *   <tr>
 *     <td>USERNAME_ONLY</td>
 *     <td>Username displayed, no public profile page access (default)</td>
 *     <td>No (404)</td>
 *   </tr>
 *   <tr>
 *     <td>PUBLIC</td>
 *     <td>Full profile accessible with bio, badges, and activity stats</td>
 *     <td>Yes</td>
 *   </tr>
 * </table>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * PrivacySettingsDTO settings = new PrivacySettingsDTO(
 *     ProfileVisibility.PUBLIC,
 *     "Public profile with bio and stats"
 * );
 * }</pre>
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
     * <p>Ensures that required fields are non-null and valid.</p>
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
     * <p>Returns true if the user's profile is publicly accessible.</p>
     *
     * @return true if current visibility is PUBLIC, false otherwise
     */
    public boolean isPublic() {
        return currentVisibility == ProfileVisibility.PUBLIC;
    }

    /**
     * Checks if the current visibility setting is HIDDEN.
     *
     * <p>Returns true if the user's profile is completely hidden.</p>
     *
     * @return true if current visibility is HIDDEN, false otherwise
     */
    public boolean isHidden() {
        return currentVisibility == ProfileVisibility.HIDDEN;
    }

    /**
     * Checks if the current visibility setting is USERNAME_ONLY.
     *
     * <p>Returns true if only the username is visible (default setting).</p>
     *
     * @return true if current visibility is USERNAME_ONLY, false otherwise
     */
    public boolean isUsernameOnly() {
        return currentVisibility == ProfileVisibility.USERNAME_ONLY;
    }
}
