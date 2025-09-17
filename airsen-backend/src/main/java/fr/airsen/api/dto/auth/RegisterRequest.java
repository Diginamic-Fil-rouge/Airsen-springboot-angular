package fr.airsen.api.dto.auth;

import jakarta.validation.constraints.*;

/**
 * Data Transfer Object for user registration requests.
 * 
 * This DTO encapsulates all the information required to create a new user account
 * in the Airsen application. It includes comprehensive validation annotations to
 * ensure data integrity and security requirements are met before processing.
 * 
 * Validation Rules:
 * - Email must be valid format and unique in the system
 * - Password must meet security requirements (8+ chars, mixed case, digits)
 * - First and last names must be provided and within reasonable length limits
 * - All fields are required and cannot be blank
 * 
 * Security Considerations:
 * - Email is normalized to lowercase during processing
 * - Password will be hashed using BCrypt before storage
 * - Input sanitization is performed to prevent injection attacks
 */
public class RegisterRequest {

    /**
     * User's email address used for authentication and notifications.
     * 
     * Must be a valid email format and will be used as the unique username
     * for authentication. Email is case-insensitive and will be normalized
     * to lowercase during processing.
     */
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 200, message = "Email cannot exceed 200 characters")
    private String email;

    /**
     * User's password for authentication.
     * 
     * Must meet security requirements:
     * - Minimum 8 characters
     * - At least one uppercase letter
     * - At least one lowercase letter
     * - At least one digit
     * - Maximum 100 characters for practical limits
     */
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
    )
    private String password;

    /**
     * User's first name for profile and display purposes.
     * 
     * Used for personalization and communication with the user.
     * Must be provided and cannot be blank.
     */
    @NotBlank(message = "First name is required")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    @Pattern(
        regexp = "^[\\p{L}\\s'-]+$",
        message = "First name can only contain letters, spaces, hyphens, and apostrophes"
    )
    private String firstName;

    /**
     * User's last name for profile and display purposes.
     * 
     * Used for personalization and communication with the user.
     * Must be provided and cannot be blank.
     */
    @NotBlank(message = "Last name is required")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    @Pattern(
        regexp = "^[\\p{L}\\s'-]+$",
        message = "Last name can only contain letters, spaces, hyphens, and apostrophes"
    )
    private String lastName;

    /**
     * Default constructor for JSON deserialization.
     */
    public RegisterRequest() {
    }

    /**
     * Constructor with all required fields.
     * 
     * @param email user's email address
     * @param password user's password
     * @param firstName user's first name
     * @param lastName user's last name
     */
    public RegisterRequest(String email, String password, String firstName, String lastName) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
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
     * Gets the user's first name.
     * 
     * @return first name
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Sets the user's first name.
     * 
     * @param firstName first name to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Gets the user's last name.
     * 
     * @return last name
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Sets the user's last name.
     * 
     * @param lastName last name to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Returns a string representation of this registration request.
     * 
     * Excludes sensitive information like passwords for security.
     * 
     * @return string representation of the request
     */
    @Override
    public String toString() {
        return "RegisterRequest{" +
                "email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", password='[PROTECTED]'" +
                '}';
    }

    /**
     * Validates the password strength beyond basic annotations.
     * 
     * This method can be called for additional password validation
     * that cannot be expressed through annotations alone.
     * 
     * @return true if password meets all security requirements
     */
    public boolean isPasswordSecure() {
        if (password == null || password.length() < 8) {
            return false;
        }
        
        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        
        return hasUpper && hasLower && hasDigit;
    }

    /**
     * Normalizes the email address to lowercase for consistent storage.
     * 
     * @return normalized email address
     */
    public String getNormalizedEmail() {
        return email != null ? email.trim().toLowerCase() : null;
    }

    /**
     * Validates that all required fields are present and not blank.
     * 
     * @return true if all required fields are valid
     */
    public boolean hasValidRequiredFields() {
        return email != null && !email.trim().isEmpty() &&
               password != null && !password.trim().isEmpty() &&
               firstName != null && !firstName.trim().isEmpty() &&
               lastName != null && !lastName.trim().isEmpty();
    }
}