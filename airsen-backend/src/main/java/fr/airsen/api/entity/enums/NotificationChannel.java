package fr.airsen.api.entity.enums;

/**
 * Enumeration of notification delivery channels in the Airsens application.
 * 
 * This enum defines the specific channels used for sending notifications,
 * currently limited to email as per business requirements.
 */
public enum NotificationChannel {
    /**
     * Email delivery channel (primary notification method)
     */
    EMAIL("Email");

    private final String displayName;

    /**
     * Constructor for NotificationChannel enum.
     * 
     * @param displayName human-readable name of the notification channel
     */
    NotificationChannel(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Gets the human-readable display name of the notification channel.
     * 
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }
}