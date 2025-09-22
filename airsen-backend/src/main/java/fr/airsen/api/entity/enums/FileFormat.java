package fr.airsen.api.entity.enums;

/**
 * File formats supported for data export.
 * 
 * Defines the formats in which users can export data
 * from the air quality monitoring application.
 */
public enum FileFormat {

    PDF("pdf"),
    

    CSV("csv");
    
    private final String value;
    
    FileFormat(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }

    public static FileFormat fromValue(String value) {
        for (FileFormat fileFormat : FileFormat.values()) {
            if (fileFormat.value.equals(value)) {
                return fileFormat;
            }
        }
        throw new IllegalArgumentException("Unknown file format: " + value);
    }
}