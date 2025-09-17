package fr.airsen.api.controller;

import fr.airsen.api.dto.auth.*;
import fr.airsen.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * 
 * This controller provides endpoints for user authentication including
 * registration, login, logout, and token refresh operations. It follows
 * the API specification defined in api-endpoints.md and provides
 * comprehensive OpenAPI documentation.
 * 
 * Security Features:
 * - Input validation using Bean Validation
 * - Secure error handling without information leakage
 * - JWT token-based stateless authentication
 * - Rate limiting headers (to be implemented)
 * - Comprehensive logging for security monitoring
 * 
 * Base Path: /api/v1/auth
 * Content-Type: application/json
 * Authentication: None required for these endpoints
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User authentication operations")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private AuthService authService;

    /**
     * POST /auth/register
     * 
     * Creates a new user account with the provided information.
     * Returns user data and JWT token for immediate authentication.
     * 
     * @param registerRequest user registration data
     * @return ResponseEntity with AuthResponse containing user info and token
     */
    @PostMapping("/register")
    @Operation(
        summary = "Register a new user account",
        description = "Creates a new user account with email, password, and personal information. " +
                     "Returns user data and JWT token for immediate authentication."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "User registered successfully",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Email already exists or validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "422",
            description = "Validation errors in request data",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        logger.info("Registration request received for email: {}", 
                   registerRequest.getNormalizedEmail());

        try {
            // Check if email already exists
            if (authService.isEmailRegistered(registerRequest.getEmail())) {
                logger.warn("Registration failed: email already exists: {}", 
                           registerRequest.getNormalizedEmail());
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.builder()
                        .error("Email already exists")
                        .message("An account with this email address already exists")
                        .build());
            }

            // Register the user
            AuthResponse response = authService.registerUser(registerRequest);
            
            logger.info("User registration successful for: {}", 
                       registerRequest.getNormalizedEmail());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Registration validation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                    .error("Validation failed")
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            logger.error("Unexpected error during registration", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                    .error("Registration failed")
                    .message("Unable to create account at this time")
                    .build());
        }
    }

    /**
     * POST /auth/login
     * 
     * Authenticates a user with email and password credentials.
     * Returns user data and JWT token for authenticated access.
     * 
     * @param loginRequest user login credentials
     * @return ResponseEntity with AuthResponse containing user info and token
     */
    @PostMapping("/login")
    @Operation(
        summary = "Authenticate user login",
        description = "Validates user credentials and returns user data with JWT token " +
                     "for authenticated access to protected resources."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Authentication successful",
            content = @Content(schema = @Schema(implementation = AuthResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Invalid credentials",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "403",
            description = "Account suspended or inactive",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Authentication request received for email: {}", 
                   loginRequest.getNormalizedEmail());

        try {
            // Authenticate the user
            AuthResponse response = authService.authenticateUser(loginRequest);
            
            logger.info("Authentication successful for: {}", 
                       loginRequest.getNormalizedEmail());
            
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            logger.warn("Authentication failed for: {} - {}", 
                       loginRequest.getNormalizedEmail(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.builder()
                    .error("Invalid credentials")
                    .message("Email or password is incorrect")
                    .build());
        } catch (Exception e) {
            logger.error("Unexpected error during authentication for: {}", 
                        loginRequest.getNormalizedEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                    .error("Authentication failed")
                    .message("Unable to authenticate at this time")
                    .build());
        }
    }

    /**
     * POST /auth/logout
     * 
     * Logs out the current user by invalidating their JWT token.
     * In a stateless JWT implementation, this is primarily client-side.
     * 
     * @param request HTTP request containing the JWT token
     * @return ResponseEntity with success message
     */
    @PostMapping("/logout")
    @Operation(
        summary = "User logout",
        description = "Logs out the current user. In a stateless JWT implementation, " +
                     "this primarily involves client-side token disposal."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Successfully logged out",
            content = @Content(schema = @Schema(implementation = MessageResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - invalid or missing token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<?> logout(HttpServletRequest request) {
        logger.debug("Logout request received");

        try {
            // Extract JWT token from request
            String token = extractTokenFromRequest(request);
            
            // Process logout
            MessageResponse response = authService.logoutUser(token);
            
            logger.debug("Logout processed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.warn("Error during logout processing: {}", e.getMessage());
            // Return success anyway to avoid information leakage
            return ResponseEntity.ok(MessageResponse.success("Successfully logged out"));
        }
    }

    /**
     * POST /auth/refresh
     * 
     * Refreshes a JWT token to extend its validity period.
     * Validates the existing token and returns a new one.
     * 
     * @param request HTTP request containing the JWT token to refresh
     * @return ResponseEntity with new token information
     */
    @PostMapping("/refresh")
    @Operation(
        summary = "Refresh JWT token",
        description = "Validates an existing JWT token and returns a new token " +
                     "with extended expiration time."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Token refreshed successfully",
            content = @Content(schema = @Schema(implementation = RefreshTokenResponse.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Token expired, invalid, or missing",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        logger.debug("Token refresh request received");

        try {
            // Extract JWT token from request
            String token = extractTokenFromRequest(request);
            
            if (token == null) {
                logger.warn("Token refresh failed: no token provided");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ErrorResponse.builder()
                        .error("Token required")
                        .message("JWT token is required for refresh")
                        .build());
            }

            // Refresh the token
            RefreshTokenResponse response = authService.refreshToken(token);
            
            logger.debug("Token refresh successful");
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            logger.warn("Token refresh failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.builder()
                    .error("Token expired or invalid")
                    .message("Unable to refresh token")
                    .build());
        } catch (Exception e) {
            logger.error("Unexpected error during token refresh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                    .error("Token refresh failed")
                    .message("Unable to refresh token at this time")
                    .build());
        }
    }

    /**
     * Extracts JWT token from the Authorization header.
     * 
     * Expected header format: "Authorization: Bearer <jwt-token>"
     * 
     * @param request HTTP request
     * @return JWT token string without "Bearer " prefix, or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * Data Transfer Object for error responses.
     * 
     * Provides a consistent error format across all authentication endpoints.
     */
    public static class ErrorResponse {
        private String error;
        private String message;
        private String timestamp;
        private String path;

        public ErrorResponse() {
            this.timestamp = java.time.Instant.now().toString();
        }

        public ErrorResponse(String error, String message) {
            this();
            this.error = error;
            this.message = message;
        }

        // Getters and setters
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String error;
            private String message;
            private String path;

            public Builder error(String error) {
                this.error = error;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder path(String path) {
                this.path = path;
                return this;
            }

            public ErrorResponse build() {
                ErrorResponse response = new ErrorResponse(error, message);
                response.setPath(path);
                return response;
            }
        }
    }
}