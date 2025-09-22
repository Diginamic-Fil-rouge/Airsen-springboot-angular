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

    private Long id;

    private String email;

    private String firstName;

    private String lastName;

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

    public UserDTO() {
    }

    public UserDTO(Long id, String email, String firstName, String lastName, UserRole role) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.isActive = true; // Default to active
        this.isEmailVerified = false; // Default to unverified
    }

    public UserDTO(Long id, String email, String firstName, String lastName, 
                   UserRole role, Boolean isActive, LocalDateTime createdAt, 
                   LocalDateTime lastLogin, Boolean isEmailVerified) {
        this.id = id;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.lastLogin = lastLogin;
        this.isEmailVerified = isEmailVerified;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    public Boolean getIsEmailVerified() {
        return isEmailVerified;
    }

    public void setIsEmailVerified(Boolean isEmailVerified) {
        this.isEmailVerified = isEmailVerified;
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

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isLoggedUser() {
        return role == UserRole.USER || role == UserRole.ADMIN;
    }

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