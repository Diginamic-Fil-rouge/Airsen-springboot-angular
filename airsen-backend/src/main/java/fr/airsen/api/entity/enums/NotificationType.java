package fr.airsen.api.entity.enums;

/**
 * Enumeration of notification delivery methods in the Airsens application.
 * 
 * This enum defines the available notification channels for alert delivery
 * and user communication within the air quality monitoring system.
 */
public enum NotificationType {
    /**
     * Email notification delivery
     */
    EMAIL("Email"),
    
    /**
     * Push notification delivery (for future mobile app support)
     */
    PUSH("Push Notification"),
    
    /**
     * Both email and push notification delivery
     */
    EMAIL_AND_PUSH("Email and Push");

    private final String displayName;

    NotificationType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Checks if this notification type includes email delivery.
     * 
     * @return true if email is included in this notification type
     */
    public boolean includesEmail() {
        return this == EMAIL || this == EMAIL_AND_PUSH;
    }

    /**
     * Checks if this notification type includes push delivery.
     * 
     * @return true if push is included in this notification type
     */
    public boolean includesPush() {
        return this == PUSH || this == EMAIL_AND_PUSH;
    }
}