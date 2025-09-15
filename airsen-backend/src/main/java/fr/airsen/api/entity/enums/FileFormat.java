package fr.airsen.api.entity.enums;

/**
 * Formats de fichiers supportés pour l'export de données.
 * 
 * Définit les formats dans lesquels les utilisateurs peuvent
 * exporter les données de l'application de surveillance de qualité de l'air.
 */
public enum FileFormat {
    
    /**
     * Format PDF pour les rapports formatés.
     */
    PDF("pdf"),
    
    /**
     * Format CSV pour les données tabulaires.
     */
    CSV("csv");
    
    private final String value;
    
    FileFormat(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Récupère le FileFormat à partir de sa valeur string.
     * 
     * @param value valeur string du format
     * @return FileFormat correspondant
     * @throws IllegalArgumentException si la valeur n'est pas reconnue
     */
    public static FileFormat fromValue(String value) {
        for (FileFormat fileFormat : FileFormat.values()) {
            if (fileFormat.value.equals(value)) {
                return fileFormat;
            }
        }
        throw new IllegalArgumentException("Format de fichier inconnu: " + value);
    }
}