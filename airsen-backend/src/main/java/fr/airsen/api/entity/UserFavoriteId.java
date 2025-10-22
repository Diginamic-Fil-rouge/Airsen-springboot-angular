package fr.airsen.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for UserFavorite entity.
 *
 * Combines userId and communeId to uniquely identify a favorite relationship.
 * Implements Serializable as required by JPA for composite keys.
 *
 * Not use Java Record because JPA needs mutable setters
 * to populate fields via @MapsId annotations in UserFavorite entity.
 */
@Embeddable
public class UserFavoriteId implements Serializable {

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "insee_code")
    private String communeInseeCode;

    /**
     * Default no-arg constructor required by JPA.
     */
    public UserFavoriteId() {
    }

    /**
     * Constructor for creating composite key.
     *
     * @param userId User ID
     * @param communeInseeCode Commune INSEE code (5-digit official identifier)
     */
    public UserFavoriteId(Long userId, String communeInseeCode) {
        this.userId = userId;
        this.communeInseeCode = communeInseeCode;
    }

    /**
     * Validates that both components of the composite key are non-null.
     *
     * @throws IllegalArgumentException if userId or communeInseeCode is null
     */
    public void validate() {
        if (userId == null || communeInseeCode == null) {
            throw new IllegalArgumentException("UserFavoriteId requires both userId and communeInseeCode");
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getCommuneInseeCode() {
        return communeInseeCode;
    }

    public void setCommuneInseeCode(String communeInseeCode) {
        this.communeInseeCode = communeInseeCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserFavoriteId that = (UserFavoriteId) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(communeInseeCode, that.communeInseeCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, communeInseeCode);
    }

    @Override
    public String toString() {
        return "UserFavoriteId{" +
                "userId=" + userId +
                ", communeInseeCode='" + communeInseeCode + '\'' +
                '}';
    }
}
