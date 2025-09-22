package fr.airsen.api.dto.auth;

import fr.airsen.api.entity.enums.UserRole;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Data Transfer Object for user information in authentication responses.
 * 
 * This DTO represents user data that is safe to expose in API responses,
 * excluding sensitive information like passwords or internal system data.
 * It's used in authentication responses and user profile operations.
 * 
 * Security Considerations:
 * - Excludes password and other sensitive user data
 * - Only includes information needed for frontend functionality
 * - Provides role information for authorization decisions
 * - Uses consistent field naming with API specification
 */
public class UserDTO {

    /**
     * User's unique identifier.
     */
    private Long id;

    /**
     * User's email address (username).
     */
    private String email;

    /**
     * User's first name for display purposes.
     */
    private String firstName;

    /**
     * User's last name for display purposes.
     */
    private String lastName;

    /**
     * User's address for display purposes.
     */
    private String address;

    /**
     * User's role in the system for authorization.
     */
    private UserRole role;

    /**
     * Whether the user account is active.
     * 
     * Note: This maps to the API specification's "isActive" field.
     * Inactive users cannot authenticate or access protected resources.
     */
    private Boolean isActive;

    /**
     * When the user account was created.
     * 
     * Formatted as ISO 8601 datetime string in JSON responses.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdAt;

    /**
     * When the user last logged in.
     * 
     * Used for security monitoring and user engagement tracking.
     * May be null if user has never logged in.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime lastLogin;

    /**
     * Whether the user's email address has been verified.
     * 
     * Used for email verification workflows and security features.
     */
    private Boolean isEmailVerified;

    /**
     * Default constructor for JSON serialization.
     */
    public UserDTO() {
    }

    /**
     * Constructor with essential user information.
     * 
     * @param id user's unique identifier
     * @param email user's email address
     * @param firstName user's first name
     * @param lastName user's last name
     * @param role user's role
     */
    public UserDTO(Long id, String email, String firstName, String lastName, String address, UserRole role) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.role = role;
        this.isActive = true; // Default to active
        this.isEmailVerified = false; // Default to unverified
    }

    /**
     * Full constructor with all fields.
     * 
     * @param id user's unique identifier
     * @param email user's email address
     * @param firstName user's first name
     * @param lastName user's last name
     * @param role user's role
     * @param isActive whether account is active
     * @param createdAt when account was created
     * @param lastLogin when user last logged in
     * @param isEmailVerified whether email is verified
     */
    public UserDTO(Long id, String email, String firstName, String lastName,
                   String address,
                   UserRole role, Boolean isActive, LocalDateTime createdAt, 
                   LocalDateTime lastLogin, Boolean isEmailVerified) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.address = address;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.isEmailVerified = isEmailVerified;
    }

    // Getters and Setters

    /**
     * Gets the user's unique identifier.
     * 
     * @return user ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the user's unique identifier.
     * 
     * @param id user ID to set
     */
    public void setId(Long id) {
        this.id = id;
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
     * Gets the user's role.
     * 
     * @return user role
     */
    public UserRole getRole() {
        return role;
    }

    /**
     * Sets the user's role.
     * 
     * @param role user role to set
     */
    public void setRole(UserRole role) {
        this.role = role;
    }

    /**
     * Gets whether the user account is active.
     * 
     * @return true if account is active
     */
    public Boolean getIsActive() {
        return isActive;
    }

    /**
     * Sets whether the user account is active.
     * 
     * @param isActive account status to set
     */
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    /**
     * Gets when the user account was created.
     * 
     * @return creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets when the user account was created.
     * 
     * @param createdAt creation timestamp to set
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets when the user last logged in.
     * 
     * @return last login timestamp
     */
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    /**
     * Sets when the user last logged in.
     * 
     * @param lastLogin last login timestamp to set
     */
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * Gets whether the user's email is verified.
     * 
     * @return true if email is verified
     */
    public Boolean getIsEmailVerified() {
        return isEmailVerified;
    }

    /**
     * Sets whether the user's email is verified.
     * 
     * @param isEmailVerified email verification status to set
     */
    public void setIsEmailVerified(Boolean isEmailVerified) {
        this.isEmailVerified = isEmailVerified;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * Gets the user's full display name.
     * 
     * Combines first and last name for display purposes.
     * 
     * @return full display name
     */
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return email; // Fallback to email if no names
        }
        
        StringBuilder fullName = new StringBuilder();
        if (firstName != null) {
            fullName.append(firstName);
        }
        if (lastName != null) {
            if (fullName.length() > 0) {
                fullName.append(" ");
            }
            fullName.append(lastName);
        }
        
        return fullName.toString();
    }

    /**
     * Gets the user's display initials.
     * 
     * Uses first letters of first and last name, falling back to email.
     * 
     * @return user initials (e.g., "JD" for John Doe)
     */
    public String getInitials() {
        StringBuilder initials = new StringBuilder();
        
        if (firstName != null && !firstName.trim().isEmpty()) {
            initials.append(firstName.trim().toUpperCase().charAt(0));
        }
        
        if (lastName != null && !lastName.trim().isEmpty()) {
            initials.append(lastName.trim().toUpperCase().charAt(0));
        }
        
        if (initials.length() == 0 && email != null && !email.trim().isEmpty()) {
            initials.append(email.toUpperCase().charAt(0));
        }
        
        return initials.toString();
    }

    /**
     * Checks if the user has administrator privileges.
     * 
     * @return true if user is an administrator
     */
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * Checks if the user is a logged-in user (not visitor).
     * 
     * @return true if user is a logged-in user or administrator
     */
    public boolean isLoggedUser() {
        return role == UserRole.USER || role == UserRole.ADMIN;
    }

    /**
     * Returns a string representation of this user DTO.
     * 
     * Includes all non-sensitive information for debugging purposes.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return "UserDTO{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", role=" + role +
                ", isActive=" + isActive +
                ", createdAt=" + createdAt +
                ", lastLogin=" + lastLogin +
                ", isEmailVerified=" + isEmailVerified +
                '}';
    }
}