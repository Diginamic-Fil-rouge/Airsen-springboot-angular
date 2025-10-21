package fr.airsen.api.dto.request;

import fr.airsen.api.entity.enums.ProfileVisibility;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating user privacy settings.
 *
 * This immutable record represents a request to update a user's profile visibility setting.
 * It is used by authenticated users to change their privacy preferences (HIDDEN, USERNAME_ONLY, or PUBLIC).
 *
 * GDPR Compliance: This DTO supports GDPR Article 25 (Data Protection by Design and Default)
 * by enabling users to actively control their profile visibility and personal data exposure.
 *
 * Privacy Setting Changes:
 * - From USERNAME_ONLY to PUBLIC: Profile becomes publicly accessible with bio and stats
 * - From PUBLIC to USERNAME_ONLY: Profile page becomes inaccessible (404), but username still shows in forum
 * - From Any to HIDDEN: Maximum privacy - profile hidden, username only in forum
 *
 * Usage Example:
 * UpdatePrivacyRequest request = new UpdatePrivacyRequest(ProfileVisibility.PUBLIC);
 * User wants to make their profile publicly accessible
 *
 * @param profileVisibility the new profile visibility setting (HIDDEN, USERNAME_ONLY, or PUBLIC)
 *
 * @see ProfileVisibility
 * @see fr.airsen.api.service.UserProfileService
 * @since 1.0
 */
public record UpdatePrivacyRequest(
        @NotNull(message = "Profile visibility is required and cannot be null")
        ProfileVisibility profileVisibility
) {
    /**
     * Compact constructor with validation.
     *
     * Ensures that the profile visibility is not null. Note that @NotNull annotation
     * is enforced by Jakarta Bean Validation at the time of request binding.
     *
     * @param profileVisibility the new visibility setting (required, cannot be null)
     * @throws IllegalArgumentException if profile visibility is null
     */
    public UpdatePrivacyRequest {
        // Validation: required field
        if (profileVisibility == null) {
            throw new IllegalArgumentException("Profile visibility cannot be null");
        }
    }
}
