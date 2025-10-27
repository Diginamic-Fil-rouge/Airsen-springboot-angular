package fr.airsen.api.entity.enums;

public enum AlertSignalLevel {

    INFO("Information"),

    WATCH("Watch"),

    ALERT("Alert");

    private final String displayName;

    AlertSignalLevel(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
