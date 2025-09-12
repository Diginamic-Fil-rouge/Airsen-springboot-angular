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
 * Contrôleur de test pour vérifier le bon fonctionnement de l'API Airsen.
 * 
 * Ce contrôleur fournit des endpoints de test pour valider la configuration
 * de l'application et la documentation Swagger.
 * 
 * @author Airsen Team
 * @version 1.0.0
 * @since 2024-09-12
 */
@RestController
@RequestMapping("/test")
@Tag(name = "Test", description = "Endpoints de test pour vérifier le fonctionnement de l'API")
public class TestController {

    /**
     * GET /test/health
     * 
     * Endpoint de santé basique pour vérifier que l'API fonctionne.
     * 
     * @return ResponseEntity avec le statut de l'API
     */
    @GetMapping("/health")
    @Operation(
        summary = "Vérification de santé de l'API",
        description = "Retourne le statut de santé de l'API avec l'heure actuelle"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "API fonctionnelle",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "Airsen API");
        response.put("version", "1.0.0-SNAPSHOT");
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /test/echo/{message}
     * 
     * Endpoint d'écho pour tester les paramètres de chemin.
     * 
     * @param message message à retourner en écho
     * @return ResponseEntity avec le message d'écho
     */
    @GetMapping("/echo/{message}")
    @Operation(
        summary = "Echo d'un message",
        description = "Retourne le message fourni en écho avec des informations supplémentaires"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Message d'écho retourné avec succès",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<Map<String, Object>> echo(
            @Parameter(description = "Message à retourner en écho", required = true)
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
     * Endpoint de test pour les données POST.
     * 
     * @param data données à traiter
     * @return ResponseEntity avec les données traitées
     */
    @PostMapping("/data")
    @Operation(
        summary = "Test de données POST",
        description = "Accepte des données en POST et retourne une réponse formatée"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Données traitées avec succès",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Données invalides"
        )
    })
    public ResponseEntity<Map<String, Object>> processData(
            @Parameter(description = "Données à traiter", required = true)
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
     * Endpoint d'information sur l'API.
     * 
     * @return ResponseEntity avec les informations de l'API
     */
    @GetMapping("/info")
    @Operation(
        summary = "Informations sur l'API",
        description = "Retourne les informations détaillées sur l'API Airsen"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Informations de l'API retournées",
        content = @Content(schema = @Schema(implementation = Map.class))
    )
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("name", "Airsen API");
        response.put("description", "API REST pour le monitoring de la qualité de l'air en France");
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