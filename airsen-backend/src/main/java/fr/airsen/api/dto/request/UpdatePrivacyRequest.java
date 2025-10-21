package fr.airsen.api.dto.request;

import fr.airsen.api.entity.enums.ProfileVisibility;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating user privacy settings.
 *
 * <p>This immutable record represents a request to update a user's profile visibility setting.
 * It is used by authenticated users to change their privacy preferences (HIDDEN, USERNAME_ONLY, or PUBLIC).</p>
 *
 * <p><strong>GDPR Compliance:</strong> This DTO supports GDPR Article 25 (Data Protection by Design and Default)
 * by enabling users to actively control their profile visibility and personal data exposure.</p>
 *
 * <h3>Privacy Setting Changes:</h3>
 * <table border="1">
 *   <tr>
 *     <th>From</th>
 *     <th>To</th>
 *     <th>Effect</th>
 *   </tr>
 *   <tr>
 *     <td>USERNAME_ONLY (default)</td>
 *     <td>PUBLIC</td>
 *     <td>Profile becomes publicly accessible with bio and stats</td>
 *   </tr>
 *   <tr>
 *     <td>PUBLIC</td>
 *     <td>USERNAME_ONLY</td>
 *     <td>Profile page becomes inaccessible (404), but username still shows in forum</td>
 *   </tr>
 *   <tr>
 *     <td>Any</td>
 *     <td>HIDDEN</td>
 *     <td>Maximum privacy: profile hidden, username only in forum</td>
 *   </tr>
 * </table>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * UpdatePrivacyRequest request = new UpdatePrivacyRequest(ProfileVisibility.PUBLIC);
 * // User wants to make their profile publicly accessible
 * }</pre>
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
     * <p>Ensures that the profile visibility is not null. Note that @NotNull annotation
     * is enforced by Jakarta Bean Validation at the time of request binding.</p>
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
