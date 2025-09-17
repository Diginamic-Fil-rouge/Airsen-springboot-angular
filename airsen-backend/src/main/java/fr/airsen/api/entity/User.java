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

    /**
     * Identifiant unique de l'utilisateur.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Adresse email de l'utilisateur, utilisée pour l'authentification et les notifications.
     */
    @Column(name = "email", nullable = false, unique = true, length = 200)
    @NotBlank(message = "L'adresse email est obligatoire")
    @Email(message = "L'adresse email doit être valide")
    @Size(max = 200, message = "L'adresse email ne peut pas dépasser 200 caractères")
    private String email;

    /**
     * Mot de passe crypté de l'utilisateur.
     */
    @Column(name = "password", nullable = false, length = 255)
    @NotBlank(message = "Le mot de passe est obligatoire")
    @Size(max = 255, message = "Le mot de passe crypté ne peut pas dépasser 255 caractères")
    private String password;

    /**
     * Prénom de l'utilisateur.
     */
    @Column(name = "first_name", length = 100)
    @Size(max = 100, message = "Le prénom ne peut pas dépasser 100 caractères")
    private String firstName;

    /**
     * Nom de famille de l'utilisateur.
     */
    @Column(name = "last_name", length = 100)
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String lastName;

    /**
     * Adresse physique de l'utilisateur.
     */
    @Column(name = "address", length = 255)
    @Size(max = 255, message = "L'adresse ne peut pas dépasser 255 caractères")
    private String address;

    /**
     * Rôle de l'utilisateur dans l'application.
     * Valeurs possibles: visitor, user, admin
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @NotNull(message = "Le rôle utilisateur est obligatoire")
    private UserRole role;

    /**
     * Date de création du compte utilisateur.
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Indique si l'adresse email a été vérifiée.
     */
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

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

    /**
     * Notifications sent by this user.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> sentNotifications;

    /**
     * Notifications received by this user.
     */
    @OneToMany(mappedBy = "userReceiver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Notification> receivedNotifications;

    /**
     * Constructeur par défaut.
     */
    public User() {
        this.role = UserRole.getDefaultRole();
        this.emailVerified = false;
    }

    /**
     * Constructeur avec paramètres principaux.
     * 
     * @param email adresse email
     * @param password mot de passe crypté
     * @param firstName prénom
     * @param lastName nom de famille
     */
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

    /**
     * Vérifie si l'utilisateur a un rôle spécifique.
     * 
     * @param role rôle à vérifier
     * @return true si l'utilisateur a le rôle
     */
    public boolean hasRole(UserRole role) {
        return this.role == role;
    }


    /**
     * Marque l'adresse email comme vérifiée.
     */
    public void verifyEmail() {
        this.emailVerified = true;
    }

    /**
     * Met à jour le profil utilisateur.
     * 
     * @param firstName nouveau prénom (peut être null)
     * @param lastName nouveau nom de famille (peut être null)
     * @param address nouvelle adresse (peut être null)
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

    /**
     * Met à jour uniquement le prénom de l'utilisateur.
     * 
     * @param firstName nouveau prénom
     */
    public void updateFirstName(String firstName) {
        if (firstName != null && !firstName.trim().isEmpty()) {
            this.firstName = firstName.trim();
        }
    }

    /**
     * Met à jour uniquement le nom de famille de l'utilisateur.
     * 
     * @param lastName nouveau nom de famille
     */
    public void updateLastName(String lastName) {
        if (lastName != null && !lastName.trim().isEmpty()) {
            this.lastName = lastName.trim();
        }
    }

    /**
     * Met à jour uniquement l'adresse de l'utilisateur.
     * 
     * @param address nouvelle adresse
     */
    public void updateAddress(String address) {
        if (address != null && !address.trim().isEmpty()) {
            this.address = address.trim();
        }
    }

    /**
     * Met à jour le mot de passe de l'utilisateur.
     * Note: Le mot de passe doit être déjà crypté avant d'appeler cette méthode.
     * 
     * @param encryptedPassword nouveau mot de passe crypté
     */
    public void updatePassword(String encryptedPassword) {
        if (encryptedPassword != null && !encryptedPassword.trim().isEmpty()) {
            this.password = encryptedPassword;
        }
    }

    /**
     * Met à jour l'email de l'utilisateur et réinitialise le statut de vérification.
     * 
     * @param newEmail nouvelle adresse email
     */
    public void updateEmail(String newEmail) {
        if (newEmail != null && !newEmail.trim().isEmpty()) {
            this.email = newEmail.trim().toLowerCase();
            this.emailVerified = false; // Réinitialiser la vérification lors du changement d'email
        }
    }

    /**
     * Vérifie si le profil utilisateur est complet.
     * Un profil est considéré comme complet si le prénom et le nom sont renseignés.
     * 
     * @return true si le profil est complet
     */
    public boolean isProfileComplete() {
        return firstName != null && !firstName.trim().isEmpty() 
            && lastName != null && !lastName.trim().isEmpty();
    }

    /**
     * Vérifie si l'utilisateur a fourni une adresse.
     * 
     * @return true si l'adresse est renseignée
     */
    public boolean hasAddress() {
        return address != null && !address.trim().isEmpty();
    }

    /**
     * Récupère le nom complet de l'utilisateur.
     * 
     * @return nom complet (prénom + nom) ou email si les noms ne sont pas renseignés
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