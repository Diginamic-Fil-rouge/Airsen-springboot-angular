package fr.airsen.api.controller;

import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.security.JwtTokenProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Test controller to verify proper functioning of the Airsen API.
 *
 * This controller provides test endpoints to validate application
 * configuration and Swagger documentation.
 */
@RestController
@RequestMapping("/test")
@Tag(name = "Test", description = "Test endpoints to verify API functionality")
public class TestController {

    private final JwtTokenProvider jwtTokenProvider;
    private final Environment environment;

    public TestController(JwtTokenProvider jwtTokenProvider, Environment environment) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.environment = environment;
    }

    /**
     * GET /test/health
     * 
     * Basic health endpoint to verify API is working.
     * 
     * @return ResponseEntity with API status
     */
    @GetMapping("/health")
    @Operation(
        summary = "API health check",
        description = "Returns API health status with current time"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "API is functional",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "Hot Reload is WORKING!");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Airsen API");
        response.put("version", "1.0.0-SNAPSHOT");
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/hot-reload-test")
    public ResponseEntity<String> hotReloadTest() {
        return ResponseEntity.ok("Hot reload is working! Timestamp: " + System.currentTimeMillis());
    }

    /**
     * GET /test/echo/{message}
     * 
     * Echo endpoint to test path parameters.
     * 
     * @param message message to return as echo
     * @return ResponseEntity with echo message
     */
    @GetMapping("/echo/{message}")
    @Operation(
        summary = "Echo a message",
        description = "Returns the provided message as echo with additional information"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Echo message returned successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> echo(
            @Parameter(description = "Message to return as echo", required = true)
            @PathVariable String message) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("echo", message);
        response.put("timestamp", LocalDateTime.now());
        response.put("length", message.length());
        
        return ResponseEntity.ok(response);
    }

    /**
     * POST /test/data
     * 
     * Test endpoint for POST data.
     * 
     * @param data data to process
     * @return ResponseEntity with processed data
     */
    @PostMapping("/data")
    @Operation(
        summary = "POST data test",
        description = "Accepts POST data and returns a formatted response"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Data processed successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid data"
        )
    })
    public ResponseEntity<Map<String, Object>> processData(
            @Parameter(description = "Data to process", required = true)
            @RequestBody Map<String, Object> data) {
        
        Map<String, Object> response = new HashMap<>();
        response.put("received", data);
        response.put("processed", true);
        response.put("timestamp", LocalDateTime.now());
        response.put("dataKeys", data.keySet());
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /test/info
     * 
     * API information endpoint.
     * 
     * @return ResponseEntity with API information
     */
    @GetMapping("/info")
    @Operation(
        summary = "API information",
        description = "Returns detailed information about the Airsen API"
    )
    @ApiResponse(
        responseCode = "200",
        description = "API information returned",
        content = @Content(schema = @Schema(implementation = Map.class))
    )
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Airsen API");
        response.put("description", "REST API for air quality monitoring in France");
        response.put("version", "1.0.0-SNAPSHOT");
        response.put("springBoot", "3.2.0");
        response.put("java", System.getProperty("java.version"));
        response.put("profiles", new String[]{"dev"});
        response.put("features", new String[]{
            "Air Quality Monitoring",
            "Weather Data",
            "Geographic Data",
            "User Management",
            "Forum System",
            "Export System"
        });

        return ResponseEntity.ok(response);
    }

    /**
     * GET /test/generate-token
     *
     * Generates a test JWT token for development/testing purposes.
     * ONLY WORKS IN DEV ENVIRONMENT - Returns 403 in production.
     *
     * Query parameters:
     * - email: User email (default: test@airsen.fr)
     * - role: User role - USER, ADMIN, or VISITOR (default: USER)
     *
     * @param email User email to encode in token
     * @param role User role for authorization
     * @return ResponseEntity with generated JWT token
     */
    @GetMapping("/generate-token")
    @Operation(
        summary = "Generate test JWT token",
        description = """
            Generates a test JWT token for development and testing purposes.

            **SECURITY WARNING**: This endpoint only works in DEV environment.
            It will return 403 Forbidden in production environments.

            Use this token to test protected endpoints without needing to login.

            Examples:
            - GET /test/generate-token (generates USER token for test@airsen.fr)
            - GET /test/generate-token?email=admin@airsen.fr&role=ADMIN
            - GET /test/generate-token?email=sarah@airsen.fr&role=ADMIN
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Token generated successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Forbidden - Only available in DEV environment"
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid role parameter"
        )
    })
    public ResponseEntity<Map<String, Object>> generateTestToken(
            @Parameter(description = "User email for token", example = "sarah@airsen.fr")
            @RequestParam(defaultValue = "test@airsen.fr") String email,
            @Parameter(description = "User role (USER, ADMIN, VISITOR)", example = "ADMIN")
            @RequestParam(defaultValue = "USER") String role) {

        // SECURITY: Only allow in DEV environment
        String activeProfile = environment.getProperty("spring.profiles.active", "");
        if (!activeProfile.contains("dev") && !activeProfile.contains("test")) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Forbidden");
            errorResponse.put("message", "Token generation only available in DEV/TEST environments");
            errorResponse.put("activeProfile", activeProfile);
            return ResponseEntity.status(403).body(errorResponse);
        }

        try {
            // Parse role
            UserRole userRole = UserRole.valueOf(role.toUpperCase());

            // Generate token using JwtTokenProvider
            String token = jwtTokenProvider.generateAccessToken(email, userRole);

            // Prepare response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("tokenType", "Bearer");
            response.put("expiresIn", 86400000); // 24 hours in milliseconds
            response.put("user", Map.of(
                "email", email,
                "role", userRole.name()
            ));
            response.put("usage", "Authorization: Bearer " + token);
            response.put("timestamp", LocalDateTime.now());
            response.put("environment", activeProfile);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Invalid role
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid role");
            errorResponse.put("message", "Role must be one of: USER, ADMIN, VISITOR");
            errorResponse.put("providedRole", role);
            errorResponse.put("validRoles", new String[]{"USER", "ADMIN", "VISITOR"});
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}