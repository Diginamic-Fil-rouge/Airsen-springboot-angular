package fr.airsen.api.entity.enums;

/**
 * Possible statuses for a data export request.
 * 
 * Defines the different states through which an export request passes
 * from its creation to its completion or failure.
 */
public enum ExportStatus {
    

    IN_PROGRESS("in_progress"),

    COMPLETED("completed"),

    FAILED("failed");
    
    private final String value;
    
    ExportStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }

    public static ExportStatus fromValue(String value) {
        for (ExportStatus status : ExportStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown export status: " + value);
    }
}