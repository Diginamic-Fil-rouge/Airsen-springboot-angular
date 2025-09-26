package fr.airsen.api.controller;

import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.dto.request.UpdatePasswordRequest;
import fr.airsen.api.dto.request.UpdateUserProfileRequest;
import fr.airsen.api.security.UserPrincipal;
import fr.airsen.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * User Controller.
 *
 * Controller for managing authenticated user profile operations.
 * All endpoints require JWT authentication and operate on the current user's data.
 * 
 * Security Notes:
 * - All endpoints require Bearer token authentication
 * - Operations are restricted to the authenticated user's own data
 * - Admin operations are handled by a separate AdminController
 */
@RestController
@RequestMapping("/users")
@Tag(name = "User Management", description = "User profile management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get current user's profile.
     * 
     * Retrieves the profile information for the authenticated user.
     * 
     * @return UserDTO containing current user's profile information
     */
    @GetMapping("/profile")
    @Operation(summary = "Get current user profile", 
               description = "Retrieve the profile information for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserDTO> getCurrentUserProfile() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Check if user is authenticated with a valid UserPrincipal
        if (authentication == null || !authentication.isAuthenticated() || 
            !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return ResponseEntity.status(401).build();
        }
        
        Long userId = getUserIdFromAuthentication(authentication);
        UserDTO userProfile = userService.getCurrentUserProfile(userId);
        return ResponseEntity.ok(userProfile);
    }

    /**
     * Update current user's profile.
     * 
     * Updates profile information (firstName, lastName) for the authenticated user.
     * 
     * @param request Update request containing new profile information
     * @return Success message
     */
    @PutMapping("/profile")
    @Operation(summary = "Update current user profile", 
               description = "Update profile information for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request data"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<Map<String, String>> updateCurrentUserProfile(
            @Valid @RequestBody UpdateUserProfileRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Check if user is authenticated with a valid UserPrincipal
        if (authentication == null || !authentication.isAuthenticated() || 
            !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return ResponseEntity.status(401).build();
        }
        
        Long userId = getUserIdFromAuthentication(authentication);
        userService.updateCurrentUserProfile(userId, request);
        
        return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
    }

    /**
     * Update current user's password.
     * 
     * Changes the password for the authenticated user after verifying the current password.
     * 
     * @param request Update request containing current and new passwords
     * @return Success message
     */
    @PutMapping("/password")
    @Operation(summary = "Update current user password", 
               description = "Change password for the authenticated user")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Password updated successfully"),
        @ApiResponse(responseCode = "400", description = "Current password incorrect"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<Map<String, String>> updateCurrentUserPassword(
            @Valid @RequestBody UpdatePasswordRequest request) {
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Check if user is authenticated with a valid UserPrincipal
        if (authentication == null || !authentication.isAuthenticated() || 
            !(authentication.getPrincipal() instanceof UserPrincipal)) {
            return ResponseEntity.status(401).build();
        }
        
        Long userId = getUserIdFromAuthentication(authentication);
        
        try {
            userService.updateCurrentUserPassword(userId, request);
            return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Current password incorrect"));
        }
    }

    /**
     * Helper method to extract user ID from authentication context.
     * 
     * Extracts the user ID from the UserPrincipal in the Spring Security context.
     * The UserPrincipal is set during JWT authentication and contains the authenticated user's information.
     * 
     * @param authentication Spring Security authentication object
     * @return User ID from the authenticated token
     * @throws IllegalStateException if authentication principal is not a UserPrincipal
     */
    private Long getUserIdFromAuthentication(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.getId();
        }
        
        throw new IllegalStateException("Authentication principal is not a UserPrincipal: " + 
                                       principal.getClass().getSimpleName());
    }
}