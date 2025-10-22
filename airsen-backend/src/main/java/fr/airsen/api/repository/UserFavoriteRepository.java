package fr.airsen.api.repository;

import fr.airsen.api.entity.UserFavorite;
import fr.airsen.api.entity.UserFavoriteId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for UserFavorite entity operations.
 *
 * Provides data access methods for managing user favorite communes with
 * optimized queries using JOIN FETCH to prevent N+1 query problems.
 */
@Repository
public interface UserFavoriteRepository extends JpaRepository<UserFavorite, UserFavoriteId> {

    /**
     * Find all favorites for a specific user with complete commune hierarchy.
     *
     * Uses JOIN FETCH to eagerly load commune, department, and region in a single query.
     * This prevents N+1 query problems when accessing commune details.
     *
     * Results are ordered by creation date (newest first) for UI display.
     *
     * @param userId User ID
     * @return List of user favorites with fully loaded commune data
     */
    @Query("SELECT uf FROM UserFavorite uf " +
           "JOIN FETCH uf.commune c " +
           "JOIN FETCH c.department d " +
           "JOIN FETCH d.region r " +
           "WHERE uf.id.userId = :userId " +
           "ORDER BY uf.createdAt DESC")
    List<UserFavorite> findByUserId(@Param("userId") Long userId);

    /**
     * Count the number of favorites for a user.
     *
     * Used for validation to enforce maximum 10 favorites per user.
     * Efficient count query without loading entities.
     *
     * @param userId User ID
     * @return Number of favorites for the user
     */
    @Query("SELECT COUNT(uf) FROM UserFavorite uf WHERE uf.id.userId = :userId")
    int countByUserId(@Param("userId") Long userId);

    /**
     * Check if a specific favorite exists.
     *
     * Fast existence check using composite key components.
     * Used to prevent duplicate favorites.
     *
     * @param userId User ID
     * @param communeId Commune INSEE code
     * @return true if favorite exists, false otherwise
     */
    boolean existsById_UserIdAndId_CommuneId(Long userId, String communeId);

    /**
     * Delete a specific favorite by composite key components.
     *
     * More efficient than loading entity first and then deleting.
     * Used in remove favorite operation.
     *
     * @param userId User ID
     * @param communeId Commune INSEE code
     */
    @Modifying
    @Query("DELETE FROM UserFavorite uf WHERE uf.id.userId = :userId AND uf.id.communeId = :communeId")
    void deleteByUserIdAndCommuneId(@Param("userId") Long userId, @Param("communeId") String communeId);

    /**
     * Get most favorited communes for analytics.
     *
     * Returns communes ordered by favorite count (descending).
     * Optional feature for "Popular Locations" dashboard widget.
     *
     * Usage: findMostFavoritedCommunes(PageRequest.of(0, 10))
     *
     * @param pageable Pagination parameters (limit results)
     * @return List of [Commune, count] pairs ordered by popularity
     */
    @Query("SELECT uf.commune, COUNT(uf) as favCount FROM UserFavorite uf " +
           "GROUP BY uf.commune " +
           "ORDER BY favCount DESC")
    List<Object[]> findMostFavoritedCommunes(Pageable pageable);

    /**
     * Find all users who favorited a specific commune.
     *
     * Optional analytics query for commune popularity analysis.
     * Returns only user IDs to avoid loading full user entities.
     *
     * @param communeId Commune INSEE code
     * @return List of user IDs who favorited this commune
     */
    @Query("SELECT uf.id.userId FROM UserFavorite uf WHERE uf.id.communeId = :communeId")
    List<Long> findUserIdsByCommuneId(@Param("communeId") String communeId);

    /**
     * Delete all favorites for a user.
     *
     * Used when user account is deleted or user wants to clear all favorites.
     * Cascades are configured in entity, but this provides explicit control.
     *
     * @param userId User ID
     */
    @Modifying
    @Query("DELETE FROM UserFavorite uf WHERE uf.id.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
