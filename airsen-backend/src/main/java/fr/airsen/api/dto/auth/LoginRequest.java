package fr.airsen.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Login request DTO for user authentication.
 * 
 * This record represents the data required for user login authentication.
 * It includes email and password with appropriate validation annotations
 * to ensure data integrity and security.
 * 
 * Security Features:
 * - Email validation with proper format checking
 * - Password minimum length requirement
 * - Input sanitization through Bean Validation
 * - Secure toString implementation without password exposure
 */
public record LoginRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    String password
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
     * Secure toString implementation that excludes password.
     * 
     * @return string representation without sensitive information
     */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + ", password=***]";
    }
}