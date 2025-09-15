package fr.airsen.api.entity.enums;

/**
 * Types de filtres pour les demandes d'export de données.
 * 
 * Définit les différents types de données qui peuvent être exportées
 * par les utilisateurs de l'application de surveillance de qualité de l'air.
 */
public enum ExportType {
    
    /**
     * Export des données de qualité de l'air uniquement.
     */
    AIR_QUALITY("air_quality"),
    
    /**
     * Export des données météorologiques uniquement.
     */
    WEATHER("weather"),
    
    /**
     * Export des données démographiques de population uniquement.
     */
    POPULATION("population"),
    
    /**
     * Export complet incluant toutes les données disponibles.
     */
    COMPLETE("complete");
    
    private final String value;
    
    ExportType(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Récupère le ExportType à partir de sa valeur string.
     * 
     * @param value valeur string du filtre
     * @return ExportType correspondant
     * @throws IllegalArgumentException si la valeur n'est pas reconnue
     */
    public static ExportType fromValue(String value) {
        for (ExportType exportType : ExportType.values()) {
            if (exportType.value.equals(value)) {
                return exportType;
            }
        }
        throw new IllegalArgumentException("Type d'export inconnu: " + value);
    }
}