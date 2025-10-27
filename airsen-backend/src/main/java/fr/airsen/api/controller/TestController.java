package fr.airsen.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
}