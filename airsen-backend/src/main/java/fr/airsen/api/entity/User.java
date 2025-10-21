package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.ProfileVisibility;
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
    @Index(name = "idx_user_email", columnList = "email", unique = true),
    @Index(name = "idx_user_deleted_at", columnList = "deleted_at")
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

    @Column(name = "telephone", length = 20)
    @Size(max = 20, message = "Telephone cannot exceed 20 characters")
    private String telephone;

    @Column(name = "bio", length = 500)
    @Size(max = 500, message = "Bio cannot exceed 500 characters")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @NotNull(message = "User role is required")
    private UserRole role;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    /**
     * Whether the user account is active.
     *
     * Inactive accounts cannot authenticate or access protected resources.
     * Administrators can suspend accounts by setting this value to false.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Profile visibility setting (GDPR privacy control).
     *
     * Controls how much information other users can see on the profile page:
     * - HIDDEN: Profile page completely hidden (404 response)
     * - USERNAME_ONLY: Only username/full name visible (default)
     * - PUBLIC: Full profile information visible
     *
     * Defaults to USERNAME_ONLY as per GDPR Article 25 (privacy by default).
     * Forum posts always show author name regardless of this setting.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "profile_visibility", nullable = false, length = 50)
    private ProfileVisibility profileVisibility = ProfileVisibility.getDefaultVisibility();

    /**
     * Timestamp when user requested account deletion (GDPR soft delete).
     *
     * When set, the account enters a 30-day grace period before permanent deletion.
     * During this period, the user can log in to cancel the deletion request.
     * After 30 days, a scheduled job permanently deletes the account data.
     *
     * Forum content (threads/messages) is preserved with author name as per
     * GDPR Article 17(3)(e) public interest exception for environmental discussions.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * User-provided reason for account deletion (GDPR transparency).
     *
     * Optional text field capturing why the user requested deletion.
     * Used for internal analytics to improve user retention.
     * Maximum 500 characters.
     */
    @Column(name = "deletion_reason", length = 500)
    @Size(max = 500, message = "Deletion reason cannot exceed 500 characters")
    private String deletionReason;

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

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
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

    public void updateTelephone(String telephone) {
        if (telephone != null && !telephone.trim().isEmpty()) {
            this.telephone = telephone.trim();
        }
    }

    public void updateBio(String bio) {
        if (bio != null && !bio.trim().isEmpty()) {
            this.bio = bio.trim();
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

    public Set<Commune> getFavoris() {
        return favoris;
    }

    public void setFavoris(Set<Commune> favoris) {
        this.favoris = favoris;
    }

    public List<ForumThread> getThreads() {
        return threads;
    }

    public void setThreads(List<ForumThread> threads) {
        this.threads = threads;
    }

    public List<ForumMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ForumMessage> messages) {
        this.messages = messages;
    }

    public List<ForumVote> getVotes() {
        return votes;
    }

    public void setVotes(List<ForumVote> votes) {
        this.votes = votes;
    }

    public List<Alert> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<Alert> alerts) {
        this.alerts = alerts;
    }

    public List<Notification> getSentNotifications() {
        return sentNotifications;
    }

    public void setSentNotifications(List<Notification> sentNotifications) {
        this.sentNotifications = sentNotifications;
    }

    public List<Notification> getReceivedNotifications() {
        return receivedNotifications;
    }

    public void setReceivedNotifications(List<Notification> receivedNotifications) {
        this.receivedNotifications = receivedNotifications;
    }

    public ProfileVisibility getProfileVisibility() {
        return profileVisibility;
    }

    public void setProfileVisibility(ProfileVisibility profileVisibility) {
        this.profileVisibility = profileVisibility;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getDeletionReason() {
        return deletionReason;
    }

    public void setDeletionReason(String deletionReason) {
        this.deletionReason = deletionReason;
    }

    // ==================== GDPR Privacy & Deletion Business Methods ====================

    /**
     * Gets the display name to show in forum posts and UI.
     *
     * Returns the user's full name (first + last) if available, otherwise falls back
     * to email address. This method is used throughout the UI for consistent name display.
     *
     * Note: This method does NOT respect deletion status. For deleted
     * users, use the preserved author name from ForumThread/ForumMessage entities instead.
     *
     * @return User's display name (full name or email)
     */
    public String getDisplayName() {
        return getFullName();
    }

    /**
     * Checks if this user's profile is publicly accessible.
     *
     * @return true if profile visibility is PUBLIC, false otherwise
     */
    public boolean isProfilePublic() {
        return this.profileVisibility == ProfileVisibility.PUBLIC;
    }

    /**
     * Checks if profile link should be displayed in forum and UI.
     *
     * Returns true if profile visibility allows showing a profile link in forum posts
     * or other UI locations. HIDDEN users do not get profile links.
     *
     * Business Rule: Deleted users (deletedAt != null) always return false, regardless
     * of their previous visibility setting. Their profile links are permanently broken
     * after deletion.
     *
     * @return true if profile link should be displayed, false if hidden or deleted
     */
    public boolean hasProfileLink() {
        // Deleted users never have profile links
        if (this.deletedAt != null) {
            return false;
        }
        // Only HIDDEN visibility prevents profile links
        return this.profileVisibility.isProfileAccessible();
    }

    /**
     * Marks this user account for deletion (GDPR soft delete).
     *
     * Initiates a 30-day grace period before permanent deletion. During this period:
     *   - Account is set to inactive (isActive = false)
     *   - User cannot login or access protected resources
     *   - User can restore account by calling {@link #cancelDeletion()}
     *   - Deletion timestamp is recorded for grace period calculation
     *
     * After 30 days, a scheduled job will permanently delete the account while preserving
     * forum content per GDPR Article 17(3)(e) public interest exception.
     *
     * @param reason Optional reason for deletion (for analytics), can be null
     */
    public void markForDeletion(String reason) {
        this.deletedAt = LocalDateTime.now();
        this.deletionReason = reason;
        this.isActive = false;  // Prevent login during grace period
    }

    /**
     * Cancels a pending deletion request (restores account).
     *
     * Can only be called during the 30-day grace period. Clears the deletion timestamp
     * and reason, and reactivates the account for login.
     *
     * Business Rule: If called after the grace period has expired,
     * this method still restores the account (scheduled job may not have run yet). However,
     * once the permanent deletion job has executed, this method has no effect as the User
     * entity will no longer exist in the database.
     */
    public void cancelDeletion() {
        this.deletedAt = null;
        this.deletionReason = null;
        this.isActive = true;  // Reactivate account
    }

    /**
     * Checks if the 30-day deletion grace period has expired.
     *
     * Returns true if this user is marked for deletion (deletedAt != null) and more
     * than 30 days have passed since the deletion request. This method is used by the
     * scheduled deletion job to identify accounts ready for permanent deletion.
     *
     * Grace Period Logic:
     *   If deletedAt is null → false (not marked for deletion)
     *   If deletedAt + 30 days > now → false (still in grace period)
     *   If deletedAt + 30 days <= now → true (ready for permanent deletion)
     *
     * @return true if grace period has expired, false if still within grace period or not deleted
     */
    public boolean isDeletionGracePeriodExpired() {
        if (this.deletedAt == null) {
            return false;
        }
        LocalDateTime gracePeriodEnd = this.deletedAt.plusDays(30);
        return LocalDateTime.now().isAfter(gracePeriodEnd);
    }

    /**
     * Checks if this user is marked for deletion (soft deleted).
     *
     * Returns true if the user has requested account deletion (deletedAt != null),
     * regardless of whether the grace period has expired. This is used to prevent
     * deleted users from accessing certain features during the grace period.
     *
     * @return true if user is marked for deletion, false otherwise
     */
    public boolean isMarkedForDeletion() {
        return this.deletedAt != null;
    }

    /**
     * Permanently deletes all personal data from this user account (GDPR compliance).
     *
     * This method is called by the scheduled deletion job after the 30-day grace period.
     * It removes all personal information while keeping the User entity for forum author
     * preservation (GDPR Article 17(3)(e) public interest exception).
     *
     * Data Retention Strategy:
     *   Deleted: email, password, firstName, lastName, address, telephone, bio, favoris
     *            (all personal data)
     *   Preserved: id (for foreign key integrity), createdAt (for historical context),
     *              role (for authorization checks)
     *   Updated: isActive = false, profileVisibility = HIDDEN
     *
     * Forum Content Preservation: Before calling this method, the UserDeletionService must
     * have already copied the user's display name to the authorName field in all
     * ForumThread and ForumMessage entities. This preserves discussion context while
     * anonymizing the author.
     *
     * Note: This method does NOT delete the User entity itself, as it must remain for
     * foreign key integrity with forum content. The user becomes a "deleted user stub"
     * with no personal data.
     */
    public void permanentlyDeletePersonalData() {
        // Clear all personal information
        this.email = "deleted_user_" + this.id + "@deleted.local";  // Unique placeholder to avoid unique constraint violations
        this.password = "";  // Clear password hash
        this.firstName = null;
        this.lastName = null;
        this.address = null;
        this.telephone = null;
        this.bio = null;

        // Clear relationships with personal data
        this.favoris.clear();

        // Ensure account is inactive and hidden
        this.isActive = false;
        this.profileVisibility = ProfileVisibility.HIDDEN;
    }

    // ==================== End GDPR Business Methods ====================

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", email='" + email + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", address='" + address + '\'' +
                ", telephone='" + telephone + '\'' +
                ", bio='" + bio + '\'' +
                ", role=" + role +
                ", emailVerified=" + emailVerified +
                ", isActive=" + isActive +
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
