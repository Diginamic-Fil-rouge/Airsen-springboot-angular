package fr.airsen.api.entity.enums;

/**
 * Enumeration of notification delivery channels in the Airsens application.
 * 
 * This enum defines the specific channels used for sending notifications,
 * currently limited to email as per business requirements.
 */
public enum NotificationChannel {
    EMAIL("Email");

    private final String displayName;

    NotificationChannel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}