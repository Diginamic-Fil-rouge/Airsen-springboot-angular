package fr.airsen.api.entity.enums;

public enum NotificationCampaignStatus {


    DRAFT("Draft"),

    SENDING("Sending"),

    COMPLETED("Completed"),

    FAILED("Failed");

    private final String displayName;

    NotificationCampaignStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
