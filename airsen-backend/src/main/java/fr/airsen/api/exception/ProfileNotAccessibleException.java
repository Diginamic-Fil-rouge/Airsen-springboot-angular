package fr.airsen.api.exception;

/**
 * Exception thrown when a user profile cannot be accessed due to privacy settings or account status.
 *
 * This exception is used to implement GDPR-compliant profile access control in AIRSEN.
 * It is thrown when attempting to access a user profile that:
 * - Does not exist (user not found)
 * - Has HIDDEN or USERNAME_ONLY visibility (not PUBLIC)
 * - Belongs to a deleted user (deletedAt != null)
 * - Belongs to an inactive user (isActive = false)
 *
 * HTTP Status Code: This exception should be mapped to 404 Not Found (not 403 Forbidden)
 * to prevent user enumeration attacks. With 404, an attacker cannot distinguish
 * between "user doesn't exist" and "user exists but profile is hidden".
 *
 * GDPR Compliance: This implements Article 25 (Data Protection by Design and Default)
 * by providing granular privacy controls and defaulting to privacy-friendly settings (USERNAME_ONLY).
 *
 * Usage Examples:
 * {@code
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
 * }
 *
 * @see fr.airsen.api.entity.enums.ProfileVisibility
 * @see fr.airsen.api.service.UserProfileService
 * @since 1.0
 */
public class ProfileNotAccessibleException extends RuntimeException {

    /**
     * Constructs a ProfileNotAccessibleException with the specified detail message.
     *
     * The message should describe why the profile is not accessible, but should NOT
     * leak sensitive information about the user's privacy settings or account status.
     *
     * Good messages:
     * - "User profile not found" (generic, doesn't reveal if user exists)
     * - "Profile not accessible" (safe for public API)
     *
     * Bad messages:
     * - "User exists but has HIDDEN visibility" (leaks user existence)
     * - "User account is deleted" (reveals account status)
     *
     * @param message the detail message explaining why profile access was denied
     */
    public ProfileNotAccessibleException(String message) {
        super(message);
    }

    /**
     * Constructs a ProfileNotAccessibleException with the specified detail message and cause.
     *
     * Use this constructor when wrapping another exception (e.g., database errors).
     *
     * @param message the detail message
     * @param cause the underlying cause of the exception
     */
    public ProfileNotAccessibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
