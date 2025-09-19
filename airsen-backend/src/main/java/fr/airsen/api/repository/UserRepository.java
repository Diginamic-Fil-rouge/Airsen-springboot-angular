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

    Page<User> findByEmailVerifiedTrue(Pageable pageable);

    /**
     * Marque l'email d'un utilisateur comme vérifié.
     * 
     * @param userId identifiant de l'utilisateur
     */
    @Modifying
    @Query("UPDATE User u SET u.emailVerified = true WHERE u.id = :userId")
    void markEmailAsVerified(@Param("userId") Long userId);

    long countByEmailVerifiedTrue();

    /**
     * Supprime les utilisateurs créés avant une certaine date.
     * Utilisé pour la politique de rétention des données (1 an maximum).
     * 
     * @param cutoffDate date limite pour la suppression
     * @return nombre d'utilisateurs supprimés
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
     * Vérifie si un email est déjà utilisé par un autre utilisateur.
     * 
     * @param email email à vérifier
     * @param excludeUserId identifiant de l'utilisateur à exclure de la vérification
     * @return true si l'email est déjà utilisé par un autre utilisateur
     */
    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.id != :excludeUserId")
    boolean isEmailTakenByOtherUser(@Param("email") String email, @Param("excludeUserId") Long excludeUserId);

    // ==================== PASSWORD MANAGEMENT METHODS ====================

    /**
     * Met à jour le mot de passe d'un utilisateur.
     * Note: Le mot de passe doit être déjà crypté avant d'être passé à cette méthode.
     * 
     * @param userId identifiant de l'utilisateur
     * @param encryptedPassword nouveau mot de passe crypté
     */
    @Modifying
    @Query("UPDATE User u SET u.password = :encryptedPassword WHERE u.id = :userId")
    void updatePassword(@Param("userId") Long userId, @Param("encryptedPassword") String encryptedPassword);

    /**
     * Récupère le mot de passe crypté d'un utilisateur pour vérification.
     * Utilisé pour valider l'ancien mot de passe lors du changement.
     * 
     * @param userId identifiant de l'utilisateur
     * @return mot de passe crypté ou null si l'utilisateur n'existe pas
     */
    @Query("SELECT u.password FROM User u WHERE u.id = :userId")
    String findPasswordByUserId(@Param("userId") Long userId);

}