package fr.airsen.api.entity.enums;

/**
 * Enumeration of alert delivery statuses in the Airsens alert system.
 * 
 * This enum defines the possible states of alert notifications
 * for tracking delivery success and failure.
 */
public enum AlertStatus {
    SENT("Sent"),
    
    FAILED("Failed"),
    
    PENDING("Pending");

    private final String displayName;

    AlertStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this status indicates a completed delivery attempt.
     * 
     * @return true if the status is either SENT or FAILED
     */
    public boolean isCompleted() {
        return this == SENT || this == FAILED;
    }

    /**
     * Checks if this status indicates successful delivery.
     * 
     * @return true if the status is SENT
     */
    public boolean isSuccessful() {
        return this == SENT;
    }
}