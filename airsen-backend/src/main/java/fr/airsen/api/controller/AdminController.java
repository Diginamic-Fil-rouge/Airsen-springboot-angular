package fr.airsen.api.controller;

import fr.airsen.api.dto.auth.UserDTO;
import fr.airsen.api.entity.enums.UserRole;
import fr.airsen.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin Controller.
 *
 * Controller for administrative operations - listing users and suspending accounts.
 * Admins cannot modify user profiles, only view and suspend accounts.
 * 
 * Available Operations:
 * - List all users with pagination and search
 * - Suspend user accounts
 * 
 * Security Notes:
 * - All endpoints require Bearer token authentication
 * - All endpoints require ADMIN role (ROLE_ADMIN)
 * - Audit logging should be implemented for admin actions
 * - Profile modifications are not allowed - users manage their own profiles
 */
@RestController
@RequestMapping("/admin")
@Tag(name = "Admin Management", description = "Administrative endpoints for user account management")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserService userService;

    /**
     * Get all users (admin only).
     *
     * Retrieves a paginated list of all users in the system with optional search functionality.
     * Matches the API specification exactly.
     *
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @param sortBy Field to sort by (default: createdAt)
     * @param sortDir Sort direction (asc/desc, default: desc)
     * @param search Optional search term for filtering users
     * @param role Optional role filter
     * @param isActive Optional active status filter
     * @return Paginated list of users
     */
    @GetMapping("/users")
    @Operation(summary = "List all users",
               description = "Retrieve a paginated list of all users in the system with optional search and filters (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Users retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required")
    })
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @Parameter(description = "Page number") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(required = false, defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction") @RequestParam(required = false, defaultValue = "desc") String sortDir,
            @Parameter(description = "Search term") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by role") @RequestParam(required = false) UserRole role,
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean isActive) {

        Page<UserDTO> usersPage = userService.findUsersWithPagination(
            page, size, sortBy, sortDir, search, role, isActive
        );

        // Return in paginated format as per API specification
        Map<String, Object> response = new HashMap<>();
        response.put("content", usersPage.getContent());
        response.put("totalElements", usersPage.getTotalElements());
        response.put("totalPages", usersPage.getTotalPages());
        response.put("currentPage", usersPage.getNumber());
        response.put("pageSize", usersPage.getSize());
        response.put("first", usersPage.isFirst());
        response.put("last", usersPage.isLast());

        return ResponseEntity.ok(response);
    }

    /**
     * Suspend user (admin only).
     * 
     * Suspends a user account by setting isActive to false.
     * Matches the API specification exactly.
     * 
     * @param userId ID of the user to suspend
     * @param request Request containing suspension reason
     * @return Success message
     */
    @PutMapping("/users/{userId}/suspend")
    @Operation(summary = "Suspend user account",
               description = "Suspend a user account (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User suspended successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Map<String, String>> suspendUser(
            @Parameter(description = "User ID to suspend") @PathVariable Long userId,
            @Valid @RequestBody Map<String, String> request) {

        String reason = request.get("reason");
        userService.suspendUser(userId, reason);

        return ResponseEntity.ok(Map.of("message", "User suspended successfully"));
    }

    /**
     * Activate user (admin only).
     *
     * Activates a suspended user account by setting isActive to true.
     *
     * @param userId ID of the user to activate
     * @return Success message
     */
    @PutMapping("/users/{userId}/activate")
    @Operation(summary = "Activate user account",
               description = "Activate a suspended user account (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User activated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<Map<String, String>> activateUser(
            @Parameter(description = "User ID to activate") @PathVariable Long userId) {

        userService.activateUser(userId);

        return ResponseEntity.ok(Map.of("message", "User activated successfully"));
    }

    /**
     * Update user role (admin only).
     *
     * Changes a user's role to a new role.
     *
     * @param userId ID of the user to update
     * @param request Request containing the new role
     * @return Updated user DTO
     */
    @PutMapping("/users/{userId}/role")
    @Operation(summary = "Update user role",
               description = "Update a user's role (admin only)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User role updated successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing token"),
        @ApiResponse(responseCode = "403", description = "Forbidden - admin role required"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "400", description = "Invalid role")
    })
    public ResponseEntity<UserDTO> updateUserRole(
            @Parameter(description = "User ID to update") @PathVariable Long userId,
            @Valid @RequestBody Map<String, String> request) {

        String roleStr = request.get("role");
        UserRole newRole = UserRole.valueOf(roleStr.toUpperCase());

        UserDTO updatedUser = userService.updateUserRole(userId, newRole);

        return ResponseEntity.ok(updatedUser);
    }
}