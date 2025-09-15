package fr.airsen.api.entity.enums;

/**
 * Énumération des rôles utilisateur dans l'application Airsen.
 * 
 * Définit les niveaux d'autorisation pour l'accès aux fonctionnalités
 * de l'application selon les spécifications du projet.
 */
public enum UserRole {

    /**
     * Visiteur - Accès très limité aux pages publiques.
     * 
     * Permissions:
     * - Accès à la page d'accueil uniquement
     * - Lecture des discussions du forum (pas de participation)
     * - Pas d'accès aux données de qualité de l'air
     * - Pas d'accès aux données météorologiques
     * - Pas d'accès aux données géographiques
     */
    VISITOR("visitor", "Visiteur", "Accès limité à la page d'accueil et lecture du forum"),

    /**
     * Utilisateur connecté - Accès complet aux données et fonctionnalités personnalisées.
     * 
     * Permissions (accès complet):
     * - Consultation des données de qualité de l'air
     * - Consultation des données météorologiques
     * - Accès aux données géographiques (cartes, communes)
     * - Gestion du profil utilisateur
     * - Ajout/suppression de favoris (max 10)
     * - Configuration d'alertes personnalisées (email uniquement)
     * - Participation complète aux discussions du forum (poster, voter)
     * - Export de données personnalisées (5/jour, 10/mois, 15/an)
     */
    USER("user", "Utilisateur connecté", "Accès complet aux données et fonctionnalités personnalisées"),

    /**
     * Administrateur - Accès complet à la gestion de l'application.
     * 
     * Permissions héritées de USER plus:
     * - Gestion des utilisateurs
     * - Modération du forum
     * - Configuration des sources de données externes
     * - Accès aux métriques et statistiques d'utilisation
     * - Gestion des paramètres de l'application
     */
    ADMIN("admin", "Administrateur", "Accès complet à la gestion de l'application");

    private final String value;
    private final String displayName;
    private final String description;

    /**
     * Constructeur de l'énumération UserRole.
     * 
     * @param value valeur stockée en base de données
     * @param displayName nom d'affichage du rôle
     * @param description description des permissions du rôle
     */
    UserRole(String value, String displayName, String description) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Récupère la valeur stockée en base de données.
     * 
     * @return valeur de l'énumération pour la base de données
     */
    public String getValue() {
        return value;
    }

    /**
     * Récupère le nom d'affichage du rôle.
     * 
     * @return nom d'affichage localisé
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Récupère la description des permissions du rôle.
     * 
     * @return description des permissions
     */
    public String getDescription() {
        return description;
    }

    /**
     * Vérifie si le rôle actuel a des permissions supérieures ou égales
     * au rôle spécifié.
     * 
     * @param role rôle à comparer
     * @return true si le rôle actuel a des permissions supérieures ou égales
     */
    public boolean hasPermissionsOf(UserRole role) {
        return this.ordinal() >= role.ordinal();
    }

    /**
     * Vérifie si le rôle correspond à un administrateur.
     * 
     * @return true si le rôle est ADMIN
     */
    public boolean isAdmin() {
        return this == ADMIN;
    }

    /**
     * Vérifie si le rôle correspond à un utilisateur authentifié.
     * 
     * @return true si le rôle est USER ou ADMIN
     */
    public boolean isAuthenticated() {
        return this == USER || this == ADMIN;
    }

    /**
     * Récupère le rôle par défaut pour les nouveaux utilisateurs.
     * 
     * @return rôle par défaut selon la spécification (USER pour les nouvelles inscriptions)
     */
    public static UserRole getDefaultRole() {
        return USER;
    }

    /**
     * Récupère un rôle par sa valeur de base de données.
     * 
     * @param value valeur stockée en base de données
     * @return rôle correspondant ou null si non trouvé
     */
    public static UserRole fromValue(String value) {
        if (value == null) return null;
        
        for (UserRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return null;
    }

    /**
     * Vérifie si ce rôle peut accéder aux données de qualité de l'air.
     * 
     * @return true si le rôle peut accéder aux données (USER ou ADMIN)
     */
    public boolean canAccessAirQualityData() {
        return this == USER || this == ADMIN;
    }

    /**
     * Vérifie si ce rôle peut gérer d'autres utilisateurs.
     * 
     * @return true si le rôle peut gérer les utilisateurs (ADMIN seulement)
     */
    public boolean canManageUsers() {
        return this == ADMIN;
    }

    /**
     * Vérifie si ce rôle peut poster dans le forum.
     * 
     * @return true si le rôle peut poster (USER ou ADMIN)
     */
    public boolean canPostInForum() {
        return this == USER || this == ADMIN;
    }

    @Override
    public String toString() {
        return displayName;
    }
}