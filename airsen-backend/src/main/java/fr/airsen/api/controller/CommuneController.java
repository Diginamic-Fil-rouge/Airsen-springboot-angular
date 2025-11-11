package fr.airsen.api.controller;

import fr.airsen.api.dto.CommuneDTO;
import fr.airsen.api.dto.response.CommuneDetailResponse;
import fr.airsen.api.service.CommuneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing French administrative communes.
 *
 * Provides endpoints for accessing commune data both by department
 * and through global search functionality.
 */
@RestController
@Tag(name = "Communes", description = "French administrative communes management")
@SecurityRequirement(name = "bearerAuth")
public class CommuneController {

    private static final Logger log = LoggerFactory.getLogger(CommuneController.class);

    private final CommuneService communeService;

    public CommuneController(CommuneService communeService) {
        this.communeService = communeService;
    }

    /**
     * GET /departments/{departmentId}/communes
     *
     * Lists all communes belonging to a specific department.
     *
     * Validates department existence before querying communes to distinguish
     * between "department has no communes" (HTTP 200) and "department doesn't exist" (HTTP 404).
     */
    @GetMapping("/departments/{departmentId}/communes")
    @Operation(
        summary = "Get communes by department",
        description = "Retrieves paginated list of communes for a specific department with optional search. " +
                     "Returns 404 if department doesn't exist, or 200 with empty list if department has no communes."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Communes retrieved successfully (may be empty list)"),
        @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<List<CommuneDTO>> getCommunesByDepartment(
            @Parameter(description = "Department identifier", example = "75")
            @PathVariable @Valid @Positive Long departmentId,
            @Parameter(description = "Page number (0-based)", example = "0")
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @Parameter(description = "Page size", example = "20")
            @RequestParam(defaultValue = "20") @Positive int size,
            @Parameter(description = "Optional search term", example = "Paris")
            @RequestParam(required = false) @Size(min = 2, max = 50) String search) {

        log.info("Received request for communes in department: {} (page: {}, size: {}, search: '{}')",
                departmentId, page, size, search);

        List<CommuneDTO> communes = communeService.getCommunesByDepartment(departmentId, page, size, search);
        log.info("Successfully retrieved {} communes for department: {}", communes.size(), departmentId);

        return ResponseEntity.ok(communes);
    }

    /**
     * GET /communes/search
     *
     * Global search for communes across all departments.
     */
    @GetMapping("/communes/search")
    @Operation(
        summary = "Search communes globally",
        description = "Search for communes across all French departments using name or INSEE code. " +
                     "Supports partial matching for both fields (case-insensitive for names). " +
                     "Examples: 'Paris' (by name), '75056' (exact INSEE code), '750' (partial code - all Paris arrondissements), 'Mar' (finds Marseille, Marignane, etc.)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Communes found successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters")
    })
    public ResponseEntity<List<CommuneDTO>> searchCommunes(
            @Parameter(
                description = "Search query - supports commune name (partial, case-insensitive) or INSEE code (partial match)",
                example = "Paris"
            )
            @RequestParam("q") @Valid @Size(min = 2, max = 50) String query,
            @Parameter(description = "Maximum number of results", example = "10")
            @RequestParam(defaultValue = "10") @Positive int limit) {

        log.info("Received global search request for communes: '{}' (limit: {})", query, limit);

        List<CommuneDTO> communes = communeService.searchCommunes(query, limit);
        log.info("Successfully found {} communes matching query: '{}'", communes.size(), query);

        return ResponseEntity.ok(communes);
    }

    /**
     * GET /communes/with-coordinates
     *
     * Returns all communes that have valid coordinates for map display.
     * Used by interactive map component to render commune markers.
     */
 @GetMapping("/communes/with-coordinates")
@Operation(
    summary = "Get communes with coordinates, optionally filtered by minimum population",
    description = "Returns communes that have valid coordinates for map display. " +
                 "Use minPopulation parameter for progressive loading (e.g., 50000 for initial zoom 6 view). " +
                 "Performance optimization: Filters server-side to reduce payload size."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Communes retrieved successfully"),
    @ApiResponse(responseCode = "400", description = "Invalid minPopulation parameter")
})
public ResponseEntity<List<CommuneDTO>> getCommunesWithCoordinates(
    @Parameter(
        description = "Minimum population threshold for filtering (optional). " +
                     "Examples: 50000 for zoom 6 (~200 communes), 10000 for zoom 10 (~1000 communes), " +
                     "omit for all communes",
        example = "50000"
    )
    @RequestParam(required = false) @Min(0) Integer minPopulation
) {
    log.info("GET /communes/with-coordinates (minPopulation: {})", minPopulation);

    List<CommuneDTO> communes = minPopulation != null
        ? communeService.getCommunesWithCoordinates(minPopulation)
        : communeService.getAllCommunesWithCoordinates();

    log.info("Retrieved {} communes for map display", communes.size());
    return ResponseEntity.ok(communes);
}

    /**
     * GET /communes/{inseeCode}/detail
     *
     * Retrieves comprehensive commune information enriched with real-time environmental data.
     */
    @GetMapping("/communes/{inseeCode}/detail")
    @Operation(
        summary = "Get commune detail with environmental data",
        description = "Retrieves comprehensive commune information including geographic data, " +
                     "current air quality from ATMO France, and current weather from Open-Meteo. " +
                     "External API calls are executed in parallel. Gracefully handles API failures " +
                     "by returning partial data (commune information is always available)."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Commune detail retrieved successfully with available environmental data"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid INSEE code format (must be exactly 5 digits)"
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Commune not found with the provided INSEE code"
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during data aggregation"
        )
    })
    public ResponseEntity<CommuneDetailResponse> getCommuneDetail(
            @Parameter(
                description = "INSEE code of the commune (5-digit unique identifier). " +
                             "Examples: '75056' (Paris), '13055' (Marseille), '69123' (Lyon)",
                example = "75056",
                required = true
            )
            @PathVariable
            @Valid
            @Pattern(regexp = "\\d{5}", message = "INSEE code must be exactly 5 digits")
            String inseeCode) {

        log.info("Received request for commune detail with environmental data: {}", inseeCode);

        CommuneDetailResponse response = communeService.getCommuneDetailWithEnvironmentalData(inseeCode);

        log.info("Successfully retrieved commune detail for {}: {} with air quality={}, weather={}",
            inseeCode,
            response.name(),
            response.airQuality() != null ? "available" : "unavailable",
            response.weather() != null ? "available" : "unavailable"
        );

        return ResponseEntity.ok(response);
    }
}
