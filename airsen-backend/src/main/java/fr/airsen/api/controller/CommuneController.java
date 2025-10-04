package fr.airsen.api.controller;

import fr.airsen.api.dto.CommuneDTO;
import fr.airsen.api.service.CommuneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
     */
    @GetMapping("/departments/{departmentId}/communes")
    @Operation(
        summary = "Get communes by department",
        description = "Retrieves paginated list of communes for a specific department with optional search"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Communes retrieved successfully"),
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
}
