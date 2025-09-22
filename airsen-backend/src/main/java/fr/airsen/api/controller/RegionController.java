package fr.airsen.api.controller;

import fr.airsen.api.dto.DepartmentDTO;
import fr.airsen.api.dto.RegionDTO;
import fr.airsen.api.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for managing French administrative regions.
 * 
 * Provides endpoints for accessing region data and related departments
 * following the official French administrative hierarchy.
 */
@RestController
@RequestMapping("/regions")
@Tag(name = "Regions", description = "French administrative regions management")
public class RegionController {

    private static final Logger log = LoggerFactory.getLogger(RegionController.class);

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    /**
     * GET /regions
     * 
     * Retrieves all French regions.
     */
    @GetMapping
    @Operation(
        summary = "Get all regions",
        description = "Retrieves all French administrative regions"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Regions retrieved successfully")
    })
    public ResponseEntity<List<RegionDTO>> getAllRegions() {
        log.info("Received request for all regions");
        
        List<RegionDTO> regions = regionService.getAllRegions();
        log.info("Successfully retrieved {} regions", regions.size());
        
        return ResponseEntity.ok(regions);
    }

    /**
     * GET /regions/{regionId}/departments
     * 
     * Retrieves all departments for a specific region.
     */
    @GetMapping("/{regionId}/departments")
    @Operation(
        summary = "Get departments by region",
        description = "Retrieves all departments belonging to a specific region"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Departments retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Region not found")
    })
    public ResponseEntity<List<DepartmentDTO>> getDepartmentsByRegion(
            @Parameter(description = "Region identifier", example = "11")
            @PathVariable @Valid @Positive Long regionId) {
        
        log.info("Received request for departments in region: {}", regionId);
        
        List<DepartmentDTO> departments = regionService.getDepartmentsByRegion(regionId);
        log.info("Successfully retrieved {} departments for region: {}", departments.size(), regionId);
        
        return ResponseEntity.ok(departments);
    }
}
