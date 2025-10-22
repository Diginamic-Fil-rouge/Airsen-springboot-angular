package fr.airsen.api.entity;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for UserFavorite entity.
 *
 * Combines userId and communeId to uniquely identify a favorite relationship.
 * Implements Serializable as required by JPA for composite keys.
 *
 * Uses Java 21 Record for immutability and automatic equals/hashCode implementation.
 */
@Embeddable
public record UserFavoriteId(
    Long userId,
    String communeId
) implements Serializable {

    /**
     * Default constructor required by JPA.
     * Creates an empty composite key.
     */
    public UserFavoriteId() {
        this(null, null);
    }

    /**
     * Validates that both components of the composite key are non-null.
     *
     * @throws IllegalArgumentException if userId or communeId is null
     */
    public void validate() {
        if (userId == null || communeId == null) {
            throw new IllegalArgumentException("UserFavoriteId requires both userId and communeId");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserFavoriteId that = (UserFavoriteId) o;
        return Objects.equals(userId, that.userId) && 
               Objects.equals(communeId, that.communeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, communeId);
    }
}
