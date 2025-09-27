package fr.airsen.api.controller;

import fr.airsen.api.dto.auth.AuthResponse;
import fr.airsen.api.dto.auth.LoginRequest;
import fr.airsen.api.dto.auth.RegisterRequest;
import fr.airsen.api.dto.auth.RefreshTokenRequest;
import fr.airsen.api.entity.User;
import fr.airsen.api.repository.UserRepository;
import fr.airsen.api.service.AuthService;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Enumeration;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Authentication controller for user login, registration, and token management.
 * 
 * This controller provides REST endpoints for all authentication operations
 * in the Airsen application. It handles user registration, login, token refresh,
 * and logout functionality with comprehensive error handling and security logging.
 * 
 * Security Features:
 * - JWT token-based authentication
 * - Role-based authorization support
 * - Comprehensive input validation
 * - Security logging for monitoring
 * - Proper error handling without information leakage
 * 
 * Endpoints:
 * - POST /auth/login - User authentication
 * - POST /auth/register - User registration
 * - POST /auth/refresh - Token refresh
 * - POST /auth/logout - User logout
 * - GET /auth/me - Current user information
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User authentication and registration endpoints")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;
    
    @Autowired
    private UserRepository userRepository;

    /**
     * Authenticates user credentials and returns JWT tokens.
     * 
     * This endpoint validates user credentials and generates JWT access and refresh tokens
     * for subsequent API requests. It supports role-based authentication and provides
     * detailed user information in the response.
     * 
     * @param loginRequest user login credentials
     * @return ResponseEntity with authentication response and JWT tokens
     */
    @PostMapping("/login")
    @Operation(
        summary = "User login", 
        description = "Authenticate user credentials and receive JWT tokens for API access"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Authentication successful", 
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<?> login(
            @Valid @org.springframework.web.bind.annotation.RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Login attempt for user: {}", loginRequest.getNormalizedEmail());
            
            AuthResponse authResponse = authService.authenticate(loginRequest);
            
            logger.info("Login successful for user: {} with role: {}", 
                       authResponse.userEmail(), authResponse.userRole());
            
            return ResponseEntity.ok(authResponse);
            
        } catch (BadCredentialsException e) {
            logger.warn("Login failed for user: {} - Invalid credentials", loginRequest.getNormalizedEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse("Invalid email or password", "INVALID_CREDENTIALS"));
                
        } catch (IllegalArgumentException e) {
            logger.warn("Login failed - Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Invalid request data", "INVALID_REQUEST"));
                
        } catch (Exception e) {
            logger.error("Login error for user: {} - {}", loginRequest.getNormalizedEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Authentication service error", "AUTH_SERVICE_ERROR"));
        }
    }

    /**
     * Registers a new user account and returns immediate authentication.
     * 
     * This endpoint creates a new user account with secure password hashing
     * and role assignment. Upon successful registration, it immediately returns
     * JWT tokens for seamless user experience.
     * 
     * @param registerRequest user registration data
     * @return ResponseEntity with authentication response and JWT tokens
     */
    @PostMapping("/register")
    @Operation(
        summary = "User registration", 
        description = "Create a new user account and receive immediate authentication tokens"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201", 
            description = "Registration successful", 
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "409", 
            description = "Email already registered",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid registration data",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<?> register(
            @Valid @org.springframework.web.bind.annotation.RequestBody RegisterRequest registerRequest) {
        try {
            logger.info("Registration attempt for user: {}", registerRequest.getNormalizedEmail());
            
            AuthResponse authResponse = authService.register(registerRequest);
            
            logger.info("Registration successful for user: {} with role: {}", 
                       authResponse.userEmail(), authResponse.userRole());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Registration failed - Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Invalid registration data", "INVALID_REQUEST"));
                
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("already registered")) {
                logger.warn("Registration failed - Email already exists: {}", registerRequest.getNormalizedEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(createErrorResponse("Email address is already registered", "EMAIL_EXISTS"));
            } else if (e.getMessage() != null && e.getMessage().contains("admin accounts")) {
                logger.warn("Registration failed - Admin registration attempt: {}", registerRequest.getNormalizedEmail());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Admin accounts cannot be self-registered", "ADMIN_REGISTRATION_FORBIDDEN"));
            } else {
                logger.error("Registration error for user: {} - {}", registerRequest.getNormalizedEmail(), e.getMessage(), e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Registration failed", "REGISTRATION_ERROR"));
            }
                
        } catch (Exception e) {
            logger.error("Unexpected registration error for user: {} - {}", 
                        registerRequest.getNormalizedEmail(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Registration service error", "REGISTRATION_SERVICE_ERROR"));
        }
    }

    /**
     * Refreshes JWT access token using a valid refresh token.
     * 
     * This endpoint allows clients to obtain new access tokens without
     * requiring user re-authentication, providing a seamless user experience
     * while maintaining security.
     * 
     * @param refreshRequest refresh token request
     * @return ResponseEntity with new access token
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh access token", 
        description = "Obtain a new access token using a valid refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Token refresh successful", 
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Invalid or expired refresh token",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<?> refresh(@Valid @RequestBody RefreshTokenRequest refreshRequest) {
        try {
            logger.debug("Token refresh attempt");
            logger.debug("Received RefreshTokenRequest: {}", refreshRequest);
            logger.debug("Raw refreshToken value: '{}'", refreshRequest.refreshToken());
            logger.debug("Raw token value: '{}'", refreshRequest.getToken());
            logger.debug("Clean refreshToken value: '{}'", refreshRequest.getCleanRefreshToken());
            
            AuthResponse authResponse = authService.refreshAccessToken(refreshRequest);
            
            logger.info("Token refresh successful for user: {}", authResponse.userEmail());
            
            return ResponseEntity.ok(authResponse);
            
        } catch (BadCredentialsException e) {
            logger.warn("Token refresh failed - Invalid token: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(createErrorResponse("Invalid or expired refresh token", "INVALID_REFRESH_TOKEN"));
                
        } catch (IllegalArgumentException e) {
            logger.warn("Token refresh failed - Invalid request data: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(createErrorResponse("Invalid refresh token request", "INVALID_REQUEST"));
                
        } catch (Exception e) {
            logger.error("Token refresh error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Token refresh service error", "REFRESH_SERVICE_ERROR"));
        }
    }

    /**
     * Logs out user by invalidating refresh token.
     * 
     * This endpoint handles user logout by invalidating the refresh token.
     * Note: JWT access tokens are stateless and handled client-side.
     * 
     * @param refreshRequest refresh token to invalidate
     * @return ResponseEntity with logout confirmation
     */
    @PostMapping("/logout")
    @Operation(
        summary = "User logout", 
        description = "Logout user and invalidate refresh token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "Logout successful",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "400", 
            description = "Invalid request data",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<?> logout(@Valid @RequestBody RefreshTokenRequest refreshRequest) {
        try {
            logger.debug("Logout attempt");
            
            boolean logoutSuccessful = authService.logout(refreshRequest.getCleanRefreshToken());
            
            if (logoutSuccessful) {
                logger.info("Logout successful");
                return ResponseEntity.ok(createSuccessResponse("Logout successful", "LOGOUT_SUCCESS"));
            } else {
                return ResponseEntity.badRequest()
                    .body(createErrorResponse("Logout failed", "LOGOUT_FAILED"));
            }
            
        } catch (Exception e) {
            logger.error("Logout error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Logout service error", "LOGOUT_SERVICE_ERROR"));
        }
    }


    /**
     * Gets current user information from JWT token.
     * 
     * This endpoint extracts user information from the JWT token
     * and returns current user details. Requires valid authentication.
     * 
     * @return ResponseEntity with current user information
     */
    @GetMapping("/me")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "Get current user", 
        description = "Retrieve current authenticated user information"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "User information retrieved successfully",
            content = @Content(schema = @Schema(implementation = Map.class))
        ),
        @ApiResponse(
            responseCode = "401", 
            description = "Authentication required",
            content = @Content(schema = @Schema(implementation = Map.class))
        )
    })
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "User not authenticated");
            errorResponse.put("status", "UNAUTHORIZED");
            errorResponse.put("timestamp", LocalDateTime.now());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }
        
        try {
            // Extract user information from JWT token
            String email = authentication.getName();
            User user = userRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("email", user.getEmail());
            userInfo.put("firstName", user.getFirstName());
            userInfo.put("lastName", user.getLastName());
            userInfo.put("role", user.getRole().name());
            userInfo.put("createdAt", user.getCreatedAt());
            
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            logger.error("Error retrieving current user information: {}", e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error retrieving user information");
            errorResponse.put("status", "INTERNAL_SERVER_ERROR");
            errorResponse.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Health check endpoint for authentication service.
     * 
     * @return ResponseEntity with service status
     */
    @GetMapping("/health")
    @Operation(
        summary = "Authentication service health check", 
        description = "Check if authentication service is operational"
    )
    @ApiResponse(
        responseCode = "200", 
        description = "Service is healthy",
        content = @Content(schema = @Schema(implementation = Map.class))
    )
    public ResponseEntity<?> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("service", "Authentication Service");
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("version", "1.0.0");
        
        return ResponseEntity.ok(health);
    }

    /**
     * Creates a standardized error response.
     * 
     * @param message error message
     * @param code error code
     * @return error response map
     */
    private Map<String, Object> createErrorResponse(String message, String code) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("code", code);
        errorResponse.put("timestamp", LocalDateTime.now());
        return errorResponse;
    }

    /**
     * Creates a standardized success response.
     * 
     * @param message success message
     * @param code success code
     * @return success response map
     */
    private Map<String, Object> createSuccessResponse(String message, String code) {
        Map<String, Object> successResponse = new HashMap<>();
        successResponse.put("success", true);
        successResponse.put("message", message);
        successResponse.put("code", code);
        successResponse.put("timestamp", LocalDateTime.now());
        return successResponse;
    }
    
}