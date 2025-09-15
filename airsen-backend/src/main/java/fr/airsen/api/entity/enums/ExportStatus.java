package fr.airsen.api.entity.enums;

/**
 * Statuts possibles pour une demande d'export de données.
 * 
 * Définit les différents états par lesquels passe une demande d'export
 * depuis sa création jusqu'à sa finalisation ou son échec.
 */
public enum ExportStatus {
    
    /**
     * Export en cours de traitement.
     */
    IN_PROGRESS("in_progress"),
    
    /**
     * Export terminé avec succès.
     */
    COMPLETED("completed"),
    
    /**
     * Export échoué avec erreur.
     */
    FAILED("failed");
    
    private final String value;
    
    ExportStatus(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
    
    /**
     * Récupère le ExportStatus à partir de sa valeur string.
     * 
     * @param value valeur string du statut
     * @return ExportStatus correspondant
     * @throws IllegalArgumentException si la valeur n'est pas reconnue
     */
    public static ExportStatus fromValue(String value) {
        for (ExportStatus status : ExportStatus.values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Statut d'export inconnu: " + value);
    }
}