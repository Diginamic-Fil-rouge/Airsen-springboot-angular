package fr.airsen.api.controller;

import fr.airsen.api.dto.DepartmentDTO;
import fr.airsen.api.service.DepartmentService;

import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing French administrative departments.
 * 
 * Provides endpoints for accessing department data and information
 * within the French administrative hierarchy.
 */
@RestController
@RequestMapping("/departments")
@Tag(name = "Departments", description = "French administrative departments management")
@SecurityRequirement(name = "bearerAuth")
public class DepartmentController {

    private static final Logger log = LoggerFactory.getLogger(DepartmentController.class);

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * GET /departments
     * 
     * Retrieves all departments.
     */
    @GetMapping
    @Operation(
        summary = "Get all departments",
        description = "Retrieves a list of all French administrative departments"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Departments retrieved successfully")
    })
    public ResponseEntity<List<DepartmentDTO>> getAllDepartments() {
        
        log.info("Received request for all departments");
        
        List<DepartmentDTO> departments = departmentService.getAllDepartments();
        log.info("Successfully retrieved {} departments", departments.size());
        
        return ResponseEntity.ok(departments);
    }

    /**
     * GET /departments/{id}
     * 
     * Retrieves a specific department by its identifier.
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Get department by ID",
        description = "Retrieves a specific department using its unique identifier"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Department retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Department not found")
    })
    public ResponseEntity<DepartmentDTO> getDepartmentById(
            @Parameter(description = "Department unique identifier", example = "75")
            @PathVariable @Valid @Positive Long id) {
        
        log.info("Received request for department with ID: {}", id);
        
        DepartmentDTO department = departmentService.getDepartmentById(id);
        log.info("Successfully retrieved department: {} ({})", department.getName(), department.getDepartmentCode());
        
        return ResponseEntity.ok(department);
    }
}
