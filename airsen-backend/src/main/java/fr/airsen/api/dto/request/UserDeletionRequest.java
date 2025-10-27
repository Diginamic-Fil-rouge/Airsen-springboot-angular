package fr.airsen.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user deletion requests (admin operation).
 *
 * This immutable record represents an admin request to delete a user account. It is used by
 * administrators to initiate the GDPR-compliant user deletion process with a 30-day grace period.
 *
 * GDPR Compliance: This DTO supports GDPR Article 17 (Right to Erasure) by
 * requiring administrators to provide a documented reason for deletion, creating an audit trail
 * for compliance documentation.
 *
 * Deletion Process:
 * 1. Soft Delete (Day 0): User account marked for deletion, becomes inactive,
 *    deletion reason recorded
 * 2. Grace Period (Days 1-30): 30-day window for deletion cancellation or user
 *    recovery requests
 * 3. Hard Delete (Day 30+): Permanent deletion executed:
 *    - User record deleted
 *    - Forum author names preserved (public interest exception)
 *    - Alerts, favorites, profile data deleted
 *    - Votes and notifications anonymized
 *
 * Valid Deletion Reasons:
 * - GDPR deletion request from user
 * - Account violation - spam activity
 * - Account violation - harassment/abusive behavior
 * - Account violation - inappropriate content
 * - User request - privacy concerns
 * - Inactivity - no account activity for 12+ months
 * - Administrative action - policy violation
 *
 * Usage Example:
 * UserDeletionRequest request = new UserDeletionRequest(
 *     "GDPR deletion request from user - received via support ticket #12345"
 * );
 * Admin DELETE /api/v1/admin/users/456 with this request body
 *
 * @param reason documented reason for user deletion (10-500 characters, required)
 *
 * @see fr.airsen.api.service.UserDeletionService
 * @since 1.0
 */
public record UserDeletionRequest(
        @NotBlank(message = "Deletion reason is required and cannot be blank")
        @Size(min = 10, max = 500, message = "Deletion reason must be between 10 and 500 characters")
        String reason
) {
    /**
     * Compact constructor with validation and normalization.
     *
     * Ensures that the deletion reason meets minimum requirements and is properly normalized
     * for storage in the audit trail. Note that @NotBlank and @Size annotations are enforced by
     * Jakarta Bean Validation at the time of request binding.
     *
     * @param reason the documented reason for deletion (required, must be 10-500 characters)
     * @throws IllegalArgumentException if reason is null, empty, or outside size limits
     */
    public UserDeletionRequest {
        // Validation: required field
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Deletion reason cannot be null or empty");
        }

        // Validate size
        if (reason.trim().length() < 10) {
            throw new IllegalArgumentException("Deletion reason must be at least 10 characters");
        }
        if (reason.trim().length() > 500) {
            throw new IllegalArgumentException("Deletion reason must not exceed 500 characters");
        }

        // Trim whitespace
        reason = reason.trim();
    }

    /**
     * Checks if this deletion reason indicates a GDPR compliance deletion.
     *
     * Returns true if the reason contains keywords suggesting a user-requested deletion
     * under GDPR Article 17.
     *
     * @return true if reason suggests GDPR right to erasure, false otherwise
     */
    public boolean isGdprDeletion() {
        return reason.toLowerCase().contains("gdpr") ||
               reason.toLowerCase().contains("erasure") ||
               reason.toLowerCase().contains("user request") ||
               reason.toLowerCase().contains("right to be forgotten");
    }

    /**
     * Checks if this deletion reason indicates a policy violation.
     *
     * Returns true if the reason contains keywords suggesting account violation or abuse.
     *
     * @return true if reason suggests policy violation, false otherwise
     */
    public boolean isViolationDeletion() {
        return reason.toLowerCase().contains("violation") ||
               reason.toLowerCase().contains("spam") ||
               reason.toLowerCase().contains("abuse") ||
               reason.toLowerCase().contains("inappropriate") ||
               reason.toLowerCase().contains("harassment");
    }

    /**
     * Gets a short summary of the deletion reason (first 50 characters).
     *
     * Useful for logs and audit summaries where full reason text would be too verbose.
     *
     * @return first 50 characters of the reason, or full reason if shorter
     */
    public String getSummary() {
        if (reason.length() <= 50) {
            return reason;
        }
        return reason.substring(0, 50) + "...";
    }
}
