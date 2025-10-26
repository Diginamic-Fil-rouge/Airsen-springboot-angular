package fr.airsen.api.entity.enums;

public enum NotificationDeliveryStatus {

    PENDING("Pending"),

    SENT("Sent"),

    FAILED("Failed");

    private final String displayName;

    NotificationDeliveryStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
