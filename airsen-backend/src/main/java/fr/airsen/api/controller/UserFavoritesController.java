package fr.airsen.api.controller;

import fr.airsen.api.dto.request.AddFavoriteRequest;
import fr.airsen.api.dto.response.UserFavoriteResponse;
import fr.airsen.api.exception.ForbiddenException;
import fr.airsen.api.security.UserPrincipal;
import fr.airsen.api.service.UserFavoritesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for user favorite communes management.
 *
 * Provides endpoints for users to manage their favorite communes with
 * proper authentication and authorization. Users can only manage their
 * own favorites, while administrators can access any user's favorites.
 */
@RestController
@RequestMapping("/users/{userId}/favorites")
@Tag(name = "User Favorites", description = "User favorite communes management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserFavoritesController {

    private static final Logger logger = LoggerFactory.getLogger(UserFavoritesController.class);

    private final UserFavoritesService favoritesService;

    public UserFavoritesController(UserFavoritesService favoritesService) {
        this.favoritesService = favoritesService;
    }

    /**
     * Get all favorite communes for a user.
     *
     * Returns a list of the user's favorite communes with complete geographic
     * hierarchy (region → department → commune). Results are ordered by creation
     * date (newest first).
     *
     * Authorization: Users can only access their own favorites, admins can access any user's.
     *
     * @param userId User ID from path variable
     * @param authentication Current authenticated user
     * @return List of user's favorite communes
     */
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get user favorites",
        description = "Retrieve all favorite communes for the authenticated user with complete geographic hierarchy"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Favorites retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserFavoriteResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing JWT token"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - user attempting to access another user's favorites"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User not found"
        )
    })
    public ResponseEntity<List<UserFavoriteResponse>> getUserFavorites(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            Authentication authentication) {

        logger.debug("GET /users/{}/favorites - Authenticated user: {}",
                userId, getUserEmail(authentication));

        validateUserAccess(userId, authentication);
        List<UserFavoriteResponse> favorites = favoritesService.getUserFavorites(userId);

        logger.info("User {} retrieved {} favorites", userId, favorites.size());
        return ResponseEntity.ok(favorites);
    }

    /**
     * Add a commune to user favorites.
     *
     * Adds the specified commune to the user's favorites list with validation:
     * - User must exist
     * - Commune must exist
     * - Maximum 10 favorites per user
     * - No duplicate favorites
     *
     * @param userId User ID from path variable
     * @param request Add favorite request with commune INSEE code
     * @param authentication Current authenticated user
     * @return Created favorite with HTTP 201 Created status
     */
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Add favorite",
        description = "Add a commune to user favorites (maximum 10 per user)"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Favorite added successfully",
            content = @Content(schema = @Schema(implementation = UserFavoriteResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Bad Request - maximum favorites exceeded or invalid INSEE code format"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing JWT token"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - user attempting to modify another user's favorites"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "User or commune not found"
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Conflict - commune already in favorites"
        )
    })
    public ResponseEntity<UserFavoriteResponse> addFavorite(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody AddFavoriteRequest request,
            Authentication authentication) {

        logger.debug("POST /users/{}/favorites - Adding commune {} for user {}",
                userId, request.communeInseeCode(), getUserEmail(authentication));

        validateUserAccess(userId, authentication);
        UserFavoriteResponse response = favoritesService.addFavorite(userId, request);

        logger.info("User {} added favorite: {} ({})",
                userId, response.communeName(), response.communeInseeCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Remove a commune from user favorites.
     *
     * Removes the specified commune from the user's favorites list.
     *
     * @param userId User ID from path variable
     * @param communeInseeCode Commune INSEE code (5 digits)
     * @param authentication Current authenticated user
     * @return HTTP 204 No Content on success
     */
    @DeleteMapping("/{communeInseeCode}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Remove favorite",
        description = "Remove a commune from user favorites"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "204",
            description = "Favorite removed successfully"
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing JWT token"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - user attempting to modify another user's favorites"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Favorite not found"
        )
    })
    public ResponseEntity<Void> removeFavorite(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "Commune INSEE code (5 digits)", required = true, example = "75056")
            @PathVariable String communeInseeCode,
            Authentication authentication) {

        logger.debug("DELETE /users/{}/favorites/{} - User: {}",
                userId, communeInseeCode, getUserEmail(authentication));

        validateUserAccess(userId, authentication);
        favoritesService.removeFavorite(userId, communeInseeCode);

        logger.info("User {} removed favorite: {}", userId, communeInseeCode);
        return ResponseEntity.noContent().build();
    }

    /**
     * Check if a commune is in user's favorites.
     *
     * Fast boolean check used by frontend to show/hide "Add to favorites" button.
     * Does not load full entity data for performance.
     *
     * @param userId User ID from path variable
     * @param communeInseeCode Commune INSEE code (5 digits)
     * @param authentication Current authenticated user
     * @return Boolean response indicating if commune is favorited
     */
    @GetMapping("/{communeInseeCode}/check")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Check if favorited",
        description = "Check if a commune is in user's favorites (for UI state management)"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Check completed successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing JWT token"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - user attempting to access another user's favorites"
        )
    })
    public ResponseEntity<Map<String, Boolean>> checkFavorite(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "Commune INSEE code (5 digits)", required = true, example = "75056")
            @PathVariable String communeInseeCode,
            Authentication authentication) {

        logger.debug("GET /users/{}/favorites/{}/check - User: {}",
                userId, communeInseeCode, getUserEmail(authentication));

        validateUserAccess(userId, authentication);
        boolean isFavorited = favoritesService.isFavorited(userId, communeInseeCode);

        return ResponseEntity.ok(Map.of("isFavorited", isFavorited));
    }

    /**
     * Get favorite count for a user.
     *
     * Returns the number of favorites the user currently has (0-10).
     * Useful for displaying "X/10 favorites" in the UI.
     *
     * @param userId User ID from path variable
     * @param authentication Current authenticated user
     * @return Count response with number of favorites
     */
    @GetMapping("/count")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(
        summary = "Get favorite count",
        description = "Get the number of favorites for a user (0-10)"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Count retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing JWT token"
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - user attempting to access another user's data"
        )
    })
    public ResponseEntity<Map<String, Integer>> getFavoriteCount(
            @Parameter(description = "User ID", required = true)
            @PathVariable Long userId,
            Authentication authentication) {

        logger.debug("GET /users/{}/favorites/count - User: {}",
                userId, getUserEmail(authentication));

        validateUserAccess(userId, authentication);
        int count = favoritesService.getFavoriteCount(userId);

        return ResponseEntity.ok(Map.of(
            "count", count,
            "maximum", 10
        ));
    }

    /**
     * Validates that the authenticated user can access the requested user's favorites.
     *
     * Authorization Rules:
     * - Administrators (ROLE_ADMIN) can access any user's favorites
     * - Regular users (ROLE_USER) can only access their own favorites
     * - Path userId must match authenticated user's ID for regular users
     *
     * @param userId User ID from path variable
     * @param authentication Current authenticated user
     * @throws ForbiddenException if user attempts to access another user's favorites
     */
    private void validateUserAccess(Long userId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            logger.warn("Unauthenticated access attempt to user {} favorites", userId);
            throw new ForbiddenException("Authentication required");
        }

        if (!(authentication.getPrincipal() instanceof UserPrincipal)) {
            logger.warn("Invalid authentication principal type for user {} favorites", userId);
            throw new ForbiddenException("Invalid authentication");
        }

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        // Admin can access any user's favorites
        if (principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            logger.debug("Admin {} accessing user {} favorites", principal.getId(), userId);
            return;
        }

        // Regular user can only access own favorites
        if (!principal.getId().equals(userId)) {
            logger.warn("User {} attempted to access user {} favorites (forbidden)",
                    principal.getId(), userId);
            throw new ForbiddenException("You can only access your own favorites");
        }

        logger.debug("User {} accessing own favorites", userId);
    }

    /**
     * Extracts user email from authentication for logging purposes.
     *
     * @param authentication Current authentication
     * @return User email or "unknown" if not available
     */
    private String getUserEmail(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return ((UserPrincipal) authentication.getPrincipal()).getEmail();
        }
        return "unknown";
    }
}
