package fr.airsen.api.dto.response;

import java.time.LocalDateTime;

/**
 * Response DTO for user favorite commune with denormalized geographic hierarchy.
 *
 * This DTO provides a flat structure containing all necessary information
 * for frontend display without requiring additional API calls.
 *
 * Uses INSEE code (official French government identifier) rather than internal
 * database ID for stability and interoperability with other systems.
 */
public record UserFavoriteResponse(

    /**
     * Official French INSEE code (5 digits)
     * Example: "75056" for Paris, "69123" for Lyon
     */
    String communeInseeCode,

    /**
     * Commune name
     * Example: "Paris", "Lyon", "Marseille"
     */
    String communeName,

    /**
     * Department name
     * Example: "Paris", "Rhône", "Bouches-du-Rhône"
     */
    String departmentName,

    /**
     * Region name
     * Example: "Île-de-France", "Auvergne-Rhône-Alpes"
     */
    String regionName,

    /**
     * Timestamp when the favorite was added
     * Used for sorting (newest first) in the UI
     */
    LocalDateTime addedAt
) {

    /**
     * Creates a new UserFavoriteResponse with complete commune information.
     *
     * @param communeInseeCode Commune INSEE code (5 digits)
     * @param communeName Commune name
     * @param departmentName Department name
     * @param regionName Region name
     * @param addedAt Timestamp when favorite was created
     */
    public UserFavoriteResponse {
        // Compact constructor for validation - record automatically generates constructor
    }
}
