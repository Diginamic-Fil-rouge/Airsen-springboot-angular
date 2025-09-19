package fr.airsen.api.entity;

import fr.airsen.api.entity.enums.ExportType;
import fr.airsen.api.entity.enums.FileFormat;
import fr.airsen.api.entity.enums.ExportStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entité représentant une demande d'export de données utilisateur.
 * 
 * Cette entité gère les demandes d'export de données environnementales
 * avec filtrage par type de données, format de fichier et plage de dates.
 * Inclut les limites d'export par utilisateur pour respecter les contraintes système.
 */
@Entity
@Table(name = "export_requests", indexes = {
    @Index(name = "idx_export_user_id", columnList = "user_id"),
    @Index(name = "idx_export_status", columnList = "status"),
    @Index(name = "idx_export_created_date", columnList = "created_date")
})
@EntityListeners(AuditingEntityListener.class)
public class ExportRequest {

    /**
     * Identifiant unique de la demande d'export.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Utilisateur ayant effectué la demande d'export.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "L'utilisateur est obligatoire pour une demande d'export")
    private User user;

    /**
     * Type de données à exporter (qualité de l'air, météo, population, complet).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 50)
    @NotNull(message = "Le type d'export est obligatoire")
    private ExportType exportType;

    /**
     * Format du fichier d'export (PDF, CSV).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_format", nullable = false, length = 10)
    @NotNull(message = "Le format de fichier est obligatoire")
    private FileFormat fileFormat;

    /**
     * Date de début de la plage de données à exporter.
     */
    @Column(name = "start_date", nullable = false)
    @NotNull(message = "La date de début est obligatoire")
    @PastOrPresent(message = "La date de début ne peut pas être dans le futur")
    private LocalDate startDate;

    /**
     * Date de fin de la plage de données à exporter.
     */
    @Column(name = "end_date", nullable = false)
    @NotNull(message = "La date de fin est obligatoire")
    @PastOrPresent(message = "La date de fin ne peut pas être dans le futur")
    private LocalDate endDate;

    /**
     * Statut actuel de la demande d'export.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Le statut d'export est obligatoire")
    private ExportStatus status;

    /**
     * Date de création de la demande d'export.
     */
    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    /**
     * Chemin du fichier généré (null si l'export n'est pas terminé).
     */
    @Column(name = "generated_file", length = 255)
    @Size(max = 255, message = "Le chemin du fichier ne peut pas dépasser 255 caractères")
    private String generatedFile;

    /**
     * Communes incluses dans cette demande d'export.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "export_communes",
        joinColumns = @JoinColumn(name = "export_request_id"),
        inverseJoinColumns = @JoinColumn(name = "commune_id")
    )
    private List<Commune> communes;

    /**
     * Constructeur par défaut.
     */
    public ExportRequest() {
        this.status = ExportStatus.IN_PROGRESS;
    }

    /**
     * Constructeur avec paramètres principaux.
     * 
     * @param user utilisateur demandant l'export
     * @param exportType type de données à exporter
     * @param fileFormat format du fichier d'export
     * @param startDate date de début de la plage
     * @param endDate date de fin de la plage
     */
    public ExportRequest(User user, ExportType exportType, FileFormat fileFormat, 
                        LocalDate startDate, LocalDate endDate) {
        this();
        this.user = user;
        this.exportType = exportType;
        this.fileFormat = fileFormat;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ExportType getExportType() {
        return exportType;
    }

    public void setExportType(ExportType exportType) {
        this.exportType = exportType;
    }

    public FileFormat getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(FileFormat fileFormat) {
        this.fileFormat = fileFormat;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public ExportStatus getStatus() {
        return status;
    }

    public void setStatus(ExportStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public String getGeneratedFile() {
        return generatedFile;
    }

    public void setGeneratedFile(String generatedFile) {
        this.generatedFile = generatedFile;
    }

    public List<Commune> getCommunes() {
        return communes;
    }

    public void setCommunes(List<Commune> communes) {
        this.communes = communes;
    }

    /**
     * Valide que la plage de dates est cohérente (date de début <= date de fin).
     * 
     * @return true si la plage de dates est valide
     */
    @AssertTrue(message = "La date de début doit être antérieure ou égale à la date de fin")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true; // @NotNull validations handle this case
        }
        return !startDate.isAfter(endDate);
    }

    /**
     * Marque l'export comme terminé avec succès.
     * 
     * @param filePath chemin du fichier généré
     */
    public void markAsCompleted(String filePath) {
        this.status = ExportStatus.COMPLETED;
        this.generatedFile = filePath;
    }

    /**
     * Marque l'export comme échoué.
     */
    public void markAsFailed() {
        this.status = ExportStatus.FAILED;
        this.generatedFile = null;
    }

    /**
     * Vérifie si l'export est terminé (succès ou échec).
     * 
     * @return true si l'export est dans un état final
     */
    public boolean isFinished() {
        return status == ExportStatus.COMPLETED || status == ExportStatus.FAILED;
    }

    /**
     * Vérifie si l'export a réussi.
     * 
     * @return true si l'export est terminé avec succès
     */
    public boolean isSuccessful() {
        return status == ExportStatus.COMPLETED && generatedFile != null;
    }

    @Override
    public String toString() {
        return "ExportRequest{" +
                "id=" + id +
                ", exportType=" + exportType +
                ", fileFormat=" + fileFormat +
                ", startDate=" + startDate +
                ", endDate=" + endDate +
                ", status=" + status +
                ", createdDate=" + createdDate +
                ", generatedFile='" + generatedFile + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ExportRequest that = (ExportRequest) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}