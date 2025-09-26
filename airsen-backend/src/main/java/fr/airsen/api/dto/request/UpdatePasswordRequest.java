package fr.airsen.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating user password.
 * 
 * This DTO represents the data required to change a user's password.
 * It includes both the current password for verification and the new password.
 * 
 * Validation:
 * - Current password is required for security verification
 * - New password must meet minimum security requirements
 * - Passwords must be between 8 and 100 characters
 * 
 * Security Considerations:
 * - Current password validation prevents unauthorized changes
 * - New password requirements enforce security standards
 * - Passwords are never returned in responses
 * - This operation requires authentication
 */
public class UpdatePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String newPassword;

    /**
     * Default constructor for JSON deserialization.
     */
    public UpdatePasswordRequest() {
    }

    public UpdatePasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "UpdatePasswordRequest{" +
                "currentPassword='[PROTECTED]'" +
                ", newPassword='[PROTECTED]'" +
                '}';
    }
}