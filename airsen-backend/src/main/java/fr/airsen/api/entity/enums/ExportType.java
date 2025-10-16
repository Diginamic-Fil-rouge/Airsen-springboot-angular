package fr.airsen.api.entity.enums;

/**
 * Filter types for data export requests.
 * 
 * Defines the different types of data that can be exported
 * by users of the air quality monitoring application.
 */
public enum ExportType {
    

    AIR_QUALITY("air_quality"),

    WEATHER("weather"),

    POPULATION("population"),

    COMPLETE("complete");
    
    private final String value;
    
    ExportType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    

    public static ExportType fromValue(String value) {
        for (ExportType exportType : ExportType.values()) {
            if (exportType.value.equals(value)) {
                return exportType;
            }
        }
        throw new IllegalArgumentException("Unknown export type: " + value);
    }
}