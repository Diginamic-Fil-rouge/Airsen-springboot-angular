package fr.airsen.api.entity.enums;

public enum AlertSignalSource {

    ATMO("ATMO France API"),

    WEATHER("Weather Monitoring");

    private final String displayName;

    AlertSignalSource(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
