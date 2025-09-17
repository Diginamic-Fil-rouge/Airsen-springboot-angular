package fr.airsen.api.entity.enums;

/**
 * Enumeration of air pollutants monitored by the ATMO France air quality system.
 * 
 * This enum defines the various pollutants that can be tracked for alert thresholds
 * in the Airsens air quality monitoring application.
 */
public enum Pollutant {
    /**
     * Nitrogen Dioxide - Primary traffic-related pollutant
     */
    NO2("Nitrogen Dioxide", "µg/m³"),
    
    /**
     * Ozone - Secondary pollutant formed by photochemical reactions
     */
    O3("Ozone", "µg/m³"),
    
    /**
     * Particulate Matter 10 micrometers - Coarse particles
     */
    PM10("Particulate Matter 10", "µg/m³"),
    
    /**
     * Particulate Matter 2.5 micrometers - Fine particles
     */
    PM25("Particulate Matter 2.5", "µg/m³"),
    
    /**
     * Sulfur Dioxide - Industrial and fossil fuel combustion pollutant
     */
    SO2("Sulfur Dioxide", "µg/m³");

    private final String displayName;
    private final String unit;

    /**
     * Constructor for Pollutant enum.
     * 
     * @param displayName human-readable name of the pollutant
     * @param unit measurement unit for the pollutant
     */
    Pollutant(String displayName, String unit) {
        this.displayName = displayName;
        this.unit = unit;
    }

    /**
     * Gets the human-readable display name of the pollutant.
     * 
     * @return display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the measurement unit for the pollutant.
     * 
     * @return measurement unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * Gets the database column name for this pollutant in AirQuality entity.
     * 
     * @return column name corresponding to this pollutant
     */
    public String getColumnName() {
        return switch (this) {
            case NO2 -> "NO2";
            case O3 -> "O3";
            case PM10 -> "Pm10";
            case PM25 -> "Pm25";
            case SO2 -> "SO2";
        };
    }
}