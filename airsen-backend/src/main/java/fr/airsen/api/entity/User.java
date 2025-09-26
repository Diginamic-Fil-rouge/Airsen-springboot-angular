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

/**
 * Entité représentant un utilisateur de l'application Airsen.
 * 
 * Cette entité gère les informations personnelles des utilisateurs,
 * leur authentification et leur rôle dans l'application de surveillance
 * de la qualité de l'air en France.
 */
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
    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "L'adresse email doit être valide")
    @Size(max = 200, message = "L'adresse email ne peut pas dépasser 200 caractères")
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(max = 255, message = "Le mot de passe crypté ne peut pas dépasser 255 caractères")
    private String password;

    @Column(name = "first_name", length = 100)
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    private String firstName;

    @Column(name = "last_name", length = 100)
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String lastName;

    @Column(name = "address", length = 255)
    @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
    private String address;

    @Column(name = "telephone", length = 20)
    @Size(max = 20, message = "Le numéro de téléphone ne peut pas dépasser 20 caractères")
    private String telephone;

    @Column(name = "bio", length = 500)
    @Size(max = 500, message = "La biographie ne peut pas dépasser 500 caractères")
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @NotNull(message = "Le rôle utilisateur est obligatoire")
    private UserRole role;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    /**
     * Indique si le compte utilisateur est actif.
     * 
     * Un compte inactif ne peut pas se connecter à l'application.
     * Les administrateurs peuvent suspendre des comptes en définissant cette valeur à false.
     */
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

    /**
     * Air quality alerts created by this user.
     */
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

    public void updatePassword(String encryptedPassword) {
        if (encryptedPassword != null && !encryptedPassword.trim().isEmpty()) {
            this.password = encryptedPassword;
        }
    }

    public void updateEmail(String newEmail) {
        if (newEmail != null && !newEmail.trim().isEmpty()) {
            this.email = newEmail.trim().toLowerCase();
            this.emailVerified = false; // Réinitialiser la vérification lors du changement d'email
        }
    }

    public boolean isProfileComplete() {
        return firstName != null && !firstName.trim().isEmpty() 
            && lastName != null && !lastName.trim().isEmpty();
    }

    public boolean hasAddress() {
        return address != null && !address.trim().isEmpty();
    }

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