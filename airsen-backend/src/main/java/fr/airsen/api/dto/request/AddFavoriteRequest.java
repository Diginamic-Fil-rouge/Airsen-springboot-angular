package fr.airsen.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for adding a commune to user favorites.
 *
 * This DTO validates the commune INSEE code format before processing
 * the favorite addition request.
 *
 * Business Rules:
 * - INSEE code must be exactly 5 digits
 * - User can have maximum 10 favorites (validated in service layer)
 * - Duplicate favorites are prevented (validated in service layer)
 */
public record AddFavoriteRequest(

    @NotBlank(message = "Commune INSEE code is required")
    @Pattern(regexp = "^\\d{5}$", message = "Invalid INSEE code format (must be exactly 5 digits)")
    String communeInseeCode
) {

    /**
     * Creates a new AddFavoriteRequest with the specified commune INSEE code.
     *
     * @param communeInseeCode French commune INSEE code (5 digits)
     *                         Example: "75056" for Paris, "69123" for Lyon
     */
    public AddFavoriteRequest {
        // Compact constructor for validation - record automatically generates constructor
    }
}
