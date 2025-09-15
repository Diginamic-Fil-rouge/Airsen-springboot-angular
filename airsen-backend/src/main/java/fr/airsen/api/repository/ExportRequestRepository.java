package fr.airsen.api.repository;

import fr.airsen.api.entity.ExportRequest;
import fr.airsen.api.entity.User;
import fr.airsen.api.entity.enums.ExportStatus;
import fr.airsen.api.entity.enums.ExportType;
import fr.airsen.api.entity.enums.FileFormat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Repository pour la gestion des entités ExportRequest.
 * 
 * Fournit les méthodes d'accès aux données pour les demandes d'export
 * avec validation des limites par utilisateur et gestion des statuts
 * selon les spécifications du système d'export Airsen.
 */
@Repository
public interface ExportRequestRepository extends JpaRepository<ExportRequest, Long> {

    // ==================== BASIC QUERIES ====================

    /**
     * Recherche toutes les demandes d'export d'un utilisateur.
     *
     * @param user     utilisateur
     * @param pageable paramètres de pagination
     * @return page des demandes d'export de l'utilisateur
     */
    Page<ExportRequest> findByUser(User user, Pageable pageable);

    /**
     * Recherche les demandes d'export d'un utilisateur par statut.
     *
     * @param user     utilisateur
     * @param status   statut de l'export
     * @param pageable paramètres de pagination
     * @return page des demandes d'export filtrées par statut
     */
    Page<ExportRequest> findByUserAndStatus(User user, ExportStatus status, Pageable pageable);

    /**
     * Recherche les demandes d'export par type et format.
     *
     * @param exportType type d'export
     * @param fileFormat format de fichier
     * @param pageable   paramètres de pagination
     * @return page des demandes d'export filtrées
     */
    Page<ExportRequest> findByExportTypeAndFileFormat(ExportType exportType, FileFormat fileFormat, Pageable pageable);

    // ==================== EXPORT LIMITS VALIDATION ====================

    /**
     * Compte les demandes d'export d'un utilisateur créées dans les dernières 24 heures.
     * Limite: 5 demandes par jour.
     *
     * @param user  utilisateur
     * @param since date limite (24h en arrière)
     * @return nombre de demandes dans les dernières 24h
     */
    @Query("SELECT COUNT(er) FROM ExportRequest er WHERE er.user = :user AND er.createdDate >= :since")
    long countByUserAndCreatedDateAfter(@Param("user") User user, @Param("since") LocalDateTime since);

    /**
     * Compte les demandes d'export d'un utilisateur créées dans le mois en cours.
     * Limite: 10 demandes par mois.
     *
     * @param user       utilisateur
     * @param monthStart début du mois en cours
     * @return nombre de demandes dans le mois en cours
     */
    @Query("SELECT COUNT(er) FROM ExportRequest er WHERE er.user = :user AND er.createdDate >= :monthStart")
    long countByUserAndCreatedDateInCurrentMonth(@Param("user") User user, @Param("monthStart") LocalDateTime monthStart);

    /**
     * Compte les demandes d'export d'un utilisateur créées dans l'année en cours.
     * Limite: 15 demandes par an.
     *
     * @param user      utilisateur
     * @param yearStart début de l'année en cours
     * @return nombre de demandes dans l'année en cours
     */
    @Query("SELECT COUNT(er) FROM ExportRequest er WHERE er.user = :user AND er.createdDate >= :yearStart")
    long countByUserAndCreatedDateInCurrentYear(@Param("user") User user, @Param("yearStart") LocalDateTime yearStart);

    /**
     * Vérifie si un utilisateur peut créer une nouvelle demande d'export.
     * Contrôle toutes les limites: jour, mois, année.
     *
     * @param userId     identifiant de l'utilisateur
     * @param dayLimit   début de la journée (pour limite quotidienne)
     * @param monthLimit début du mois (pour limite mensuelle)
     * @param yearLimit  début de l'année (pour limite annuelle)
     * @return true si l'utilisateur peut créer une demande
     */
    @Query("""

            SELECT CASE 
            WHEN (SELECT COUNT(er1) FROM ExportRequest er1 WHERE er1.user.id = :userId AND er1.createdDate >= :dayLimit) >= 5 THEN false
            WHEN (SELECT COUNT(er2) FROM ExportRequest er2 WHERE er2.user.id = :userId AND er2.createdDate >= :monthLimit) >= 10 THEN false
            WHEN (SELECT COUNT(er3) FROM ExportRequest er3 WHERE er3.user.id = :userId AND er3.createdDate >= :yearLimit) >= 15 THEN false
            ELSE true
        END
        """)
    boolean canUserCreateNewExport(@Param("userId") Long userId, 
                                  @Param("dayLimit") LocalDateTime dayLimit,
                                  @Param("monthLimit") LocalDateTime monthLimit, 
                                  @Param("yearLimit") LocalDateTime yearLimit);

    // ==================== STATUS AND FILTERING QUERIES ====================

    /**
     * Recherche les demandes d'export terminées avec succès d'un utilisateur.
     *
     * @param user     utilisateur
     * @param pageable paramètres de pagination
     * @return page des exports réussis de l'utilisateur
     */
    @Query("SELECT er FROM ExportRequest er WHERE er.user = :user AND er.status = 'COMPLETED' AND er.generatedFile IS NOT NULL")
    Page<ExportRequest> findSuccessfulExportsByUser(@Param("user") User user, Pageable pageable);

    /**
     * Recherche les demandes d'export dans une plage de dates.
     *
     * @param startDate date de début
     * @param endDate   date de fin
     * @param pageable  paramètres de pagination
     * @return page des demandes d'export dans la plage
     */
    @Query("SELECT er FROM ExportRequest er WHERE er.startDate >= :startDate AND er.endDate <= :endDate")
    Page<ExportRequest> findByDateRange(@Param("startDate") LocalDate startDate, 
                                       @Param("endDate") LocalDate endDate, 
                                       Pageable pageable);
}