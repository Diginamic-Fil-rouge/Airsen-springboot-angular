package fr.airsen.api.entity.enums;

/**
 * Enumeration of alert delivery statuses in the Airsens alert system.
 * 
 * This enum defines the possible states of alert notifications
 * for tracking delivery success and failure.
 */
public enum AlertStatus {
    /**
     * Alert notification has been successfully sent
     */
    SENT("Sent"),
    
    /**
     * Alert notification failed to send
     */
    FAILED("Failed"),
    
    /**
     * Alert notification is pending delivery
     */
    PENDING("Pending");

    private final String displayName;

    /**
     * Constructor for AlertStatus enum.
     * 
     * @param displayName human-readable name of the alert status
     */
    AlertStatus(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name of the alert status.
     * 
     * @return display name
     */
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