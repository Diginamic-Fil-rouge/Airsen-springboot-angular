package fr.airsen.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for user login requests.
 * 
 * This DTO encapsulates the credentials required for user authentication
 * in the Airsen application. It provides validation to ensure proper
 * format and security requirements are met before authentication processing.
 * 
 * Security Considerations:
 * - Email is case-insensitive and normalized during processing
 * - Password is never logged or exposed in string representations
 * - Input validation prevents malformed authentication attempts
 * - Rate limiting should be applied at the controller level
 */
public class LoginRequest {

    /**
     * User's email address used as username for authentication.
     * 
     * Must be a valid email format and correspond to an existing user account.
     * Email is case-insensitive and will be normalized to lowercase during
     * authentication processing.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 200, message = "Email cannot exceed 200 characters")
    private String email;

    /**
     * User's password for authentication.
     * 
     * Must be provided and will be compared against the hashed password
     * stored in the database using BCrypt secure comparison.
     */
    @NotBlank(message = "Password is required")
    @Size(max = 100, message = "Password cannot exceed 100 characters")
    private String password;

    /**
     * Default constructor for JSON deserialization.
     */
    public LoginRequest() {
    }

    /**
     * Constructor with email and password.
     * 
     * @param email user's email address
     * @param password user's password
     */
    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     * Gets the user's email address.
     * 
     * @return email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * Sets the user's email address.
     * 
     * @param email email address to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Gets the user's password.
     * 
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the user's password.
     * 
     * @param password password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns a string representation of this login request.
     * 
     * Excludes password for security purposes, showing only email
     * and indicating that password is protected.
     * 
     * @return string representation of the request
     */
    @Override
    public String toString() {
        return "LoginRequest{" +
                "email='" + email + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }

    /**
     * Normalizes the email address to lowercase for consistent authentication.
     * 
     * @return normalized email address
     */
    public String getNormalizedEmail() {
        return email != null ? email.trim().toLowerCase() : null;
    }

    /**
     * Validates that both required fields are present and not blank.
     * 
     * @return true if email and password are both provided
     */
    public boolean hasValidCredentials() {
        return email != null && !email.trim().isEmpty() &&
               password != null && !password.trim().isEmpty();
    }

    /**
     * Checks if the email format appears valid without full validation.
     * 
     * This is a basic check that can be used before more expensive validation.
     * 
     * @return true if email contains @ symbol and basic format
     */
    public boolean hasBasicEmailFormat() {
        return email != null && email.contains("@") && email.length() > 3;
    }

    /**
     * Gets the domain part of the email address.
     * 
     * Useful for logging and analytics without exposing full email.
     * 
     * @return domain part of email or null if invalid
     */
    public String getEmailDomain() {
        if (email == null || !email.contains("@")) {
            return null;
        }
        String[] parts = email.split("@");
        return parts.length == 2 ? parts[1].toLowerCase() : null;
    }

    /**
     * Creates a sanitized version for logging purposes.
     * 
     * Shows only the domain part of the email for security.
     * 
     * @return sanitized string for logging
     */
    public String toLogString() {
        String domain = getEmailDomain();
        return "LoginRequest{" +
                "emailDomain='" + (domain != null ? domain : "[INVALID]") + '\'' +
                ", hasPassword=" + (password != null && !password.isEmpty()) +
                '}';
    }
}