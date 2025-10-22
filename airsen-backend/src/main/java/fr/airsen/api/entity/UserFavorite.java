package fr.airsen.api.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * Join entity representing a user's favorite commune.
 *
 * This entity replaces the simple @ManyToMany relationship to add audit trail
 * and enable business logic validation (maximum 10 favorites per user).
 *
 * Database Table: user_favorites
 * Primary Key: Composite (user_id, insee_code)
 * Foreign Keys: user_id -> users.id, insee_code -> communes.insee_code
 */
@Entity
@Table(name = "user_favorites",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_user_commune",
           columnNames = {"user_id", "insee_code"}
       ),
       indexes = {
           @Index(name = "idx_user_favorites_user_id", columnList = "user_id"),
           @Index(name = "idx_user_favorites_insee_code", columnList = "insee_code"),
           @Index(name = "idx_user_favorites_created_at", columnList = "created_at")
       })
public class UserFavorite {

    @EmbeddedId
    private UserFavoriteId id;

    /**
     * The user who favorited the commune.
     * Lazy loading to prevent N+1 query problems.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required for favorite")
    private User user;

    /**
     * The commune that was favorited.
     * Lazy loading with JOIN FETCH in repository queries.
     *
     * Note: Cannot use @MapsId because insee_code is not the @Id field in Commune.
     * The composite key ID must be set manually before saving the entity.
     * References insee_code column (natural identifier) instead of auto-generated id.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insee_code", nullable = false, referencedColumnName = "insee_code", insertable = false, updatable = false)
    @NotNull(message = "Commune is required for favorite")
    private Commune commune;

    /**
     * Timestamp when the favorite was created.
     * Automatically set on entity creation via @PrePersist.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @NotNull(message = "Creation timestamp is required")
    private LocalDateTime createdAt;

    /**
     * JPA lifecycle callback to set creation timestamp.
     * Called automatically before entity is persisted to database.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public UserFavorite() {
    }

    /**
     * Constructor for creating a new favorite relationship.
     * Manually sets composite key because insee_code is not the @Id in Commune.
     *
     * @param user User who is favoriting
     * @param commune Commune being favorited
     */
    public UserFavorite(User user, Commune commune) {
        this.id = new UserFavoriteId(user.getId(), commune.getInseeCode());
        this.user = user;
        this.commune = commune;
    }

    // Getters and Setters

    public UserFavoriteId getId() {
        return id;
    }

    public void setId(UserFavoriteId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Commune getCommune() {
        return commune;
    }

    public void setCommune(Commune commune) {
        this.commune = commune;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserFavorite that = (UserFavorite) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "UserFavorite{" +
                "id=" + id +
                ", createdAt=" + createdAt +
                '}';
    }
}
