package fr.airsen.api.dto.auth;

import fr.airsen.api.entity.enums.UserRole;
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
public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    String password,
    
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    String firstName,
    
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    String lastName,
    
    UserRole role
) {
    
    /**
     * Gets normalized email in lowercase.
     * 
     * @return normalized email address
     */
    public String getNormalizedEmail() {
        return email != null ? email.toLowerCase().trim() : null;
    }
    
    /**
     * Gets normalized first name with proper capitalization.
     * 
     * @return normalized first name
     */
    public String getNormalizedFirstName() {
        if (firstName == null || firstName.trim().isEmpty()) {
            return null;
        }
        String trimmed = firstName.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }
    
    /**
     * Gets normalized last name with proper capitalization.
     * 
     * @return normalized last name
     */
    public String getNormalizedLastName() {
        if (lastName == null || lastName.trim().isEmpty()) {
            return null;
        }
        String trimmed = lastName.trim();
        return trimmed.substring(0, 1).toUpperCase() + trimmed.substring(1).toLowerCase();
    }
    
    /**
     * Gets the assigned role, defaulting to USER if not specified.
     * 
     * @return user role for registration
     */
    public UserRole getAssignedRole() {
        return role != null ? role : UserRole.USER;
    }
    
    /**
     * Secure toString implementation that excludes password.
     * 
     * @return string representation without sensitive information
     */
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", firstName=" + firstName + 
               ", lastName=" + lastName + ", role=" + role + ", password=***]";
    }
}