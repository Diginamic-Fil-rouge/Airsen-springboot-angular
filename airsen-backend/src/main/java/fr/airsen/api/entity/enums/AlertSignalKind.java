package fr.airsen.api.entity.enums;

/**
 * Enumeration of environmental signal types monitored by AIRSEN.
 * Defines the specific kind of environmental condition that triggered an alert signal.
 */
public enum AlertSignalKind {
    
    AQI("Air Quality Index"),

    /**
     * Particulate Matter 2.5 micrometers alert.
     * Fine particles that can penetrate deep into lungs.
     */
    PM25("PM2.5 Particulate Matter"),

    /**
     * Particulate Matter 10 micrometers alert.
     * Coarse particles from dust, pollen, and mold.
     */
    PM10("PM10 Particulate Matter"),

    /**
     * Heat alert.
     * Extreme temperature conditions (≥35°C).
     */
    HEAT("High Temperature"),

    /**
     * Wind alert.
     * Strong wind conditions (≥70 km/h).
     */
    WIND("Strong Wind"),

    /**
     * Rain alert.
     * Heavy precipitation conditions (≥30mm).
     */
    RAIN("Heavy Rain");

    private final String displayName;

    AlertSignalKind(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
