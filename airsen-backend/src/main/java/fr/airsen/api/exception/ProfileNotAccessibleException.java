package fr.airsen.api.exception;

/**
 * Exception thrown when a user profile cannot be accessed due to privacy settings or account status.
 *
 * <p>This exception is used to implement GDPR-compliant profile access control in AIRSEN.
 * It is thrown when attempting to access a user profile that:</p>
 * <ul>
 *   <li>Does not exist (user not found)</li>
 *   <li>Has HIDDEN or USERNAME_ONLY visibility (not PUBLIC)</li>
 *   <li>Belongs to a deleted user (deletedAt != null)</li>
 *   <li>Belongs to an inactive user (isActive = false)</li>
 * </ul>
 *
 * <p><strong>HTTP Status Code:</strong> This exception should be mapped to <strong>404 Not Found</strong>
 * (not 403 Forbidden) to prevent user enumeration attacks. With 404, an attacker cannot distinguish
 * between "user doesn't exist" and "user exists but profile is hidden".</p>
 *
 * <p><strong>GDPR Compliance:</strong> This implements Article 25 (Data Protection by Design and Default)
 * by providing granular privacy controls and defaulting to privacy-friendly settings (USERNAME_ONLY).</p>
 *
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // User not found
 * throw new ProfileNotAccessibleException("User profile not found with ID: " + userId);
 *
 * // Profile not public
 * throw new ProfileNotAccessibleException("User profile is not publicly accessible");
 *
 * // User deleted
 * throw new ProfileNotAccessibleException("User account has been deleted");
 *
 * // User inactive
 * throw new ProfileNotAccessibleException("User account is inactive");
 * }</pre>
 *
 * @see fr.airsen.api.entity.enums.ProfileVisibility
 * @see fr.airsen.api.service.UserProfileService
 * @since 1.0
 */
public class ProfileNotAccessibleException extends RuntimeException {

    /**
     * Constructs a ProfileNotAccessibleException with the specified detail message.
     *
     * <p>The message should describe why the profile is not accessible, but should NOT
     * leak sensitive information about the user's privacy settings or account status.</p>
     *
     * <p><strong>Good messages:</strong></p>
     * <ul>
     *   <li>"User profile not found" (generic, doesn't reveal if user exists)</li>
     *   <li>"Profile not accessible" (safe for public API)</li>
     * </ul>
     *
     * <p><strong>Bad messages:</strong></p>
     * <ul>
     *   <li>"User exists but has HIDDEN visibility" (leaks user existence)</li>
     *   <li>"User account is deleted" (reveals account status)</li>
     * </ul>
     *
     * @param message the detail message explaining why profile access was denied
     */
    public ProfileNotAccessibleException(String message) {
        super(message);
    }

    /**
     * Constructs a ProfileNotAccessibleException with the specified detail message and cause.
     *
     * <p>Use this constructor when wrapping another exception (e.g., database errors).</p>
     *
     * @param message the detail message
     * @param cause the underlying cause of the exception
     */
    public ProfileNotAccessibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
