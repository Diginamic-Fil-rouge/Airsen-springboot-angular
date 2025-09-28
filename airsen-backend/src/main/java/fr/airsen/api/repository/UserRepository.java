package fr.airsen.api.repository;

import fr.airsen.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    Page<User> findByEmailVerifiedTrue(Pageable pageable);


    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.id = :userId")
    void markEmailAsVerified(@Param("userId") Long userId);

    long countByEmailVerifiedTrue();

    /**
     * Deletes users created before a certain date.
     * Used for data retention policy (1 year maximum).
     * 
     * @param cutoffDate cutoff date for deletion
     * @return number of deleted users
     */
    @Modifying
    @Query("DELETE FROM User u WHERE u.createdAt < :cutoffDate")
    int deleteByCreatedAtBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    // ==================== PROFILE UPDATE METHODS ====================

    @Modifying
    @Query("UPDATE User u SET u.firstName = :firstName, u.lastName = :lastName, u.address = :address WHERE u.id = :userId")
    void updateUserProfile(@Param("userId") Long userId, 
                          @Param("firstName") String firstName, 
                          @Param("lastName") String lastName, 
                          @Param("address") String address);

    @Modifying
    @Query("UPDATE User u SET u.firstName = :firstName WHERE u.id = :userId")
    void updateFirstName(@Param("userId") Long userId, @Param("firstName") String firstName);

    @Modifying
    @Query("UPDATE User u SET u.lastName = :lastName WHERE u.id = :userId")
    void updateLastName(@Param("userId") Long userId, @Param("lastName") String lastName);

    @Modifying
    @Query("UPDATE User u SET u.address = :address WHERE u.id = :userId")
    void updateAddress(@Param("userId") Long userId, @Param("address") String address);

    // ==================== EMAIL MANAGEMENT METHODS ====================
    /**
     * Checks if an email is already used by another user.
     * 
     * @param email email to check
     * @param excludeUserId identifier of the user to exclude from verification
     * @return true if the email is already used by another user
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.id != :excludeUserId")
    boolean isEmailTakenByOtherUser(@Param("email") String email, @Param("excludeUserId") Long excludeUserId);

    // ==================== PASSWORD MANAGEMENT METHODS ====================

    /**
     * Updates a user's password.
     * Note: The password must already be encrypted before being passed to this method.
     * 
     * @param userId user identifier
     * @param encryptedPassword new encrypted password
     */
    @Modifying
    @Query("UPDATE User u SET u.password = :encryptedPassword WHERE u.id = :userId")
    void updatePassword(@Param("userId") Long userId, @Param("encryptedPassword") String encryptedPassword);

    /**
     * Retrieves a user's encrypted password for verification.
     * Used to validate the old password during password change.
     * 
     * @param userId user identifier
     * @return encrypted password or null if user does not exist
     */
    @Query("SELECT u.password FROM User u WHERE u.id = :userId")
    String findPasswordByUserId(@Param("userId") Long userId);

    // ==================== BROADCAST NOTIFICATION METHODS ====================

    /**
     * Finds all active users with verified emails for broadcasting.
     */
    @Query("SELECT DISTINCT u FROM User u WHERE u.isActive = true AND u.emailVerified = true")
    java.util.List<User> findAllActiveUsersWithVerifiedEmail();

    /**
     * Finds users by region code through their alerts.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.alerts a JOIN a.commune c JOIN c.department d JOIN d.region r " +
           "WHERE u.isActive = true AND u.emailVerified = true AND r.regionCode = :regionCode")
    java.util.List<User> findActiveUsersByRegionCode(@Param("regionCode") String regionCode);

    /**
     * Finds users by department code through their alerts.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.alerts a JOIN a.commune c JOIN c.department d " +
           "WHERE u.isActive = true AND u.emailVerified = true AND d.departmentCode = :departmentCode")
    java.util.List<User> findActiveUsersByDepartmentCode(@Param("departmentCode") String departmentCode);

    /**
     * Finds users by commune code through their alerts.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.alerts a JOIN a.commune c " +
           "WHERE u.isActive = true AND u.emailVerified = true AND c.inseeCode = :communeCode")
    java.util.List<User> findActiveUsersByCommuneCode(@Param("communeCode") String communeCode);

    /**
     * Finds users by region code through their favorite communes.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.favoris c JOIN c.department d JOIN d.region r " +
           "WHERE u.isActive = true AND u.emailVerified = true AND r.regionCode = :regionCode")
    java.util.List<User> findActiveUsersByFavoriteRegionCode(@Param("regionCode") String regionCode);

    /**
     * Finds users by department code through their favorite communes.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.favoris c JOIN c.department d " +
           "WHERE u.isActive = true AND u.emailVerified = true AND d.departmentCode = :departmentCode")
    java.util.List<User> findActiveUsersByFavoriteDepartmentCode(@Param("departmentCode") String departmentCode);

    /**
     * Finds users by commune code through their favorite communes.
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.favoris c " +
           "WHERE u.isActive = true AND u.emailVerified = true AND c.inseeCode = :communeCode")
    java.util.List<User> findActiveUsersByFavoriteCommuneCode(@Param("communeCode") String communeCode);

}