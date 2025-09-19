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
 * Entity representing a user data export request.
 * 
 * This entity manages environmental data export requests
 * with filtering by data type, file format and date range.
 * Includes per-user export limits to respect system constraints.
 */
@Entity
@Table(name = "export_requests", indexes = {
    @Index(name = "idx_export_user_id", columnList = "user_id"),
    @Index(name = "idx_export_status", columnList = "status"),
    @Index(name = "idx_export_created_date", columnList = "created_date")
})
@EntityListeners(AuditingEntityListener.class)
public class ExportRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required for export request")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 50)
    @NotNull(message = "Export type is required")
    private ExportType exportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_format", nullable = false, length = 10)
    @NotNull(message = "File format is required")
    private FileFormat fileFormat;

    @Column(name = "start_date", nullable = false)
    @NotNull(message = "Start date is required")
    @PastOrPresent(message = "Start date cannot be in the future")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @NotNull(message = "End date is required")
    @PastOrPresent(message = "End date cannot be in the future")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Export status is required")
    private ExportStatus status;

    @CreatedDate
    @Column(name = "created_date", nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @Column(name = "generated_file", length = 255)
    @Size(max = 255, message = "File path cannot exceed 255 characters")
    private String generatedFile;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "export_communes",
        joinColumns = @JoinColumn(name = "export_request_id"),
        inverseJoinColumns = @JoinColumn(name = "commune_id")
    )
    private List<Commune> communes;

    public ExportRequest() {
        this.status = ExportStatus.IN_PROGRESS;
    }

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
     * Validates that the date range is consistent (start date <= end date).
     * 
     * @return true if the date range is valid
     */
    @AssertTrue(message = "Start date must be before or equal to end date")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) {
            return true; // @NotNull validations handle this case
        }
        return !startDate.isAfter(endDate);
    }

    /**
     * Marks the export as completed successfully.
     * 
     * @param filePath path of the generated file
     */
    public void markAsCompleted(String filePath) {
        this.status = ExportStatus.COMPLETED;
        this.generatedFile = filePath;
    }

    /**
     * Marks the export as failed.
     */
    public void markAsFailed() {
        this.status = ExportStatus.FAILED;
        this.generatedFile = null;
    }

    /**
     * Checks if the export is finished (success or failure).
     * 
     * @return true if export is in a final state
     */
    public boolean isFinished() {
        return status == ExportStatus.COMPLETED || status == ExportStatus.FAILED;
    }

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