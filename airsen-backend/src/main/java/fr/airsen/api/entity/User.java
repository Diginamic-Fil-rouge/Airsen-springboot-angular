package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Set;


@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email", unique = true)
})
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 200)
    @NotBlank(message = "Email address is required")
    @Email(message = "Email address must be valid")
    @Size(max = 200, message = "Email address cannot exceed 200 characters")
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    @NotBlank(message = "Password is required")
    @Size(max = 255, message = "Encrypted password cannot exceed 255 characters")
    private String password;

    @Column(name = "first_name", length = 100)
    @Size(max = 100, message = "First name cannot exceed 100 characters")
    private String firstName;

    @Column(name = "last_name", length = 100)
    @Size(max = 100, message = "Last name cannot exceed 100 characters")
    private String lastName;

    @Column(name = "address", length = 255)
    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @NotNull(message = "User role is required")
    private UserRole role;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToMany
    @JoinTable(name = "user_favorite", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "commune_id"))
    private Set<Commune> favoris = new HashSet<>();


    @OneToMany(mappedBy = "author")
    private List<ForumThread> threads;

    @OneToMany(mappedBy = "author")
    private List<ForumMessage> messages;

    @OneToMany(mappedBy = "user")
    private List<ForumVote> votes;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Alert> alerts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> sentNotifications;

    @OneToMany(mappedBy = "userReceiver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> receivedNotifications;

    public User() {
        this.role = UserRole.getDefaultRole();
        this.emailVerified = false;
        this.isActive = true;
    }

    public User(String email, String password, String firstName, String lastName) {
        this();
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public boolean hasRole(UserRole role) {
        return this.role == role;
    }


    public void verifyEmail() {
        this.emailVerified = true;
    }

    /**
     * Updates user profile with new information.
     * Only updates non-null and non-empty values.
     * 
     * @param firstName new first name (can be null)
     * @param lastName new last name (can be null) 
     * @param address new address (can be null)
     */
    public void updateProfile(String firstName, String lastName, String address) {
        if (firstName != null && !firstName.trim().isEmpty()) {
            this.firstName = firstName.trim();
        }
        if (lastName != null && !lastName.trim().isEmpty()) {
            this.lastName = lastName.trim();
        }
        if (address != null && !address.trim().isEmpty()) {
            this.address = address.trim();
        }
    }

    public void updateFirstName(String firstName) {
        if (firstName != null && !firstName.trim().isEmpty()) {
            this.firstName = firstName.trim();
        }
    }

    public void updateLastName(String lastName) {
        if (lastName != null && !lastName.trim().isEmpty()) {
            this.lastName = lastName.trim();
        }
    }

    public void updateAddress(String address) {
        if (address != null && !address.trim().isEmpty()) {
            this.address = address.trim();
        }
    }

    /**
     * Updates user password.
     * Note: Password must be already encrypted before calling this method.
     * 
     * @param encryptedPassword new encrypted password
     */
    public void updatePassword(String encryptedPassword) {
        if (encryptedPassword != null && !encryptedPassword.trim().isEmpty()) {
            this.password = encryptedPassword;
        }
    }

    /**
     * Updates user email and resets verification status.
     * 
     * @param newEmail new email address
     */
    public void updateEmail(String newEmail) {
        if (newEmail != null && !newEmail.trim().isEmpty()) {
            this.email = newEmail.trim().toLowerCase();
            this.emailVerified = false; // Reset verification when email changes
        }
    }

    /**
     * Checks if user profile is complete.
     * A profile is considered complete if both first name and last name are provided.
     * 
     * @return true if profile is complete
     */
    public boolean isProfileComplete() {
        return firstName != null && !firstName.trim().isEmpty() 
            && lastName != null && !lastName.trim().isEmpty();
    }

    public boolean hasAddress() {
        return address != null && !address.trim().isEmpty();
    }

    /**
     * Gets user's full name.
     * 
     * @return full name (first + last) or email if names are not provided
     */
    public String getFullName() {
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        } else if (firstName != null) {
            return firstName;
        } else if (lastName != null) {
            return lastName;
        } else {
            return email;
        }
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", role=" + role +
                ", emailVerified=" + emailVerified +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        User user = (User) o;
        return id != null && id.equals(user.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}