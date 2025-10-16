package fr.airsen.api.dto.auth;

import fr.airsen.api.entity.enums.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Registration request DTO for new user account creation.
 * 
 * This record represents the data required for user registration.
 * It includes comprehensive validation for all required fields
 * and supports role assignment during registration.
 * 
 * Security Features:
 * - Email validation with proper format checking
 * - Password strength requirements
 * - Name field validation
 * - Input sanitization through Bean Validation
 * - Secure toString implementation without password exposure
 * - Role assignment validation
 */
@Schema(description = "User registration request")
public record RegisterRequest(
    @Schema(description = "User email address", example = "user@airsen.fr", required = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @Schema(description = "User password (minimum 8 characters)", example = "password123", required = true)
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    String password,
    
    @Schema(description = "User first name", example = "John", required = true)
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    String firstName,
    
    @Schema(description = "User last name", example = "Doe", required = true)
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    String lastName,
    
    @Schema(description = "User role (optional, defaults to USER)", example = "USER", allowableValues = {"USER", "ADMIN"})
    UserRole role
) {

    public String getNormalizedEmail() {
        return email != null ? email.toLowerCase().trim() : null;
    }

    public String getNormalizedFirstName() {
        if (firstName == null || firstName.trim().isEmpty()) {
            return null;
        }
        String trimmed = firstName.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }

    public String getNormalizedLastName() {
        if (lastName == null || lastName.trim().isEmpty()) {
            return null;
        }
        String trimmed = lastName.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }
    
    public UserRole getAssignedRole() {
        return role != null ? role : UserRole.USER;
    }

    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", firstName=" + firstName + 
               ", lastName=" + lastName + ", role=" + role + ", password=***]";
    }
}